param(
    [string]$MySqlBin = 'D:\phpstudy_pro\Extensions\MySQL5.7.26\bin',
    [ValidateRange(1024, 65535)][int]$Port = 13316
)

# Owns a new instance, never connects to the application's configured database.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$mysqld = Join-Path $MySqlBin 'mysqld.exe'
$mysql = Join-Path $MySqlBin 'mysql.exe'
$mysqladmin = Join-Path $MySqlBin 'mysqladmin.exe'
foreach ($binary in @($mysqld, $mysql, $mysqladmin)) {
    if (-not (Test-Path -LiteralPath $binary)) { throw "Missing executable: $binary" }
}
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "Port $Port is already in use; choose another test port."
}
$runId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
$runDirectory = Join-Path $projectRoot ".tmp_mysql_verify_$runId"
$dataDirectory = Join-Path $runDirectory 'data'
New-Item -ItemType Directory -Path $dataDirectory -Force | Out-Null
$basedir = Split-Path -Parent $MySqlBin
$server = $null
$savedEnvironment = @{}
$environmentNames = @('MYSQL_PWD', 'TEST_DB_URL', 'TEST_PROD_DB_URL', 'TEST_MIGRATION_DB_URL',
    'TEST_DB_USERNAME', 'TEST_DB_PASSWORD', 'TEST_DB_DRIVER', 'TEST_DB_MIGRATE', 'TEST_DB_DDL', 'SPRING_PROFILES_ACTIVE')
foreach ($name in $environmentNames) { $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$testExitCode = 1

function Invoke-TestSql([string]$Sql) {
    $output = & $mysql --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$Port" --user=root --batch --skip-column-names "--execute=$Sql"
    if ($LASTEXITCODE -ne 0) { throw 'Test MySQL command failed.' }
    return $output
}

try {
    $env:MYSQL_PWD = ''
    $initialize = Start-Process -FilePath $mysqld -ArgumentList @('--no-defaults', '--initialize-insecure',
        "--basedir=`"$basedir`"", "--datadir=`"$dataDirectory`"") -WindowStyle Hidden -Wait -PassThru `
        -RedirectStandardOutput (Join-Path $runDirectory 'initialize.out.log') `
        -RedirectStandardError (Join-Path $runDirectory 'initialize.err.log')
    if ($initialize.ExitCode -ne 0) { throw "MySQL initialization failed; see $runDirectory" }
    $server = Start-Process -FilePath $mysqld -ArgumentList @('--no-defaults', "--basedir=`"$basedir`"",
        "--datadir=`"$dataDirectory`"", '--bind-address=127.0.0.1', "--port=$Port",
        '--character-set-server=utf8mb4', '--collation-server=utf8mb4_unicode_ci', '--explicit-defaults-for-timestamp=ON',
        '--innodb-buffer-pool-size=128M', '--max-connections=30', '--console') -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runDirectory 'mysql.out.log') `
        -RedirectStandardError (Join-Path $runDirectory 'mysql.err.log')
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($server.HasExited) { throw "Test MySQL exited; see $runDirectory" }
        $client = New-Object System.Net.Sockets.TcpClient
        try { $client.Connect('127.0.0.1', $Port); $ready = $true; break } catch { Start-Sleep -Milliseconds 500 } finally { $client.Dispose() }
    }
    if (-not $ready) { throw 'Test MySQL startup timed out.' }
    $testPassword = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    Invoke-TestSql "ALTER USER 'root'@'localhost' IDENTIFIED BY '$testPassword';" | Out-Null
    $env:MYSQL_PWD = $testPassword
    Invoke-TestSql 'CREATE DATABASE youai_verify_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE youai_verify_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE youai_verify_migration CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;' | Out-Null
    $version = Invoke-TestSql 'SELECT VERSION();'
    Write-Output "Real MySQL version: $version; isolated port: $Port"
    Write-Output "Verification artifacts: $runDirectory"
    $jdbcOptions = '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
    $env:TEST_DB_URL = "jdbc:mysql://127.0.0.1:$Port/youai_verify_app$jdbcOptions"
    $env:TEST_PROD_DB_URL = "jdbc:mysql://127.0.0.1:$Port/youai_verify_prod$jdbcOptions"
    $env:TEST_MIGRATION_DB_URL = "jdbc:mysql://127.0.0.1:$Port/youai_verify_migration$jdbcOptions"
    $env:TEST_DB_USERNAME = 'root'
    $env:TEST_DB_PASSWORD = $testPassword
    $env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
    $env:TEST_DB_MIGRATE = 'true'
    $env:TEST_DB_DDL = 'validate'
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $ErrorActionPreference = 'Continue'
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'test-backend.ps1') `
        -BuildDirectory (Join-Path $runDirectory 'target') *> (Join-Path $runDirectory 'tests.log')
    $testExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    $history = Invoke-TestSql "SELECT 'app', COUNT(*), MAX(CAST(version AS UNSIGNED)) FROM youai_verify_app.flyway_schema_history WHERE success = 1 UNION ALL SELECT 'prod', COUNT(*), MAX(CAST(version AS UNSIGNED)) FROM youai_verify_prod.flyway_schema_history WHERE success = 1 UNION ALL SELECT 'upgrade', COUNT(*), MAX(CAST(version AS UNSIGNED)) FROM youai_verify_migration.flyway_schema_history WHERE success = 1;"
    $history | Set-Content -LiteralPath (Join-Path $runDirectory 'migration-history.tsv') -Encoding UTF8
    Get-Content -LiteralPath (Join-Path $runDirectory 'tests.log') -Tail 20
    Write-Output "MySQL verification exit code: $testExitCode"
} finally {
    if ($server -and -not $server.HasExited) {
        try {
            & $mysqladmin --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$Port" --user=root shutdown 2>$null
            if (-not $server.WaitForExit(10000)) { Stop-Process -Id $server.Id -Force }
        } catch { if (-not $server.HasExited) { Stop-Process -Id $server.Id -Force } }
    }
    foreach ($name in $environmentNames) { [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process') }
    Write-Output 'Isolated MySQL instance stopped. Existing application databases were not used.'
}
exit $testExitCode
