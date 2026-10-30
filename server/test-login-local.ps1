$ErrorActionPreference = 'Stop'
$baseUrl = 'http://127.0.0.1:18080'
$username = Read-Host '刚创建的管理员用户名'
$secret = Read-Host '管理员密码' -AsSecureString
$password = [System.Net.NetworkCredential]::new('', $secret).Password

try {
    $body = @{ username = $username; password = $password } | ConvertTo-Json -Compress
    $login = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post `
        -ContentType 'application/json; charset=utf-8' -Body $body `
        -SkipHttpErrorCheck -TimeoutSec 10
    $body = $null
    $password = $null

    if ($login.StatusCode -ne 200) {
        Write-Host "登录未通过：HTTP $($login.StatusCode)。请核对账号密码；若为 500，请查看服务端终端报错。"
        exit 1
    }

    $result = $login.Content | ConvertFrom-Json
    $headers = @{ Authorization = "Bearer $($result.token)" }
    $me = Invoke-WebRequest -Uri "$baseUrl/api/auth/me" -Headers $headers `
        -SkipHttpErrorCheck -TimeoutSec 10
    if ($me.StatusCode -ne 200) {
        Write-Host "登录成功，但身份查询失败：HTTP $($me.StatusCode)"
        exit 1
    }

    $user = $me.Content | ConvertFrom-Json
    Write-Host "登录与身份查询成功：$($user.username)（$($user.roleCode)）"

    $logout = Invoke-WebRequest -Uri "$baseUrl/api/auth/logout" -Method Post -Headers $headers `
        -SkipHttpErrorCheck -TimeoutSec 10
    $afterLogout = Invoke-WebRequest -Uri "$baseUrl/api/auth/me" -Headers $headers `
        -SkipHttpErrorCheck -TimeoutSec 10
    if ($logout.StatusCode -eq 204 -and $afterLogout.StatusCode -eq 401) {
        Write-Host '退出登录成功；原令牌已失效。'
    } else {
        Write-Host "退出登录检查未通过：退出 HTTP $($logout.StatusCode)，再次查询 HTTP $($afterLogout.StatusCode)"
        exit 1
    }
}
finally {
    $password = $null
    $secret = $null
    $body = $null
    $result = $null
    $headers = $null
}
