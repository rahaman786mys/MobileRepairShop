$watchPath = "C:\Users\rhmna\OneDrive\Desktop\testing 2\MobileRepairShop"
$logFile = Join-Path $watchPath "change_log.txt"

$action = {
    $path = $Event.SourceEventArgs.FullPath
    $changeType = $Event.SourceEventArgs.ChangeType
    $logFile = "C:\Users\rhmna\OneDrive\Desktop\testing 2\MobileRepairShop\change_log.txt"
    
    if ($path -match '\\(build|\\.gradle|\\.git|\\.idea)\\' -or $path -match '\\change_log\\.txt' -or $path -match '\\watcher\\.ps1') {
        return
    }
    
    if ($path -match '\.(kt|xml|gradle|properties)$') {
        $time = Get-Date -Format "HH:mm:ss"
        $relative = $path.Replace("C:\Users\rhmna\OneDrive\Desktop\testing 2\MobileRepairShop\", "").TrimStart("\")
        $line = "$time [$changeType] $relative"
        Add-Content -Path $logFile -Value $line
    }
}

$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $watchPath
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

Register-ObjectEvent $watcher "Created" -Action $action | Out-Null
Register-ObjectEvent $watcher "Changed" -Action $action | Out-Null
Register-ObjectEvent $watcher "Deleted" -Action $action | Out-Null
Register-ObjectEvent $watcher "Renamed" -Action $action | Out-Null

"Watcher started at $(Get-Date -Format 'HH:mm:ss')" | Out-File -FilePath $logFile -Force

while ($true) {
    Start-Sleep -Seconds 60
    $time = Get-Date -Format "HH:mm:ss"
    Add-Content -Path $logFile -Value "--- $time (minute check) ---"
}

