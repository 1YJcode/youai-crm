param(
    [switch]$MySql,
    [switch]$S3,
    [string]$TestClasses = 'CallRecordingControllerTest,CallRecordingIntegrationTest,CustomerCallIntegrationTest,RecordingAnalysisIntegrationTest,RecordingKnowledgeIntegrationTest,RecordingStorageQueueIntegrationTest,RecordingS3IntegrationTest'
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$suffix = [guid]::NewGuid().ToString('N')
$outputPath = Join-Path $project ".tmp_storage_verification_$suffix"
New-Item -ItemType Directory -Path $outputPath | Out-Null
$repoPath = Join-Path $project '.m2'
$agentPath = Join-Path $repoPath 'net/bytebuddy/byte-buddy-agent/1.15.11/byte-buddy-agent-1.15.11.jar'
$containers = @()
$variables = @('TEST_DB_URL','TEST_DB_USERNAME','TEST_DB_PASSWORD','TEST_DB_DRIVER','TEST_DB_MIGRATE','TEST_DB_DDL','S3_TEST_ENDPOINT')
$previous = @{}
foreach ($name in $variables) { $previous[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
$testExit = 1
try {
    # Only explicitly created test services may be used; never inherit another database target.
    foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name,$null,'Process') }
    if ($MySql) {
        $mysqlName = "youai-storage-mysql-$suffix"
        $testPassword = [guid]::NewGuid().ToString('N')
        docker run -d --name $mysqlName -p '127.0.0.1::3306' -e "MYSQL_ROOT_PASSWORD=$testPassword" -e 'MYSQL_ROOT_HOST=%' -e 'MYSQL_DATABASE=recording_test' mysql:8.4 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Cannot start isolated MySQL' }
        $containers += $mysqlName
        $ready = $false
        for ($attempt=0; $attempt -lt 120; $attempt++) {
            docker exec -e "MYSQL_PWD=$testPassword" $mysqlName mysql -uroot -e 'SELECT 1' 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) { $ready=$true; break }
            Start-Sleep -Milliseconds 500
        }
        if (-not $ready) { throw 'Isolated MySQL startup timed out' }
        $mapping = docker port $mysqlName 3306/tcp
        $portNumber = ($mapping -split ':')[-1]
        $env:TEST_DB_URL = "jdbc:mysql://127.0.0.1:$portNumber/recording_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
        $env:TEST_DB_USERNAME='root'; $env:TEST_DB_PASSWORD=$testPassword
        $env:TEST_DB_DRIVER='com.mysql.cj.jdbc.Driver'; $env:TEST_DB_MIGRATE='true'; $env:TEST_DB_DDL='validate'
    }
    if ($S3) {
        $minioName = "youai-storage-minio-$suffix"
        docker run -d --name $minioName -p '127.0.0.1::9000' -e MINIO_ROOT_USER=storage-test-user -e MINIO_ROOT_PASSWORD=storage-test-password-20261025 quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z server /data | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Cannot start isolated MinIO' }
        $containers += $minioName
        $env:S3_TEST_ENDPOINT='http://'+(docker port $minioName 9000/tcp)
    }
    $arguments = @('-B','-f',(Join-Path $project 'backend/pom.xml'),"-Dmaven.repo.local=$repoPath",
        "-Dyouai.build.directory=$outputPath/target","-Dtest=$TestClasses","-DargLine=-javaagent:$agentPath")
    if ($MySql) { $arguments += '-DreuseForks=false' }
    & mvn.cmd @arguments test *> (Join-Path $outputPath 'tests.log')
    $testExit=$LASTEXITCODE
    if ($MySql) {
        docker exec -e "MYSQL_PWD=$testPassword" $mysqlName mysql -uroot -N -e 'SELECT version,success FROM recording_test.flyway_schema_history ORDER BY installed_rank' > (Join-Path $outputPath 'migration-history.txt')
    }
    @{testExitCode=$testExit; completedAt=(Get-Date).ToString('o')} | ConvertTo-Json > (Join-Path $outputPath 'result.json')
} finally {
    foreach ($container in $containers) { docker rm -f -v $container | Out-Null }
    foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') }
    Write-Output "Recording storage verification: $outputPath"
}
exit $testExit
