@echo off
setlocal
cd /d "%~dp0"

if not exist out (
    mkdir out
)

dir /s /b src\*.java > .sources.tmp
javac -encoding UTF-8 -d out @.sources.tmp
if errorlevel 1 (
    del .sources.tmp >nul 2>nul
    echo.
    echo Build failed. Check the errors above.
    pause
    exit /b 1
)
del .sources.tmp >nul 2>nul

powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'javaw.exe' -WorkingDirectory '%~dp0' -ArgumentList '-Dfile.encoding=UTF-8','-cp','out','com.wwpet.Main'"
exit /b 0
