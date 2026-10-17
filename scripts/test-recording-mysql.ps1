param([string]$TestClasses = 'CallRecordingIntegrationTest,RecordingAnalysisIntegrationTest')
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$suffix = [guid]::NewGuid().ToString('N')
$containerName = "youai-recording-test-$suffix"
$outputPath = Join-Path $project ".tmp_recording_mysql_$suffix"
New-Item -ItemType Directory -Path $outputPath | Out-Null
$testPassword = [guid]::NewGuid().ToString('N')
$exitResult = 1
$created = $false
$variables = @('TEST_DB_URL','TEST_DB_USERNAME','TEST_DB_PASSWORD','TEST_DB_DRIVER','TEST_DB_MIGRATE','TEST_DB_DDL')
$previous = @{}
foreach ($name in $variables) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    docker run -d --name $containerName -p '127.0.0.1::3306' -e "MYSQL_ROOT_PASSWORD=$testPassword" -e 'MYSQL_ROOT_HOST=%' -e 'MYSQL_DATABASE=recording_test' mysql:8.4 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot start isolated MySQL container' }
    $created = $true
    $ready = $false
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        docker exec -e "MYSQL_PWD=$testPassword" $containerName mysql -uroot -e 'SELECT 1' 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw 'MySQL startup timeout' }
    $mapping = docker port $containerName 3306/tcp
    $portNumber = ($mapping -split ':')[-1]
    $env:TEST_DB_URL = "jdbc:mysql://127.0.0.1:$portNumber/recording_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    $env:TEST_DB_USERNAME = 'root'
    $env:TEST_DB_PASSWORD = $testPassword
    $env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
    $env:TEST_DB_MIGRATE = 'true'
    $env:TEST_DB_DDL = 'validate'
    $agentPath = Join-Path $env:USERPROFILE '.m2/repository/net/bytebuddy/byte-buddy-agent/1.15.11/byte-buddy-agent-1.15.11.jar'
    & mvn.cmd -f (Join-Path $project 'backend/pom.xml') "-Dyouai.build.directory=$outputPath/target" "-Dtest=$TestClasses" "-DargLine=-javaagent:$agentPath" test *> (Join-Path $outputPath 'tests.log')
    $exitResult = $LASTEXITCODE
    docker exec -e "MYSQL_PWD=$testPassword" $containerName mysql -uroot -N -e 'SELECT VERSION(); SELECT version,success FROM recording_test.flyway_schema_history ORDER BY installed_rank;' | Set-Content (Join-Path $outputPath 'migration-history.txt')
    @{testExitCode=$exitResult; completedAt=(Get-Date).ToString('o')} | ConvertTo-Json | Set-Content (Join-Path $outputPath 'result.json')
} finally {
    if ($created) { docker rm -f -v $containerName | Out-Null }
    foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    Write-Output "Recording verification artifacts: $outputPath"
}
exit $exitResult
