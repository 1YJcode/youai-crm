$projectRoot = Split-Path -Parent $PSScriptRoot
$javaHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\jdk') -Directory |
    Select-Object -First 1 -ExpandProperty FullName
$mavenHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\maven') -Directory |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $javaHome -or -not $mavenHome) {
    throw 'Portable Java or Maven is missing from .tools.'
}

$env:JAVA_HOME = $javaHome
$maven = Join-Path $mavenHome 'bin\mvn.cmd'
$localRepository = Join-Path $projectRoot '.m2'

& $maven -f (Join-Path $projectRoot 'backend\pom.xml') "-Dmaven.repo.local=$localRepository" test
exit $LASTEXITCODE
