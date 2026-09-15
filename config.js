(() => {
  const host = window.location.hostname;
  const apiHost = host && !["localhost", "127.0.0.1", "::1"].includes(host) ? host : "127.0.0.1";
  window.YOUAI_API_BASE = `http://${apiHost}:8080/api`;
})();
