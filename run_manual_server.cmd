@echo off
cd /d E:\DATN\project\boardinghouse_platform
if "%DB_USERNAME%"=="" set DB_USERNAME=root
if "%DB_PASSWORD%"=="" set DB_PASSWORD=123456
if "%APP_FRONTEND_URL%"=="" set APP_FRONTEND_URL=http://localhost:8080
"C:\Program Files\Java\jdk-17\bin\java.exe" -jar "E:\DATN\project\boardinghouse_platform\target\boardinghouse-platform-0.0.1-SNAPSHOT.jar" >> "E:\DATN\project\boardinghouse_platform\target\manual-server.out.log" 2>> "E:\DATN\project\boardinghouse_platform\target\manual-server.err.log"
