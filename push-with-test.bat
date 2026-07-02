@echo off
echo ========================================
echo   Running tests before push...
echo ========================================
cd /d "%~dp0"
call gradlew test --no-daemon
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo   TESTS FAILED - Push CANCELLED
    echo   Fix failures and try again.
    echo ========================================
    exit /b 1
)
echo.
echo ========================================
echo   ALL TESTS PASSED - Pushing...
echo ========================================
git push
