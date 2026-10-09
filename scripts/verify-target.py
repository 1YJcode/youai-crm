"""Read-only target smoke acceptance and bounded concurrent HTTP capacity probe.

Uses Python standard library. Tokens come from environment, never from arguments.
Reports contain metrics only, never customer payloads or credentials.
"""
import argparse
import concurrent.futures
import datetime
import json
import math
import os
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request


def request(base, path, token, timeout):
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    start = time.perf_counter()
    try:
        req = urllib.request.Request(base + path, headers=headers)
        # Do not send bearer credentials through redirects to other origins.
        class NoRedirect(urllib.request.HTTPRedirectHandler):
            def redirect_request(self, req, fp, code, msg, headers, newurl):
                return None
        with urllib.request.build_opener(NoRedirect).open(req, timeout=timeout) as response:
            status = response.status
            try:
                payload = json.load(response)
            except (ValueError, UnicodeError):
                payload = None
    except urllib.error.HTTPError as error:
        status, payload = error.code, None
        error.close()
    except (OSError, urllib.error.URLError):
        status, payload = 0, None
    return status, payload, (time.perf_counter() - start) * 1000


def acceptable(path, status, payload):
    if status != 200:
        return False
    if path == "/api/health":
        return isinstance(payload, dict) and payload.get("status") == "UP" and payload.get("database") == 1
    if path.startswith("/api/customers?"):
        return isinstance(payload, dict) and isinstance(payload.get("content"), list) and isinstance(payload.get("totalElements"), int)
    if path == "/api/auth/me":
        return isinstance(payload, dict) and payload.get("id") is not None
    return isinstance(payload, (dict, list))


def percentile(samples, fraction):
    ordered = sorted(samples)
    return round(ordered[max(0, math.ceil(len(ordered) * fraction) - 1)], 2) if ordered else None


def run(args):
    token = os.environ.get("TARGET_ACCESS_TOKEN")
    if not token:
        raise ValueError("Set TARGET_ACCESS_TOKEN to a dedicated test account access token.")
    base = args.base_url.rstrip("/")
    url = urllib.parse.urlsplit(base)
    if url.scheme not in ("http", "https") or not url.netloc or url.username or url.password or url.query or url.fragment:
        raise ValueError("base-url must be an HTTP(S) origin without credentials, query or fragment.")
    paths = ["/api/health", "/api/auth/me", "/api/customers?page=0&size=20", "/api/tasks", "/api/orders"]
    report = {"startedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "baseUrl": base, "mode": args.mode, "checks": [], "scope": "read-only HTTP; full deployment signoff requires docs/target-validation.md"}
    for path in paths:
        status, payload, latency = request(base, path, token, args.timeout)
        report["checks"].append({"path": path, "status": status, "latencyMs": round(latency, 2),
                                  "passed": acceptable(path, status, payload)})
        if path.startswith("/api/customers?") and isinstance(payload, dict):
            report["visibleCustomerCount"] = payload.get("totalElements")
    status, _, _ = request(base, "/api/customers?page=0&size=20", None, args.timeout)
    report["checks"].append({"path": "/api/customers (anonymous)", "status": status, "passed": status in (401, 403)})
    passed = all(check["passed"] for check in report["checks"])
    if args.mode == "load" and passed:
        count = report.get("visibleCustomerCount", 0)
        if count < args.min_customers:
            raise ValueError(f"Account sees {count} customers; requires at least {args.min_customers}.")
        workload = paths[2:]
        # Warm-up is excluded from measurement.
        for _ in range(args.warmup):
            for path in workload:
                request(base, path, token, args.timeout)
        started = time.perf_counter()
        deadline = started + args.duration
        def worker(index):
            samples, failures, statuses, by_path = [], 0, {}, {}
            i = index
            while time.perf_counter() < deadline:
                path = workload[i % len(workload)]
                status, payload, latency = request(base, path, token, args.timeout)
                ok = acceptable(path, status, payload)
                samples.append(latency)
                failures += int(not ok)
                statuses[str(status)] = statuses.get(str(status), 0) + 1
                entry = by_path.setdefault(path, {"samples": [], "failures": 0})
                entry["samples"].append(latency)
                entry["failures"] += int(not ok)
                i += 1
            return samples, failures, statuses, by_path
        samples, failures, statuses, by_path = [], 0, {}, {}
        with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
            for values, errors, codes, endpoints in executor.map(worker, range(args.concurrency)):
                samples.extend(values)
                failures += errors
                for code, count in codes.items():
                    statuses[code] = statuses.get(code, 0) + count
                for path, entry in endpoints.items():
                    combined = by_path.setdefault(path, {"samples": [], "failures": 0})
                    combined["samples"].extend(entry["samples"])
                    combined["failures"] += entry["failures"]
        elapsed = time.perf_counter() - started
        rate = failures / len(samples) if samples else 1
        p95 = percentile(samples, .95)
        report["load"] = {"concurrency": args.concurrency, "requestedDurationSeconds": args.duration,
                          "elapsedSeconds": round(elapsed, 2), "requests": len(samples), "failures": failures,
                          "errorRate": rate, "requestsPerSecond": round(len(samples) / elapsed, 2),
                          "p95Ms": p95, "p99Ms": percentile(samples, .99), "httpStatuses": statuses,
                          "thresholds": {"maxErrorRate": args.max_error_rate, "maxP95Ms": args.max_p95_ms,
                                         "minRequestsPerSecond": args.min_rps, "minCustomers": args.min_customers},
                          "endpoints": {path: {"requests": len(entry["samples"]), "failures": entry["failures"],
                                               "p95Ms": percentile(entry["samples"], .95), "p99Ms": percentile(entry["samples"], .99)}
                                        for path, entry in by_path.items()}}
        passed = bool(samples) and rate <= args.max_error_rate and p95 <= args.max_p95_ms and len(samples) / elapsed >= args.min_rps
    report["passed"] = passed
    report["completedAt"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=["acceptance", "load"])
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--concurrency", type=int, default=10)
    parser.add_argument("--duration", type=int, default=60)
    parser.add_argument("--warmup", type=int, default=2)
    parser.add_argument("--timeout", type=float, default=10)
    parser.add_argument("--min-customers", type=int, default=10000)
    parser.add_argument("--max-p95-ms", type=float, default=1000)
    parser.add_argument("--max-error-rate", type=float, default=.01)
    parser.add_argument("--min-rps", type=float, default=1)
    args = parser.parse_args()
    if not (1 <= args.concurrency <= 500 and 1 <= args.duration <= 3600 and 0 <= args.warmup <= 100 and
            args.timeout > 0 and args.min_customers >= 0 and args.max_p95_ms > 0 and 0 <= args.max_error_rate <= 1 and args.min_rps >= 0):
        parser.error("Invalid concurrency, duration, warmup or threshold.")
    try:
        report = run(args)
    except ValueError as error:
        report = {"passed": False, "error": str(error)}
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"{'PASS' if report['passed'] else 'FAIL'}: {output}")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
