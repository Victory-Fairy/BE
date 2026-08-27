# S3 Image Storage Implementation Plan

> Superseded by `docs/superpowers/plans/2026-08-27-s3-presigned-image-urls.md` after the delivery design changed from CloudFront to S3 Presigned URLs.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Store production uploads in private S3 and serve restored images directly through the default CloudFront domain while preserving existing database paths.

**Architecture:** `core-file` keeps local disk only as its image-processing workspace, uploads every generated file through AWS SDK for Java v2, and deletes new workspace files only after successful upload. CloudFront reads the private bucket through OAC; the frontend uses the CloudFront domain directly and retains one TODO for the later Presigned URL policy.

**Tech Stack:** Java 17, Spring Boot, AWS SDK for Java v2 2.49.6, JUnit 5, Mockito, S3, CloudFront OAC, Next.js

**Spec:** `docs/superpowers/specs/2026-08-27-s3-image-storage-design.md`

## Global Constraints

- Keep `victory-fairy-prod-files-411511125457-ap-northeast-2-an` private with public-access blocking enabled.
- Use the EC2 instance role; never add static AWS access keys.
- Preserve `image/{refType}/{yyyyMM}/{saveName}[_{size}].{ext}` object keys and existing DB fields.
- Preserve local filesystem storage when `victory-fairy.file.s3-enabled=false`.
- Do not delete the restored EC2 files or handover tar during this migration.
- Add exactly one follow-up comment in the frontend image URL builder: `// TODO: 이미지 접근 정책 확정 후 Presigned URL 방식으로 변경`.

---

### Task 1: S3 object uploader

**Files:**
- Modify: `core/core-file/build.gradle`
- Modify: `support/common/src/main/java/kr/co/victoryfairy/support/properties/FileProperties.java`
- Create: `core/core-file/src/main/java/kr/co/victoryfairy/core/file/config/S3FileConfiguration.java`
- Create: `core/core-file/src/main/java/kr/co/victoryfairy/core/file/service/S3FileUploader.java`
- Test: `core/core-file/src/test/java/kr/co/victoryfairy/core/file/service/S3FileUploaderTest.java`

**Interfaces:**
- Consumes: a local processing root and generated `Path` values.
- Produces: `void upload(Path storageRoot, List<Path> files)`; keys are normalized relative paths using `/`.

- [ ] **Step 1: Write the failing uploader test**

Test with a temporary root containing `image/profile/202608/sample.jpg`; mock `S3Client` and assert `PutObjectRequest.bucket`, `.key`, and `.contentType` equal the configured values.

- [ ] **Step 2: Run the test and verify RED**

Run: `./gradlew :core:core-file:test --tests '*S3FileUploaderTest'`

Expected: compilation fails because `S3FileUploader` does not exist.

- [ ] **Step 3: Add the minimum AWS SDK and uploader**

Use the AWS SDK BOM and S3 client:

```gradle
implementation platform('software.amazon.awssdk:bom:2.49.6')
implementation 'software.amazon.awssdk:s3'
```

Create `S3Client` only when `victory-fairy.file.s3-enabled=true`, with `Region.of(fileProperties.getS3Region())` and the default credential provider chain. `S3FileUploader.upload` calls `putObject` with `RequestBody.fromFile(file)`.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `./gradlew :core:core-file:test --tests '*S3FileUploaderTest'`

Expected: PASS.

### Task 2: Upload processed files before database persistence

**Files:**
- Modify: `core/core-file/src/main/java/kr/co/victoryfairy/core/file/service/impl/FileServiceImpl.java`
- Test: `core/core-file/src/test/java/kr/co/victoryfairy/core/file/service/impl/FileServiceImplTest.java`

**Interfaces:**
- Consumes: optional `S3FileUploader` supplied only in S3 mode.
- Produces: existing `createFile(CreateRequest)` response and database rows, unchanged.

- [ ] **Step 1: Write failing service tests**

Use `MockMultipartFile`, a temporary `storagePath`, empty resize arrays, mocked repositories, and a mocked uploader. Verify:

```text
successful S3 upload -> repository saveAll executes and generated workspace file is deleted
failed S3 upload -> repository saveAll never executes and FAIL_UPLOAD propagates
uploader absent -> generated file remains for local serving
```

- [ ] **Step 2: Run the tests and verify RED**

Run: `./gradlew :core:core-file:test --tests '*FileServiceImplTest'`

