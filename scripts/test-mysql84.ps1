param([ValidateRange(1024,65535)][int]$Port = 13318)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$id = [guid]::NewGuid().ToString('N')
$container = "youai-verify84-$id"
$output = Join-Path $root ".tmp_mysql84_$id"
New-Item -ItemType Directory -Path $output | Out-Null
$password = [guid]::NewGuid().ToString('N')
$names = @('TEST_DB_URL','TEST_PROD_DB_URL','TEST_MIGRATION_DB_URL','TEST_DB_USERNAME','TEST_DB_PASSWORD','TEST_DB_DRIVER','TEST_DB_MIGRATE','TEST_DB_DDL','SPRING_PROFILES_ACTIVE')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
$created = $false
$result = 1
function Sql([string]$query) {
    $value = $query | docker exec -i -e "MYSQL_PWD=$password" $container mysql -uroot -N -B
    if ($LASTEXITCODE -ne 0) { throw 'Test SQL failed.' }
    return $value
}
try {
    docker run -d --name $container -p "127.0.0.1:${Port}:3306" -e "MYSQL_ROOT_PASSWORD=$password" -e 'MYSQL_ROOT_HOST=%' mysql:8.4 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot start isolated MySQL 8.4 container.' }
    $created = $true
    $ready = $false
    for ($i=0; $i -lt 120; $i++) {
        $ErrorActionPreference = 'Continue'
        docker exec -e "MYSQL_PWD=$password" $container mysql -uroot -e 'SELECT 1' 2>$null | Out-Null
        $probeExit = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        if ($probeExit -eq 0) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw 'MySQL startup timed out.' }
    $version = Sql 'SELECT VERSION();'
    if ($version -notmatch '^8\.4\.') { throw "Unexpected MySQL version: $version" }
    $version | Set-Content (Join-Path $output 'mysql-version.txt')
    docker inspect --format '{{.Image}}' $container | Set-Content (Join-Path $output 'image-id.txt')
    foreach ($db in @('app','prod','migration')) {
        Sql "CREATE DATABASE youai_verify_$db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
    }
    $options = '?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
    $env:TEST_DB_URL = "jdbc:mysql://127.0.0.1:$Port/youai_verify_app$options"
    $env:TEST_PROD_DB_URL = "jdbc:mysql://127.0.0.1:$Port/youai_verify_prod$options"
    $env:TEST_MIGRATION_DB_URL = "jdbc:mysql://127.0.0.1:$Port/youai_verify_migration$options"
    $env:TEST_DB_USERNAME = 'root'
    $env:TEST_DB_PASSWORD = $password
    $env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
    $env:TEST_DB_MIGRATE = 'true'
    $env:TEST_DB_DDL = 'validate'
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $ErrorActionPreference = 'Continue'
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'test-backend.ps1') -BuildDirectory (Join-Path $output 'target') *> (Join-Path $output 'tests.log')
    $result = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    foreach ($db in @('app','prod','migration')) {
        Sql "SELECT version,description,success FROM youai_verify_$db.flyway_schema_history ORDER BY installed_rank;" | Set-Content (Join-Path $output "history-$db.tsv")
    }
    @{ mysqlVersion=$version; testExitCode=$result; completedAt=(Get-Date).ToString('o') } | ConvertTo-Json | Set-Content (Join-Path $output 'result.json')
} finally {
    try {
        if ($created) {
            $ErrorActionPreference = 'Continue'
            docker logs $container *> (Join-Path $output 'mysql.log')
            docker rm -f -v $container | Out-Null
            if ($LASTEXITCODE -ne 0) { Write-Warning "Cleanup failed; remove test container $container manually." }
        }
    } finally {
        foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$saved[$name],'Process') }
        Write-Output "Verification artifacts: $output"
    }
}
exit $result
