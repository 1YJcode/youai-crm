param(
    [string]$DbHost = '127.0.0.1',
    [int]$DbPort = 3306,
    [string]$DbUsername = 'root',
    [string]$DbPassword = 'root'
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\jdk') -Directory | Select-Object -First 1 -ExpandProperty FullName
$mavenHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\maven') -Directory | Select-Object -First 1 -ExpandProperty FullName

if (-not $javaHome -or -not $mavenHome) {
    throw 'Portable Java or Maven is missing from .tools.'
}

$env:JAVA_HOME = $javaHome
$maven = Join-Path $mavenHome 'bin\mvn.cmd'
$localRepository = Join-Path $projectRoot '.m2'

if (-not $env:DB_URL) {
    $env:DB_URL = "jdbc:mysql://${DbHost}:${DbPort}/youke_crm?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
}
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = $DbUsername }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = $DbPassword }

& $maven -f (Join-Path $projectRoot 'backend\pom.xml') "-Dmaven.repo.local=$localRepository" spring-boot:run