Expected: FAIL because the service does not upload or clean up processed files.

- [ ] **Step 3: Return generated paths from processing and upload once**

Make `saveFile` return the original plus resize output paths. In `convertFile`, call the optional uploader after all generated paths exist, then delete only those paths after upload succeeds. Do not catch an uploader failure before `createFile` reaches `fileRepository.saveAll`.

- [ ] **Step 4: Run focused and module tests**

Run:

```bash
./gradlew :core:core-file:test --tests '*FileServiceImplTest'
./gradlew :core:core-file:test
```

Expected: PASS.

### Task 3: Configure and deploy production S3 writes

**Files:**
- Modify on EC2: `/opt/victoryfairy/config/file/runtime.env`
- Deploy on EC2: `/opt/victoryfairy/apps/core-file.jar`

**Interfaces:**
- Consumes: EC2 role `ec-2-role` and the existing private bucket.
- Produces: production file uploads stored under the existing S3 `image/` layout.

- [ ] **Step 1: Build the core-file JAR**

Run: `./gradlew :core:core-file:clean :core:core-file:bootJar -Pci`

- [ ] **Step 2: Add runtime settings without credentials**

```text
VICTORY_FAIRY_FILE_S3_ENABLED=true
VICTORY_FAIRY_FILE_S3_BUCKET=victory-fairy-prod-files-411511125457-ap-northeast-2-an
VICTORY_FAIRY_FILE_S3_REGION=ap-northeast-2
```

- [ ] **Step 3: Back up and replace only core-file.jar, then restart**

Confirm `victoryfairy@file` is active and `/v2/file/api-docs` returns HTTP 200.

- [ ] **Step 4: Smoke-test one upload**

Upload one test image through the existing authenticated application flow, confirm its original and configured resize keys exist in S3, and confirm no new permanent file remains in the EC2 workspace.

### Task 4: Create private S3 CloudFront delivery

**Files:**
- AWS resources: one CloudFront standard distribution and its OAC/bucket policy.

**Interfaces:**
- Consumes: private S3 bucket origin.
- Produces: CloudFront `Distribution.DomainName` for frontend configuration.

- [ ] **Step 1: Create a standard distribution**

Choose the S3 bucket origin, create OAC with `Sign requests (recommended)`, redirect HTTP to HTTPS, allow only GET/HEAD, and use the caching-optimized managed policy.

- [ ] **Step 2: Apply the generated bucket policy**

Allow `cloudfront.amazonaws.com` only `s3:GetObject` on the bucket objects, conditioned on the created distribution ARN. Keep S3 public-access blocking enabled.

- [ ] **Step 3: Verify restored content**

Request the known object `image/profile/202605/e0949ae4-cc91-4d95-9022-503bc86ab8dc_1024.jpeg` through `Distribution.DomainName`; expect HTTP 200 and `image/jpeg`. Direct unauthenticated S3 access must remain denied.

### Task 5: Point the frontend directly at CloudFront

**Files:**
- Modify: `/Users/kimgho/Desktop/FE-Next/src/utils/image.ts`
- Modify: `/Users/kimgho/Desktop/FE-Next/src/api/file/file.type.ts`
- Modify: `/Users/kimgho/Desktop/FE-Next/next.config.js`

**Interfaces:**
- Consumes: CloudFront `Distribution.DomainName` from Task 4.
- Produces: image URLs that bypass EC2.

- [ ] **Step 1: Add one failing URL-builder test if the repository already has a compatible test runner**

Assert a relative `image/...` value becomes `https://<Distribution.DomainName>/image/...`. If no test runner covers this utility, obtain explicit approval to treat the environment-only edit as the TDD exception.

- [ ] **Step 2: Use one image base environment variable**

Set `NEXT_PUBLIC_IMAGE_URL` in production deployment and make both existing image builders use it. Add exactly this comment beside the builder:

```ts
// TODO: 이미지 접근 정책 확정 후 Presigned URL 방식으로 변경
```

Allow the CloudFront hostname in `next.config.js` without removing the old hostname during rollback.

- [ ] **Step 3: Build and deploy the frontend**

Run its existing lint, test, and production build commands, then deploy through the existing main-branch workflow.

- [ ] **Step 4: Verify the user-visible recovery**

Confirm an existing profile image and diary image load from the CloudFront hostname, and confirm the browser makes no image request to EC2 for the newly deployed frontend.
