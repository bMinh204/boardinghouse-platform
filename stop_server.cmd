@echo off
setlocal EnableExtensions

set "APP_DIR=%~dp0"
set "APP_JAR=%APP_DIR%target\boardinghouse-platform-0.0.1-SNAPSHOT.jar"
set "APP_JAR_NAME=boardinghouse-platform-0.0.1-SNAPSHOT.jar"
set "JPS=C:\Program Files\Java\jdk-17\bin\jps.exe"
set "FOUND=0"

if not exist "%JPS%" set "JPS=jps"

for /f "tokens=1,*" %%A in ('"%JPS%" -lv 2^>nul') do (
    echo %%B | find /I "%APP_JAR%" >nul
    if not errorlevel 1 (
        echo Stopping packaged app process %%A
        taskkill /PID %%A /F >nul 2>nul
        if not errorlevel 1 set "FOUND=1"
    ) else (
        echo %%B | find /I "%APP_JAR_NAME%" >nul
        if not errorlevel 1 (
            echo Stopping packaged app process %%A
            taskkill /PID %%A /F >nul 2>nul
            if not errorlevel 1 set "FOUND=1"
        )
    )
)

if "%FOUND%"=="0" echo No running packaged app found.
exit /b 0
