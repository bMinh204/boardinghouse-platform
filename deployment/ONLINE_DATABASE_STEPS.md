# Online Database Setup

## Current Local Database

- Database name: `boarding_house_platform`
- SQL dump file: `deployment/boarding_house_platform_online_seed.sql`
- Spring Boot uses these environment variables:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`

## Create Online MySQL Database

Create a MySQL database online with this name:

```sql
boarding_house_platform
```

Use MySQL 8.x if possible. The app uses Hibernate `ddl-auto=update`, so it can create/update tables automatically, but importing the dump is better if you want current local data online.

## Import Local Data

Replace the placeholders with your real online DB information:

```powershell
& "E:\DATN\mysql.exe" `
  -h YOUR_DB_HOST `
  -P 3306 `
  -u YOUR_DB_USERNAME `
  -p `
  --default-character-set=utf8mb4 `
  boarding_house_platform `
  --ssl-mode=REQUIRED `
  < "E:\DATN\project\boardinghouse_platform\deployment\boarding_house_platform_online_seed.sql"
```

If your provider gives a JDBC URL, convert it to this format:

```properties
DB_URL=jdbc:mysql://YOUR_DB_HOST:3306/boarding_house_platform?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=utf8
```

## Production Environment Variables

Set these on the online app server:

```properties
DB_URL=jdbc:mysql://YOUR_DB_HOST:3306/boarding_house_platform?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=utf8
DB_USERNAME=YOUR_DB_USERNAME
DB_PASSWORD=YOUR_DB_PASSWORD
APP_FRONTEND_URL=https://YOUR_DOMAIN
```

## Verify Connection

Run this from the deployment machine:

```powershell
& "E:\DATN\mysql.exe" -h YOUR_DB_HOST -P 3306 -u YOUR_DB_USERNAME -p --ssl-mode=REQUIRED -e "SHOW TABLES;" boarding_house_platform
```

Expected tables include:

```text
users
rooms
room_images
room_amenities
rental_requests
conversations
messages
surveys
```
