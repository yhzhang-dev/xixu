param(
    [string]$LedgerFile = 'E:\dsh-im\log\账本\ledger.jsonl',
    [string]$AppUsername = '',
    [switch]$PreviewOnly
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$importer = Join-Path $PSScriptRoot 'tools\import_dsh_ledger.py'
$mysqlExe = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
if (-not (Test-Path -LiteralPath $LedgerFile -PathType Leaf)) { throw "账本文件不存在：$LedgerFile" }
if (-not (Test-Path -LiteralPath $mysqlExe -PathType Leaf) -and -not $PreviewOnly) { throw "找不到 MySQL 命令行：$mysqlExe" }

$summary = & python $importer --ledger $LedgerFile --summary
if ($LASTEXITCODE -ne 0) { throw '账本预检失败，未写入数据库' }
$parsed = $summary | ConvertFrom-Json
Write-Host "预检完成：共 $($parsed.total) 笔（支出 $($parsed.expense)、收入 $($parsed.income)、作废 $($parsed.void)）。"
if ($PreviewOnly) { return }

if ([string]::IsNullOrWhiteSpace($AppUsername)) { $AppUsername = Read-Host '要导入到哪个汐序用户名' }
if ([string]::IsNullOrWhiteSpace($AppUsername)) { throw '未提供汐序用户名，未写入数据库' }
$dbUser = Read-Host 'MySQL 用户名（直接回车使用 root）'
if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = 'root' }
$dbSecret = Read-Host 'MySQL 密码' -AsSecureString
$oldMysqlPwd = if (Test-Path Env:MYSQL_PWD) { $env:MYSQL_PWD } else { $null }
$env:MYSQL_PWD = [System.Net.NetworkCredential]::new('', $dbSecret).Password

try {
    $mysqlArgs = @('--default-character-set=utf8mb4', '-h', '127.0.0.1', '-P', '3306', '-u', $dbUser, '-D', 'xixu_dev', '-N', '-B')
    $tableCount = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_entry'"
    if ($LASTEXITCODE -ne 0) { throw '无法连接 xixu_dev；未写入账本' }
    if ([int]$tableCount -eq 0) { throw 'ledger_entry 表不存在，请先执行 database/04-记账表建表.sql' }

    $columnCount = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_entry' AND COLUMN_NAME = 'source_ref'"
    $indexCount = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_entry' AND INDEX_NAME = 'uk_ledger_user_source' AND NON_UNIQUE = 0"
    if ($LASTEXITCODE -ne 0) { throw '无法核对 ledger_entry 结构' }
    if ([int]$columnCount -eq 0 -and [int]$indexCount -eq 0) {
        Write-Host '正在为 ledger_entry 增加导入来源字段（首次导入，执行一次）…'
        Get-Content -LiteralPath (Join-Path $projectRoot 'database\09-记账来源标识迁移.sql') -Raw -Encoding UTF8 | & $mysqlExe @mysqlArgs
        if ($LASTEXITCODE -ne 0) { throw '来源字段迁移失败；未导入数据' }
    } elseif ([int]$columnCount -ne 1 -or [int]$indexCount -lt 1) {
        throw 'ledger_entry 的来源字段或去重索引不完整；未导入数据'
    }

    $usernameHex = [Convert]::ToHexString([Text.Encoding]::UTF8.GetBytes($AppUsername))
    $userCount = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM app_user WHERE username = CONVERT(0x$usernameHex USING utf8mb4) AND status = 1"
    if ($LASTEXITCODE -ne 0 -or [int]$userCount -ne 1) { throw '目标汐序账号不存在或已禁用；未导入数据' }

    $sql = & python $importer --ledger $LedgerFile --username $AppUsername
    if ($LASTEXITCODE -ne 0) { throw '导入 SQL 生成失败，未写入数据库' }
    $sql | & $mysqlExe @mysqlArgs
    if ($LASTEXITCODE -ne 0) { throw 'MySQL 导入失败；事务应已回滚' }

    $imported = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM ledger_entry WHERE user_id = (SELECT id FROM app_user WHERE username = CONVERT(0x$usernameHex USING utf8mb4)) AND source_ref LIKE 'dsh-im/ledger/%'"
    if ($LASTEXITCODE -ne 0) { throw '导入后计数失败' }
    Write-Host "导入完成：该账号目前有 $imported 笔 dsh-im 账本记录。重复运行会跳过已导入的来源行。"
}
finally {
    if ($null -eq $oldMysqlPwd) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $oldMysqlPwd }
}
