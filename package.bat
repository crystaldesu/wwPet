@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0package.ps1"
if errorlevel 1 (
    echo.
    echo Packaging failed.
    pause
    exit /b 1
)
echo.
echo Packaging finished.
pause
exit /b 0
