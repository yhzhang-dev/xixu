param(
    [string]$LogDirectory = 'E:\dsh-im\log',
    [string]$AppUsername = '',
    [switch]$PreviewOnly
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$importer = Join-Path $PSScriptRoot 'tools\import_dsh_logs.py'
$mysqlExe = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
if (-not (Test-Path -LiteralPath $LogDirectory -PathType Container)) { throw "日志目录不存在：$LogDirectory" }
if (-not (Test-Path -LiteralPath $mysqlExe -PathType Leaf) -and -not $PreviewOnly) { throw "找不到 MySQL 命令行：$mysqlExe" }

$summary = & python $importer --log-dir $LogDirectory --summary
if ($LASTEXITCODE -ne 0) { throw '日志预检失败，未写入数据库' }
$parsed = $summary | ConvertFrom-Json
Write-Host "预检完成：$($parsed.days) 天、$($parsed.rows) 条生活记录。只导入每日「生活记录」表；不导入日记、附件、账本原文件。"
Write-Host '运动类别保留原文，不自动推断为完成训练或填造组次。'
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
    $tableCount = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('life_log_entry','workout_set')"
    if ($LASTEXITCODE -ne 0) { throw '无法连接 xixu_dev；未写入日志' }
    if ([int]$tableCount -eq 0) {
        Write-Host '日志与训练表尚未建立，正在执行 05 建表脚本…'
        Get-Content -LiteralPath (Join-Path $projectRoot 'database\05-日志与训练记录.sql') -Raw -Encoding UTF8 | & $mysqlExe @mysqlArgs
        if ($LASTEXITCODE -ne 0) { throw '日志表创建失败，请保留上方错误；未导入数据' }
    } elseif ([int]$tableCount -ne 2) {
        throw '日志与训练表只有一张，结构不完整；未导入数据'
    }

    $columnCount = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'life_log_entry' AND COLUMN_NAME IN ('category_name','time_label','remark','source_ref')"
    if ($LASTEXITCODE -ne 0) { throw '无法核对日志表结构' }
    if ([int]$columnCount -eq 0) {
        Write-Host '检测到旧版日志表，正在增加原始类别、时间、备注与来源字段…'
        Get-Content -LiteralPath (Join-Path $projectRoot 'database\06-日志真实样本字段迁移.sql') -Raw -Encoding UTF8 | & $mysqlExe @mysqlArgs
        if ($LASTEXITCODE -ne 0) { throw '日志表迁移未完成，请保留上方错误，不要重复导入' }
    } elseif ([int]$columnCount -ne 4) {
        throw "日志表只有 $columnCount/4 个新字段，可能发生过部分迁移；未导入数据"
    }
    $importCheck = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'chk_life_log_source' AND CHECK_CLAUSE LIKE '%IMPORT%'"
    $sourceIndex = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'life_log_entry' AND INDEX_NAME = 'uk_life_log_user_source' AND NON_UNIQUE = 0"
    if ($LASTEXITCODE -ne 0 -or [int]$importCheck -ne 1 -or [int]$sourceIndex -lt 1) {
        throw '日志表的 IMPORT 来源约束或去重索引不完整；未导入数据'
    }

    $usernameHex = [Convert]::ToHexString([Text.Encoding]::UTF8.GetBytes($AppUsername))
    $userCount = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM app_user WHERE username = CONVERT(0x$usernameHex USING utf8mb4) AND status = 1"
    if ($LASTEXITCODE -ne 0 -or [int]$userCount -ne 1) { throw '目标汐序账号不存在或已禁用；未导入数据' }

    # Python 先在内存中验证全部行，再输出 UTF-8 十六进制 SQL；不生成含私人信息的中间文件。
    $sql = & python $importer --log-dir $LogDirectory --username $AppUsername
    if ($LASTEXITCODE -ne 0) { throw '导入 SQL 生成失败，未写入数据库' }
    $sql | & $mysqlExe @mysqlArgs
    if ($LASTEXITCODE -ne 0) { throw 'MySQL 导入失败，请保留上方错误；事务应已回滚' }

    $imported = & $mysqlExe @mysqlArgs -e "SELECT COUNT(*) FROM life_log_entry WHERE user_id = (SELECT id FROM app_user WHERE username = CONVERT(0x$usernameHex USING utf8mb4)) AND source_type = 'IMPORT' AND source_ref LIKE 'dsh-im/log/%'"
    if ($LASTEXITCODE -ne 0) { throw '导入后计数失败，无法确认结果' }
    Write-Host "导入完成：该账号目前有 $imported 条 dsh-im 生活记录样本。重复运行会跳过已导入的来源行。"
    Write-Host '刷新 Web 或手机的日志列表，切换到 2026 年 10 月查看。'
}
finally {
    if ($null -eq $oldMysqlPwd) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $oldMysqlPwd }
}
