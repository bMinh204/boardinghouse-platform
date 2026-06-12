# Deploy On Koyeb + TiDB Cloud

Use this when Render free is too slow. Koyeb can deploy the existing Dockerfile, so the Spring Boot app does not need to be split into frontend/backend.

## Current Architecture

- App host: Koyeb Web Service / App
- Database: TiDB Cloud Starter, MySQL-compatible
- Source repo: `bMinh204/boardinghouse-platform`
- Branch: `main`
- Runtime: Dockerfile

## 1. Prepare TiDB

You already created:

```text
Database: boarding_house_platform
Host: gateway01.ap-southeast-1.prod.alicloud.tidbcloud.com
Port: 4000
Username: 2Re9bNnKzAiRxHt.root
```

Keep your current TiDB password private. If it was exposed, generate a new one in TiDB and update the host platform environment variable.

## 2. Create Koyeb App

1. Go to https://app.koyeb.com.
2. Choose **Create App**.
3. Select **GitHub** as the deployment source.
4. Choose repo:

```text
bMinh204/boardinghouse-platform
```

5. Choose branch:

```text
main
```

6. For build/runtime, choose Dockerfile if Koyeb asks:

```text
Dockerfile
```

7. Leave root directory empty, because the `Dockerfile` is at the repository root.

## 3. Service Settings

Recommended values:

```text
App name: trototn-boardinghouse
Service name: web
Instance type: Free / smallest available
Region: Singapore if available, otherwise nearest Asia or default free region
Port: 8080
```

The Dockerfile also passes `--server.port=${PORT:-8080}`, so it works if Koyeb injects `PORT`.

## 4. Environment Variables

Set these in Koyeb:

```properties
DB_URL=jdbc:mysql://gateway01.ap-southeast-1.prod.alicloud.tidbcloud.com:4000/boarding_house_platform?sslMode=REQUIRED&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true
DB_USERNAME=2Re9bNnKzAiRxHt.root
DB_PASSWORD=YOUR_TIDB_PASSWORD
APP_UPLOAD_PROVIDER=local
CHATBOT_PROVIDER=local
APP_BOOTSTRAP_ADMIN_EMAIL=admin@trototn.vn
APP_BOOTSTRAP_ADMIN_PASSWORD=Admin@12345678
```

After Koyeb gives you the public URL, add/update:

```properties
APP_FRONTEND_URL=https://YOUR_KOYEB_APP_URL
```

Then redeploy once.

## 5. First Login

After a successful deploy, login with:

```text
Email: admin@trototn.vn
Password: Admin@12345678
```

The app now updates the bootstrap admin if it already exists, so this account should be promoted to `ADMIN`, unlocked, activated, and reset to the environment password.

## 6. Notes

- If image upload uses `APP_UPLOAD_PROVIDER=local`, uploaded files may disappear after redeploy. Use Cloudinary later for persistent images.
- If login fails, check Koyeb logs for:

```text
Bootstrap admin account was created
```

or:

```text
Bootstrap admin account was updated
```

- If database connection fails, regenerate the TiDB password and update `DB_PASSWORD`.
