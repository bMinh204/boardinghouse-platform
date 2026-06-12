# Deploy Free: Render + TiDB Cloud

This project is a Spring Boot app that currently uses MySQL. The lowest-friction free replacement for Railway is:

- Render: hosts the Spring Boot web service.
- TiDB Cloud Starter: hosts a MySQL-compatible database.

## 1. Create A TiDB Cloud Starter Database

1. Go to https://tidbcloud.com and create a free Starter cluster.
2. Open the cluster, click **Connect**, and choose a MySQL-compatible connection.
3. Create or select database:

```sql
CREATE DATABASE IF NOT EXISTS boarding_house_platform;
```

4. Save these values:

```text
Host
Port, usually 4000
Username, usually includes a TiDB prefix
Password
Database: boarding_house_platform
```

Example Render variables:

```properties
DB_URL=jdbc:mysql://YOUR_TIDB_HOST:4000/boarding_house_platform?sslMode=REQUIRED&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true
DB_USERNAME=YOUR_TIDB_USERNAME
DB_PASSWORD=YOUR_TIDB_PASSWORD
```

If TiDB gives a username with a prefix, keep the full username exactly as shown.

## 2. Import Current Data

If you want to copy existing data from the SQL dump:

```powershell
& "E:\DATN\mysql.exe" `
  -h YOUR_TIDB_HOST `
  -P 4000 `
  -u YOUR_TIDB_USERNAME `
  -p `
  --ssl-mode=REQUIRED `
  --default-character-set=utf8mb4 `
  boarding_house_platform `
  < "E:\DATN\project\boardinghouse_platform\deployment\boarding_house_platform_online_seed.sql"
```

If the import fails because some tables already exist, create a fresh empty database or drop the existing generated tables first.

## 3. Create Render Web Service

1. Go to https://dashboard.render.com.
2. Create **New Web Service**.
3. Connect GitHub repo:

```text
bMinh204/boardinghouse-platform
```

4. Choose branch:

```text
main
```

5. Render should detect the `Dockerfile`. If asked:

```text
Runtime: Docker
Plan: Free
```

6. Add environment variables:

```properties
DB_URL=jdbc:mysql://YOUR_TIDB_HOST:4000/boarding_house_platform?sslMode=REQUIRED&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true
DB_USERNAME=YOUR_TIDB_USERNAME
DB_PASSWORD=YOUR_TIDB_PASSWORD
APP_FRONTEND_URL=https://YOUR_RENDER_APP.onrender.com
APP_UPLOAD_PROVIDER=local
CHATBOT_PROVIDER=local
```

7. Deploy.

## 4. Important Notes

- Render free services can sleep when idle. First load after sleeping can be slow.
- `APP_UPLOAD_PROVIDER=local` works for demo, but uploaded images can disappear after redeploy because free containers do not provide permanent file storage.
- For permanent image hosting, use Cloudinary and set:

```properties
APP_UPLOAD_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=trototn/rooms
```

## 5. Verify

Open:

```text
https://YOUR_RENDER_APP.onrender.com/
```

Then test:

- Register/login.
- View room list.
- Add or update a room.
- Open room detail.
- Check layout and rental flow.
