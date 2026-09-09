param(
    [string]$Database = 'youke_crm',
    [string]$Username = 'root',
    [string]$Password = 'root',
    [string]$MySqlHome = 'D:\phpstudy_pro\Extensions\MySQL5.7.26'
)

$mysql = Join-Path $MySqlHome 'bin\mysql.exe'
if (-not (Test-Path -LiteralPath $mysql)) {
    throw "MySQL client not found: $mysql"
}

$sql = "CREATE DATABASE IF NOT EXISTS ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
$env:MYSQL_PWD = $Password
try {
    & $mysql --host=127.0.0.1 --port=3306 --user=$Username --execute=$sql
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to create CRM database. Confirm MySQL is running and the credentials are correct.'
}

Write-Output "Database '$Database' is ready."
