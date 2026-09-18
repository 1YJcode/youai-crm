# Compares a source and target MySQL database: exact row counts, per-table content
# fingerprints and Flyway history. Used to verify a database migration (e.g. MySQL
# 5.7 -> 8.4) without modifying either database.
#
# Usage:
#   .\scripts\verify-db-migration.ps1 -SourceContainer youai_project-mysql-1 -SourceDatabase youke_crm `
#                                     -TargetContainer youai-mysql84        -TargetDatabase youai_crm
#
# Exit code 0 = identical, 1 = mismatch, 2 = could not verify (never reported as success).
# Read-only: issues SELECT statements only.
param(
    [Parameter(Mandatory = $true)][string]$SourceContainer,
    [Parameter(Mandatory = $true)][string]$SourceDatabase,
    [Parameter(Mandatory = $true)][string]$TargetContainer,
    [Parameter(Mandatory = $true)][string]$TargetDatabase
)

$mysqlUser = 'root'
$mysqlPassword = 'root'
# Lifts the 1024-byte default so GROUP_CONCAT cannot silently truncate a fingerprint.
$initCmd = 'SET SESSION group_concat_max_len=100000000'

# Password goes through MYSQL_PWD, not -p, so the client emits no stderr warning
# (PowerShell 5.1 turns native stderr into error records that can void an assignment).
function Invoke-Query {
    param([string]$Container, [string]$Database, [string]$Sql)
    # "-u$mysqlUser" must stay quoted: unquoted, PowerShell passes the literal text
    # "-u$mysqlUser" through to the native command without expanding the variable.
    $raw = $Sql | docker exec -i -e "MYSQL_PWD=$mysqlPassword" $Container mysql "-u$mysqlUser" --init-command="$initCmd" -N -B $Database
    return @($raw | ForEach-Object { "$_".Trim() } | Where-Object { $_ -ne '' })
}

function Get-TableList {
    param([string]$Container, [string]$Database)
    $sql = "SELECT table_name FROM information_schema.tables WHERE table_schema='$Database' AND table_type='BASE TABLE' ORDER BY table_name"
    return @(Invoke-Query -Container $Container -Database $Database -Sql $sql)
}

function Get-Fingerprint {
    param([string]$Container, [string]$Database, [string]$Table)
    $columns = @(Invoke-Query -Container $Container -Database $Database -Sql "SELECT column_name FROM information_schema.columns WHERE table_schema='$Database' AND table_name='$Table' ORDER BY ordinal_position")
    if ($columns.Count -eq 0) { return '<no-columns>' }

    # Hash every row, then hash the sorted row hashes => order-independent multiset compare.
    # IFNULL keeps NULL distinguishable from an empty string.
    $colExpr = ($columns | ForEach-Object { "IFNULL(CAST($_ AS CHAR), '\0')" }) -join ', '
    $sql = "SELECT MD5(GROUP_CONCAT(h ORDER BY h SEPARATOR '')) FROM (SELECT MD5(CONCAT_WS('#', $colExpr)) AS h FROM $Table) x"
    $fp = @(Invoke-Query -Container $Container -Database $Database -Sql $sql)
    if ($fp.Count -gt 0) { return "$($fp[0])" } else { return '<query-failed>' }
}

function Get-CountMap {
    param([string]$Container, [string]$Database, [string[]]$Tables)
    $parts = foreach ($t in $Tables) { "SELECT '$t' AS tbl, COUNT(*) AS cnt FROM $t" }
    $rows = @(Invoke-Query -Container $Container -Database $Database -Sql (($parts -join ' UNION ALL ') + ';'))
    $map = @{}
    foreach ($row in $rows) {
        $bits = $row -split "`t"
        if ($bits.Count -ge 2) { $map[$bits[0]] = [int]$bits[1] }
    }
    return $map
}

Write-Output "source: $SourceContainer / $SourceDatabase"
Write-Output "target: $TargetContainer / $TargetDatabase"
Write-Output ""

