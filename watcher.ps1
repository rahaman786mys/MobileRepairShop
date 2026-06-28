$watchPath = "C:\Users\rhmna\OneDrive\Desktop\testing 2\MobileRepairShop"
$logFile = "C:\Users\rhmna\OneDrive\Desktop\testing 2\MobileRepairShop\change_log.txt"
$env:Path += ";C:\Program Files\Git\cmd"

"Watcher started at $(Get-Date -Format 'HH:mm:ss')" | Out-File -FilePath $logFile -Force

while ($true) {
    Start-Sleep -Seconds 60
    $time = Get-Date -Format "HH:mm:ss"
    try {
        $gitStatus = git -C $watchPath status --porcelain -- "app/src/main/" "*.gradle" 2>&1
        if ($gitStatus) {
            Add-Content -Path $logFile -Value "$time [CHANGED]"
            foreach ($line in $gitStatus) {
                Add-Content -Path $logFile -Value "  $line"
            }
        } else {
            Add-Content -Path $logFile -Value "$time [OK]"
        }
    } catch {
        Add-Content -Path $logFile -Value "$time [ERR] $($_.Exception.Message)"
    }
}