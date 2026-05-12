@echo off
setlocal

cd /d "%~dp0"
call "%~dp0stop_server.cmd"

E:\DATN\project\tools\apache-maven-3.9.9\bin\mvn.cmd -q -s "%~dp0maven-settings.xml" -DskipTests package -f "%~dp0pom.xml"
exit /b %ERRORLEVEL%
