# S3 Image Storage Design

## Goal

Move production image storage and delivery off the EC2 filesystem without changing the existing database file fields. Keep the S3 bucket private, serve images through the default CloudFront domain first, and leave authenticated Presigned URLs as a later privacy upgrade.

## Current State

- `core-file` writes originals and resized images under `victory-fairy.file.storage-path`.
- Nginx serves `/image/...` from `/opt/victoryfairy/data/file/image/`.
- The restored production set contains 13,981 objects (6,057,725,016 bytes) and has already been copied to `s3://victory-fairy-prod-files-411511125457-ap-northeast-2-an/image/`.
- Database rows store relative `path`, `saveName`, and `ext` values, so no database migration is required.

## Architecture

### Image writes

`core-file` continues to use local disk only as a processing workspace because the existing resize code requires `File` objects. In the production profile it uploads the original and generated resize files to the private S3 bucket using the EC2 instance role, then removes only those newly generated workspace files after every upload succeeds. Local and development profiles retain the existing filesystem behavior.

The S3 object key remains the existing relative layout:

```text
image/{refType}/{yyyyMM}/{saveName}.{ext}
image/{refType}/{yyyyMM}/{saveName}_{size}.{ext}
```

The database write occurs only after S3 upload succeeds. A failed upload returns the existing file-upload failure response and does not create a file row.

### Image reads

A CloudFront distribution uses the private S3 bucket as its origin with Origin Access Control and signed origin requests. Viewers use the distribution's default `d*.cloudfront.net` domain, so no new DNS record or ACM certificate is required.

The frontend constructs image URLs from a deploy-time image base URL and the existing relative file fields. Existing `/image/...` service on EC2 remains available temporarily as rollback compatibility, but newly deployed clients request CloudFront directly and do not traverse EC2.

### Configuration

Production receives only these new settings:

```yaml
victory-fairy:
  file:
    s3-enabled: true
    s3-bucket: victory-fairy-prod-files-411511125457-ap-northeast-2-an
    s3-region: ap-northeast-2
```

No access keys are stored. AWS SDK credential resolution uses the attached EC2 role.

## Verification and Rollback

- Unit tests verify S3 object keys, successful upload behavior, failure propagation, and workspace cleanup timing.
- Build and existing core-file tests must pass before deployment.
- A production smoke test uploads an image and confirms the original plus every configured resize through CloudFront.
- EC2 image originals and the handover tar remain untouched until production reads and writes are verified.
- Rollback restores the previous `core-file` JAR and frontend image base URL; the S3 copy remains harmless.

## Deferred Work

Authenticated Presigned GET URLs are intentionally deferred. When diary images must be limited to authorized members, API responses will issue short-lived S3 Presigned URLs and the frontend will refresh expired URLs. This later change does not alter the S3 object-key layout introduced here.
