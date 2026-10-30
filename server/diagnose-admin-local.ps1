param([switch]$CompareApi)

$ErrorActionPreference = 'Stop'
$repo = Join-Path $env:USERPROFILE '.m2\repository'
$crypto = Get-ChildItem -Path (Join-Path $repo 'org\springframework\security\spring-security-crypto') -Filter 'spring-security-crypto-*.jar' -Recurse -File |
    Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
$mysql = Get-ChildItem -Path (Join-Path $repo 'com\mysql\mysql-connector-j') -Filter 'mysql-connector-j-*.jar' -Recurse -File |
    Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
$logging = Get-ChildItem -Path (Join-Path $repo 'commons-logging\commons-logging') -Filter 'commons-logging-*.jar' -Recurse -File |
    Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
if (-not $crypto -or -not $mysql -or -not $logging) { throw '缺少本地 Maven 依赖；请先在 server 目录运行 .\mvnw.cmd test。' }

$database = Read-Host '数据库名（直接回车使用 xixu_dev）'
if (-not $database) { $database = 'xixu_dev' }
if ($database -notmatch '^[A-Za-z0-9_]+$') { throw '数据库名只允许字母、数字和下划线' }
$dbUser = Read-Host 'MySQL 用户名（直接回车使用 root）'
if (-not $dbUser) { $dbUser = 'root' }
$dbSecret = Read-Host 'MySQL 密码' -AsSecureString
$username = Read-Host '要核对的登录用户名'
$loginSecret = Read-Host '该账号的登录密码' -AsSecureString

$dbPassword = [System.Net.NetworkCredential]::new('', $dbSecret).Password
$loginPassword = [System.Net.NetworkCredential]::new('', $loginSecret).Password
try {
    if ($dbPassword.Contains("`n") -or $loginPassword.Contains("`n")) { throw '此诊断脚本不支持包含换行符的密码' }
    $psi = [System.Diagnostics.ProcessStartInfo]::new('java')
    $psi.ArgumentList.Add('-cp')
    $psi.ArgumentList.Add("$crypto;$mysql;$logging")
    $psi.ArgumentList.Add((Join-Path $PSScriptRoot 'tools\AdminLoginProbe.java'))
    $psi.ArgumentList.Add($database)
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.StandardInputEncoding = [System.Text.UTF8Encoding]::new($false)
    $process = [System.Diagnostics.Process]::Start($psi)
    $process.StandardInput.WriteLine($dbUser)
    $process.StandardInput.WriteLine($dbPassword)
    $process.StandardInput.WriteLine($username)
    $process.StandardInput.WriteLine($loginPassword)
    $process.StandardInput.Close()
    $output = $process.StandardOutput.ReadToEnd()
    $errorOutput = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw $errorOutput.Trim() }
    Write-Host $output.Trim()
    if ($CompareApi) {
        $loginBody = @{ username = $username; password = $loginPassword } | ConvertTo-Json -Compress
        try {
            $login = Invoke-WebRequest -Uri 'http://127.0.0.1:18080/api/auth/login' -Method Post `
                -ContentType 'application/json; charset=utf-8' -Body $loginBody -SkipHttpErrorCheck -TimeoutSec 10
            Write-Host "apiLogin=HTTP $($login.StatusCode)"
            if ($login.StatusCode -eq 200) {
                $token = ($login.Content | ConvertFrom-Json).token
                $headers = @{ Authorization = "Bearer $token" }
                $null = Invoke-WebRequest -Uri 'http://127.0.0.1:18080/api/auth/logout' -Method Post `
                    -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
            }
        } catch {
            Write-Host 'apiLogin=无法连接服务端'
        }
    }
} finally {
    $dbPassword = $null
    $loginPassword = $null
    $dbSecret = $null
    $loginSecret = $null
    $loginBody = $null
    $token = $null
    $headers = $null
}
