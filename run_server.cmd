@echo off
cd /d E:\DATN\project\boardinghouse_platform
if "%DB_USERNAME%"=="" set DB_USERNAME=root
if "%DB_PASSWORD%"=="" set DB_PASSWORD=123456
if "%APP_FRONTEND_URL%"=="" set APP_FRONTEND_URL=http://localhost:8080
E:\DATN\project\tools\apache-maven-3.9.9\bin\mvn.cmd -s E:\DATN\project\boardinghouse_platform\maven-settings.xml -f E:\DATN\project\boardinghouse_platform\pom.xml spring-boot:run
