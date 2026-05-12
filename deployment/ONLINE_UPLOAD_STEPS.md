# Online Image Upload Setup

The app now supports two upload providers:

- `local`: stores files in `APP_UPLOAD_DIR`; good for local development.
- `cloudinary`: stores images on Cloudinary and saves public HTTPS URLs; recommended for Railway/online deployment.

## Recommended Railway Setup

Create a free Cloudinary account, then open:

```text
Dashboard -> API Keys
```

Copy these values:

```properties
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

In Railway, add these variables to the app service:

```properties
APP_UPLOAD_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CLOUDINARY_FOLDER=trototn/rooms
```

After this, uploaded images are stored outside Railway's container filesystem, so they remain available after restart/redeploy.

## Local Development

For local development, do not set `APP_UPLOAD_PROVIDER`, or set:

```properties
APP_UPLOAD_PROVIDER=local
APP_UPLOAD_DIR=uploads/
```

Local upload URLs remain:

```text
/uploads/<filename>
```

## Optional Railway Volume Alternative

If you do not want Cloudinary, attach a Railway Volume to the app service and set:

```properties
APP_UPLOAD_PROVIDER=local
APP_UPLOAD_DIR=/data/uploads/
```

Cloudinary is still preferred because it works across redeploys and does not depend on a single container disk.
