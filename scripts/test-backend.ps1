param([string]$BuildDirectory = '')

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\jdk') -Directory -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName
$mavenHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\maven') -Directory -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName

if ($javaHome) { $env:JAVA_HOME = $javaHome }
$maven = if ($mavenHome) { Join-Path $mavenHome 'bin\mvn.cmd' } else { (Get-Command mvn.cmd -ErrorAction Stop).Source }
$localRepository = Join-Path $projectRoot '.m2'

$testArguments = @('-f', (Join-Path $projectRoot 'backend\pom.xml'), "-Dmaven.repo.local=$localRepository")
if ($BuildDirectory) { $testArguments += "-Dyouai.build.directory=$BuildDirectory" }
& $maven @testArguments test
exit $LASTEXITCODE
