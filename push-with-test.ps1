Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Running tests before push..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Set-Location -LiteralPath $PSScriptRoot
./gradlew test --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host "" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  TESTS FAILED - Push CANCELLED" -ForegroundColor Red
    Write-Host "  Fix failures and try again." -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}

Write-Host "" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "  ALL TESTS PASSED - Pushing..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
git push
