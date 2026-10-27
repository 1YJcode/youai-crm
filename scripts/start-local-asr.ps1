param([ValidateSet('cpu','cuda')][string]$Device = 'cpu', [string]$Model = 'large-v3')
$projectRoot = Split-Path -Parent $PSScriptRoot
$python = Join-Path $projectRoot '.local-asr\venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python)) { throw 'Please install the local ASR environment first.' }
& $python (Join-Path $PSScriptRoot 'local-asr-server.py') --model $Model --device $Device
