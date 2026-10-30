param([switch]$CreateAdmin)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $port = if ([string]::IsNullOrWhiteSpace($env:XIXU_PORT)) { 18080 } else { [int]$env:XIXU_PORT }
    $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $owner = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        throw "端口 $port 已被 $($owner.ProcessName) (PID $($listener.OwningProcess)) 占用，请先处理端口冲突"
    }
    $dbUser = Read-Host 'MySQL 用户名（直接回车使用 root）'
    if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = 'root' }
    $dbSecret = Read-Host 'MySQL 密码' -AsSecureString
    $env:XIXU_DB_USER = $dbUser
    $env:XIXU_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbSecret).Password

    if ($CreateAdmin) {
        $env:XIXU_BOOTSTRAP_USERNAME = Read-Host '初始管理员用户名（3-50 位字母、数字或下划线）'
        $adminSecret = Read-Host '初始管理员密码（至少 12 位）' -AsSecureString
        $env:XIXU_BOOTSTRAP_PASSWORD = [System.Net.NetworkCredential]::new('', $adminSecret).Password
    }

    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "服务端退出，代码 $LASTEXITCODE" }
}
finally {
    Remove-Item Env:XIXU_DB_USER,Env:XIXU_DB_PASSWORD,Env:XIXU_BOOTSTRAP_USERNAME,Env:XIXU_BOOTSTRAP_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
}
