# S3 Image Storage Design

## Goal

Move production image storage and delivery off the EC2 filesystem without changing the existing database file fields. Keep the S3 bucket private and return short-lived S3 Presigned URLs from authenticated backend responses.

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

The backend signs each returned S3 object key for 15 minutes. Existing `path`, `saveName`, and `ext` fields remain in every response for backward compatibility, and a new nullable `url` field carries the Presigned URL. The frontend uses `url` first and falls back to the legacy fields only when the backend does not provide it.

S3 remains fully private and viewers access objects directly through the signed S3 URL. No CloudFront distribution, custom domain, or EC2 image proxy is required. Existing `/image/...` service on EC2 remains available temporarily only for rollback compatibility.

Authorization to receive a URL follows the API endpoint's existing access control. A later diary visibility policy can decide who receives a URL without moving the underlying S3 object.

### Configuration

Production receives only these new settings:

```yaml
victory-fairy:
  file:
    s3-enabled: true
    s3-bucket: victory-fairy-prod-files-411511125457-ap-northeast-2-an
    s3-region: ap-northeast-2
    presigned-url-duration: 15m
```

No access keys are stored. AWS SDK credential resolution uses the attached EC2 role.

## Verification and Rollback

- Unit tests verify S3 object keys, successful upload behavior, failure propagation, workspace cleanup timing, and the Presigned URL expiry/key.
- Build and existing core-file tests must pass before deployment.
- A production smoke test uploads an image and confirms the original plus every configured resize through returned Presigned URLs.
- EC2 image originals and the handover tar remain untouched until production reads and writes are verified.
- Rollback restores the previous `core-file` JAR and frontend image base URL; the S3 copy remains harmless.

## Deferred Work

Fine-grained diary visibility is deferred. When community sharing is introduced, the API authorization policy will issue Presigned URLs to viewers allowed by each diary's visibility setting. This does not alter the S3 object-key layout.
