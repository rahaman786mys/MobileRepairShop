@echo off
echo --- MuZZu Tech: Get App Fingerprint ---
echo.
echo 1. Searching for Debug Key...
keytool -list -v -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -storepass android
echo.
echo ---------------------------------------
echo.
echo If you get "Access denied", run this in CMD as Administrator.
echo.
echo Copy the "SHA1" code above and paste it in your Google Cloud / Firebase Console.
pause