$sourceTables = Get-TableList -Container $SourceContainer -Database $SourceDatabase
$targetTables = Get-TableList -Container $TargetContainer -Database $TargetDatabase

# Sentinel: verifying nothing must never look like success.
if ($sourceTables.Count -eq 0 -or $targetTables.Count -eq 0) {
    Write-Output 'RESULT: COULD NOT VERIFY - one side returned no tables.'
    Write-Output "  source tables: $($sourceTables.Count)  target tables: $($targetTables.Count)"
    Write-Output '  Check container names, database names and credentials.'
    exit 2
}

$sourceCounts = Get-CountMap -Container $SourceContainer -Database $SourceDatabase -Tables $sourceTables
$targetCounts = Get-CountMap -Container $TargetContainer -Database $TargetDatabase -Tables $targetTables

$allTables = @($sourceTables + $targetTables | Sort-Object -Unique)
$mismatches = 0

Write-Output ("{0,-42} {1,8} {2,8} {3,6}  {4}" -f 'table', 'source', 'target', 'rows', 'content')
Write-Output ('-' * 92)

foreach ($table in $allTables) {
    $inSource = $sourceCounts.ContainsKey($table)
    $inTarget = $targetCounts.ContainsKey($table)
    $sc = if ($inSource) { $sourceCounts[$table] } else { '-' }
    $tc = if ($inTarget) { $targetCounts[$table] } else { '-' }

    $rowOk = ($inSource -and $inTarget -and $sourceCounts[$table] -eq $targetCounts[$table])
    $contentOk = $false
    $contentLabel = 'MISSING'

    if ($inSource -and $inTarget) {
        $sfp = Get-Fingerprint -Container $SourceContainer -Database $SourceDatabase -Table $table
        $tfp = Get-Fingerprint -Container $TargetContainer -Database $TargetDatabase -Table $table
        $contentOk = ($sfp -eq $tfp)
        if ($contentOk) { $contentLabel = 'OK' } else { $contentLabel = "DIFF ($sfp vs $tfp)" }
    }

    if (-not ($rowOk -and $contentOk)) { $mismatches++ }

    $rowLabel = if ($rowOk) { 'OK' } else { 'DIFF' }
    Write-Output ("{0,-42} {1,8} {2,8} {3,6}  {4}" -f $table, $sc, $tc, $rowLabel, $contentLabel)
}

Write-Output ('-' * 92)

$sourceFlyway = @(Invoke-Query -Container $SourceContainer -Database $SourceDatabase -Sql "SELECT CONCAT(version, '|', success) FROM $SourceDatabase.flyway_schema_history ORDER BY installed_rank")
$targetFlyway = @(Invoke-Query -Container $TargetContainer -Database $TargetDatabase -Sql "SELECT CONCAT(version, '|', success) FROM $TargetDatabase.flyway_schema_history ORDER BY installed_rank")

$flywayOk = ($sourceFlyway.Count -gt 0) -and (($sourceFlyway -join ',') -eq ($targetFlyway -join ','))

Write-Output "tables compared: $($allTables.Count)   row/content mismatches: $mismatches"
Write-Output "flyway history:  $(if ($flywayOk) { 'OK' } else { 'DIFF' }) (source $($sourceFlyway.Count) rows, target $($targetFlyway.Count) rows)"
Write-Output ''

# Distinguish "nothing to compare against" from "compared and they differ".
if ($sourceFlyway.Count -eq 0 -or $targetFlyway.Count -eq 0) {
    Write-Output 'RESULT: COULD NOT VERIFY - flyway history missing on one side (wrong database?).'
    exit 2
}

if ($mismatches -gt 0 -or -not $flywayOk) {
    Write-Output 'RESULT: MISMATCH - do not proceed with the migration.'
    exit 1
}

Write-Output 'RESULT: IDENTICAL - all tables and flyway history match.'
exit 0
