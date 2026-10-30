$ErrorActionPreference = 'Stop'
$baseUrl = 'http://127.0.0.1:18080'
$today = (Get-Date).ToString('yyyy-MM-dd')
Write-Host '此联调会新增一笔 ¥12.50 测试支出并立即作废；数据库会保留一条已作废记录。'

try {
    $health = Invoke-WebRequest -Uri "$baseUrl/api/health" -SkipHttpErrorCheck -TimeoutSec 5
    if ($health.StatusCode -ne 200) { Write-Host "服务端健康检查失败：HTTP $($health.StatusCode)"; exit 1 }
} catch {
    Write-Host '无法连接服务端，请先运行 .\start-local.ps1 并等待启动完成。'
    exit 1
}

$username = Read-Host '已存在的用户名'
$secret = Read-Host '密码' -AsSecureString
$password = [System.Net.NetworkCredential]::new('', $secret).Password

try {
    $loginBody = @{ username = $username; password = $password } | ConvertTo-Json -Compress
    $login = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body $loginBody -SkipHttpErrorCheck -TimeoutSec 10
    $password = $null
    if ($login.StatusCode -ne 200) {
        if ($login.StatusCode -eq 401) {
            Write-Host '登录未通过：HTTP 401。服务端已连接，但该数据库中用户名不存在、账号停用或密码不匹配。'
        } else {
            Write-Host "登录未通过：HTTP $($login.StatusCode)。请检查服务端终端报错。"
        }
        exit 1
    }
    $token = ($login.Content | ConvertFrom-Json).token
    $headers = @{ Authorization = "Bearer $token" }

    $beforeResponse = Invoke-WebRequest -Uri "$baseUrl/api/ledger/summary?from=$today&to=$today" -Method Get -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
    if ($beforeResponse.StatusCode -ne 200) { Write-Host "汇总接口未就绪：HTTP $($beforeResponse.StatusCode)。请重启新版服务端。"; exit 1 }
    $before = $beforeResponse.Content | ConvertFrom-Json

    $entryBody = @{ entryType = 'EXPENSE'; amount = 12.50; occurredOn = $today; categoryName = '吃饭'; channelName = '微信'; note = '冒烟测试' } | ConvertTo-Json -Compress
    $created = Invoke-WebRequest -Uri "$baseUrl/api/ledger/entries" -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $entryBody -SkipHttpErrorCheck -TimeoutSec 10
    if ($created.StatusCode -ne 201) {
        Write-Host "记账失败：HTTP $($created.StatusCode) $($created.Content)"
        exit 1
    }
    $entry = $created.Content | ConvertFrom-Json
    Write-Host "记账成功：id=$($entry.id) $($entry.entryType) $($entry.amount) $($entry.categoryName) status=$($entry.status)"

    $afterCreateResponse = Invoke-WebRequest -Uri "$baseUrl/api/ledger/summary?from=$today&to=$today" -Method Get -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
    if ($afterCreateResponse.StatusCode -ne 200) { Write-Host '新增后汇总查询失败'; exit 1 }
    $afterCreate = $afterCreateResponse.Content | ConvertFrom-Json
    if ([decimal]$afterCreate.expense -ne ([decimal]$before.expense + [decimal]12.50) -or [long]$afterCreate.count -ne ([long]$before.count + 1)) {
        Write-Host '新增后汇总金额或笔数不正确'; exit 1
    }
    Write-Host '新增后汇总校验通过'

    $list = Invoke-WebRequest -Uri "$baseUrl/api/ledger/entries?from=$today&to=$today" -Method Get -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
    if ($list.StatusCode -ne 200) {
        Write-Host "列表查询失败：HTTP $($list.StatusCode)"
        exit 1
    }
    $page = $list.Content | ConvertFrom-Json
    if ($page.total -lt 1) { Write-Host "列表查询异常：total=$($page.total) 应至少为 1"; exit 1 }
    Write-Host "列表查询成功：total=$($page.total)"

    $one = Invoke-WebRequest -Uri "$baseUrl/api/ledger/entries/$($entry.id)" -Method Get -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
    if ($one.StatusCode -ne 200) { Write-Host "单条查询失败：HTTP $($one.StatusCode)"; exit 1 }
    Write-Host "单条查询成功：id=$($entry.id)"

    $voidBody = @{ reason = '冒烟测试作废' } | ConvertTo-Json -Compress
    $void = Invoke-WebRequest -Uri "$baseUrl/api/ledger/entries/$($entry.id)/void" -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $voidBody -SkipHttpErrorCheck -TimeoutSec 10
    if ($void.StatusCode -ne 200) {
        Write-Host "作废失败：HTTP $($void.StatusCode) $($void.Content)"
        exit 1
    }
    $voided = $void.Content | ConvertFrom-Json
    Write-Host "作废成功：id=$($voided.id) status=$($voided.status) reason=$($voided.voidReason)"

    $afterVoidResponse = Invoke-WebRequest -Uri "$baseUrl/api/ledger/summary?from=$today&to=$today" -Method Get -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
    if ($afterVoidResponse.StatusCode -ne 200) { Write-Host '作废后汇总查询失败'; exit 1 }
    $afterVoid = $afterVoidResponse.Content | ConvertFrom-Json
    if ([decimal]$afterVoid.expense -ne [decimal]$before.expense -or [long]$afterVoid.count -ne [long]$before.count) {
        Write-Host '作废后汇总未恢复到测试前数值'; exit 1
    }
    Write-Host '作废后汇总校验通过'

    $audit = Invoke-WebRequest -Uri "$baseUrl/api/ledger/entries?from=$today&to=$today&includeVoided=true&size=100" -Method Get -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
    if ($audit.StatusCode -ne 200) { Write-Host '作废留痕查询失败'; exit 1 }
    $auditEntry = ($audit.Content | ConvertFrom-Json).items | Where-Object id -eq $entry.id | Select-Object -First 1
    if (-not $auditEntry -or $auditEntry.status -ne 0 -or $auditEntry.voidReason -ne '冒烟测试作废') {
        Write-Host '作废留痕内容不正确'; exit 1
    }
    Write-Host '作废留痕校验通过'

    $again = Invoke-WebRequest -Uri "$baseUrl/api/ledger/entries/$($entry.id)/void" -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $voidBody -SkipHttpErrorCheck -TimeoutSec 10
    if ($again.StatusCode -ne 409) { Write-Host "重复作废应返回 409，实际 $($again.StatusCode)"; exit 1 }
    Write-Host "重复作废校验通过（HTTP 409）"

    $logout = Invoke-WebRequest -Uri "$baseUrl/api/auth/logout" -Method Post -Headers $headers -SkipHttpErrorCheck -TimeoutSec 10
    if ($logout.StatusCode -ne 204) { Write-Host "退出失败：HTTP $($logout.StatusCode)"; exit 1 }

    Write-Host '记账冒烟测试全部通过'
}
finally {
    if ($token -and $entry -and -not $voided) {
        try {
            $cleanupBody = @{ reason = '联调中断，清理测试账目' } | ConvertTo-Json -Compress
            $cleanup = Invoke-WebRequest -Uri "$baseUrl/api/ledger/entries/$($entry.id)/void" -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $cleanupBody -SkipHttpErrorCheck -TimeoutSec 10
            if ($cleanup.StatusCode -eq 200) { Write-Host '已作废联调中断时留下的测试账目' }
            else { Write-Host "注意：测试账目 id=$($entry.id) 可能仍有效，请在 Web 端核对。" }
        } catch { Write-Host "注意：测试账目 id=$($entry.id) 可能仍有效，请在 Web 端核对。" }
    }
    $password = $null; $secret = $null; $token = $null; $headers = $null
    $loginBody = $null; $entryBody = $null; $voidBody = $null; $cleanupBody = $null
}
