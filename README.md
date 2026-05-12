# TroTotTNU

Ung dung web quan ly va ket noi thue phong tro gan Dai hoc Thai Nguyen.

## Stack

- Frontend: HTML / CSS / JavaScript thuần
- Backend: Java 17 + Spring Boot 3.3.5
- Database: MySQL

## Chuc nang da co

- Nguoi thue:
  - Dang ky / dang nhap
  - Tim phong theo gia, khu vuc, dien tich, tien nghi
  - Xem chi tiet phong, anh, map, danh gia
  - Luu phong yeu thich
  - Chat truc tuyen voi chu tro
  - Gui yeu cau thue phong
  - Gui danh gia / khao sat
- Chu tro:
  - Dang tin moi
  - Cap nhat tin dang
  - Cap nhat trang thai phong
  - Quan ly danh sach phong
  - Xem thong ke co ban, hoi thoai va yeu cau thue
- Admin:
  - Dashboard tong quan
  - Kiem duyet tin dang
  - Theo doi yeu cau thue
  - Backup JSON du lieu
- AI chatbot:
  - Goi y phong theo ngan sach / khu vuc / tien nghi

## Cau hinh MySQL

Mac dinh ung dung doc cac bien:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Gia tri mac dinh:

- `jdbc:mysql://localhost:3306/boarding_house_platform?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh`
- `root`
- `123456`

## Build

Maven da duoc tai ve trong workspace:

```powershell
.\build.cmd
```

Script tren se dung app dang chay tu file `.jar` trong `target` truoc khi build de tranh loi Windows khoa file jar.

Neu muon chay Maven truc tiep, hay dam bao app cu da dung:

```powershell
E:\DATN\project\tools\apache-maven-3.9.9\bin\mvn.cmd -q -s E:\DATN\project\boardinghouse_platform\maven-settings.xml -DskipTests package -f E:\DATN\project\boardinghouse_platform\pom.xml
```

## Chay local

Sau khi MySQL san sang:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="123456"
E:\DATN\project\tools\apache-maven-3.9.9\bin\mvn.cmd -s E:\DATN\project\boardinghouse_platform\maven-settings.xml -f E:\DATN\project\boardinghouse_platform\pom.xml spring-boot:run
```

Truy cap:

- `http://localhost:8080`

## Tai khoan seed

- He thong tao admin mac dinh dua tren bien cau hinh trong `application.properties`:
  - `app.bootstrap-admin-email=admin@trototn.vn`
  - `app.bootstrap-admin-password=admin123`
- Neu chua co user nay, khi khoi dong se tao moi va gan vai tro ADMIN.

## Ghi chu

- Chua co du lieu mau; dung chuc nang backup/restore neu can.
- Hibernate tu dong tao / cap nhat bang khi khoi dong.

## Huong dan chay nhanh

1. Dat bien moi truong neu khac mac dinh:

```
$env:DB_URL="jdbc:mysql://localhost:3306/boarding_house_platform?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="123456"
```

2. Chay ung dung:

```
E:\DATN\project\tools\apache-maven-3.9.9\bin\mvn.cmd -s E:\DATN\project\boardinghouse_platform\maven-settings.xml -f E:\DATN\project\boardinghouse_platform\pom.xml spring-boot:run
```

3. Mo trinh duyet: `http://localhost:8080`
