Param(
    [string]$Secret = "verysecret",
    [string]$Email = "admin@local",
    [int]$Port = 8081
)

Write-Host "Starting app with SPRING_H2_CONSOLE_ENABLED=true and DEV_BOOTSTRAP_SECRET='$Secret'"

$env:SPRING_H2_CONSOLE_ENABLED = 'true'
$env:DEV_BOOTSTRAP_SECRET = $Secret

$mvnArgs = "-DskipTests","spring-boot:run"

# Start mvn in background and redirect output to dev-app.log
$startInfo = Start-Process -FilePath mvn -ArgumentList $mvnArgs -NoNewWindow -RedirectStandardOutput dev-app.log -RedirectStandardError dev-app.log -PassThru
$pid = $startInfo.Id
$pid | Out-File -FilePath .dev_pid -Encoding ascii
Write-Host "App started (pid=$pid), logs -> dev-app.log"

Write-Host "Waiting for app to become healthy on http://localhost:$Port/actuator/health ..."
for ($i = 0; $i -lt 60; $i++) {
    try {
        $resp = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -Method Get -TimeoutSec 1 -ErrorAction Stop
        Write-Host "App healthy."
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}

try {
    Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -Method Get -TimeoutSec 1 -ErrorAction Stop | Out-Null
} catch {
    Write-Error "Timed out waiting for app; check dev-app.log"
    exit 1
}

Write-Host "Minting dev token for $Email using bootstrap secret..."

$body = @{ email = $Email } | ConvertTo-Json
$headers = @{ 'X-BOOTSTRAP-SECRET' = $Secret }

$resp = Invoke-RestMethod -Uri "http://localhost:$Port/api/auth/dev-token" -Method Post -Body $body -Headers $headers -ContentType 'application/json' -ErrorAction Stop

if ($null -eq $resp.data.token -or $resp.data.token -eq '') {
    Write-Error "Failed to mint token. Response:`n$(ConvertTo-Json $resp -Depth 5)"
    exit 1
}

$token = $resp.data.token
Write-Host "Dev token minted (short-lived):`n`n$token`n"

Write-Host "You can now access the dev console in a browser by setting Authorization header:`n"
Write-Host "  Authorization: Bearer $token`n"

Write-Host "Or use curl to open the dev console:`n"
Write-Host "  curl -H \"Authorization: Bearer $token\" http://localhost:$Port/dev/console`n"

Write-Host "To stop the app: kill (Get-Content .dev_pid)"
