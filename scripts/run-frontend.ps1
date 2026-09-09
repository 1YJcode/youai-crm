$projectRoot = Split-Path -Parent $PSScriptRoot
$port = 4173
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

if ($listener) {
    Write-Output "Frontend is already running on port $port (LAN access enabled)."
    exit 0
}

$pythonCandidates = @(
    (Get-Command python -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source),
    'C:\Users\卢元江\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

$python = $pythonCandidates | Select-Object -First 1
if (-not $python) {
    $serverScript = Join-Path $PSScriptRoot 'static-server.ps1'
    Start-Process -FilePath powershell.exe -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $serverScript, '-Root', $projectRoot, '-Port', $port -WindowStyle Hidden
    Write-Output "Frontend started with the built-in PowerShell server on 0.0.0.0:$port"
    exit 0
}

Start-Process -FilePath $python -ArgumentList '-m', 'http.server', $port, '--bind', '0.0.0.0' -WorkingDirectory $projectRoot -WindowStyle Hidden
Write-Output "Frontend started on 0.0.0.0:$port (LAN access enabled)"
