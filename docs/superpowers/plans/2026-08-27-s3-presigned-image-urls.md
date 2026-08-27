# S3 Presigned Image URLs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Return 15-minute S3 Presigned URLs for every profile and diary image while keeping existing file fields compatible.

**Architecture:** A shared `S3PresignedUrlService` signs object keys using the EC2 role and configuration already used for uploads. Backend DTOs add a nullable `url` field; the frontend consumes it first and retains the legacy path builder only as rollback fallback.

**Tech Stack:** Java 17, Spring Boot, AWS SDK for Java v2 2.49.6, JUnit 5, S3, Next.js

**Spec:** `docs/superpowers/specs/2026-08-27-s3-image-storage-design.md`

## Global Constraints

- Keep S3 public-access blocking enabled and do not add CloudFront.
- Use the EC2 role only; never store AWS access keys.
- Preserve existing `path`, `saveName`, and `ext` response fields.
- Use a configurable `15m` default Presigned URL duration.
- Do not delete restored EC2 files or the handover tar.
- Do not add the previously proposed TODO comment.

---

### Task 1: Shared Presigned URL service

**Files:**
- Modify: `support/common/build.gradle`
- Modify: `support/common/src/main/java/kr/co/victoryfairy/support/properties/FileProperties.java`
- Create: `support/common/src/main/java/kr/co/victoryfairy/support/config/S3PresignerConfiguration.java`
- Create: `support/common/src/main/java/kr/co/victoryfairy/support/service/S3PresignedUrlService.java`
- Test: `support/common/src/test/java/kr/co/victoryfairy/support/service/S3PresignedUrlServiceTest.java`

**Interfaces:**
- Produces: `String create(String path, String saveName, String ext)` and returns `null` when S3 is disabled.

- [ ] Write a failing test that signs literal key `image/profile/202608/sample.jpg` for exactly 15 minutes.
- [ ] Run `./gradlew :support:common:test --tests '*S3PresignedUrlServiceTest'` and verify RED.
- [ ] Add AWS SDK `s3` 2.49.6, `S3Presigner`, the `presignedUrlDuration` property, and the minimal signing service.
- [ ] Re-run the focused test and verify GREEN.

### Task 2: Add URLs to backend image responses

**Files:**
- Modify: `core/core-file/src/main/java/kr/co/victoryfairy/core/file/domain/FileDomain.java`
- Modify: `core/core-file/src/main/java/kr/co/victoryfairy/core/file/service/impl/FileServiceImpl.java`
- Modify: `core/core-api/src/main/java/kr/co/victoryfairy/core/api/domain/DiaryDomain.java`
- Modify: `core/core-api/src/main/java/kr/co/victoryfairy/core/api/domain/FreeDiaryDomain.java`
- Modify: `core/core-api/src/main/java/kr/co/victoryfairy/core/api/domain/MyPageDomain.java`
- Modify: `core/core-api/src/main/java/kr/co/victoryfairy/core/api/service/impl/DiaryServiceImpl.java`
- Modify: `core/core-api/src/main/java/kr/co/victoryfairy/core/api/service/impl/FreeDiaryServiceImpl.java`
- Modify: `core/core-api/src/main/java/kr/co/victoryfairy/core/api/service/impl/MyPageServiceImpl.java`
- Test: focused service tests for file upload and one image response mapping.

**Interfaces:**
- Produces: existing image JSON plus nullable `url`.

- [ ] Write failing tests proving upload and API image responses contain the URL returned by the signer.
- [ ] Add `url` as the final record field and call the shared signer at every existing image mapping.
- [ ] Run `./gradlew :core:core-file:test :core:core-api:test` and verify GREEN.

### Task 3: Frontend URL preference

**Files:**
- Modify: `/Users/kimgho/Desktop/FE-Next/src/api/api.type.ts`
- Modify: `/Users/kimgho/Desktop/FE-Next/src/api/file/file.type.ts`
- Modify: `/Users/kimgho/Desktop/FE-Next/src/utils/image.ts`
- Modify hardcoded calendar/create image call sites only where they bypass the shared helper.

**Interfaces:**
- Consumes: nullable backend `url`.
- Produces: direct signed S3 image requests with legacy fallback.

- [ ] Write a failing utility test if the existing frontend test runner covers `src/utils`.
- [ ] Return `url` first in the shared image builders and file model; remove hardcoded image-domain construction from remaining call sites.
- [ ] Run the repository's existing lint, tests, and production build.

### Task 4: Production rollout

**Files:**
- Modify on EC2: `/opt/victoryfairy/config/api/runtime.env` and `/opt/victoryfairy/config/file/runtime.env`
- Deploy: `core-api.jar` and `core-file.jar`

**Interfaces:**
- Consumes: bucket, region, duration, and EC2 role.
- Produces: production APIs returning working 15-minute S3 URLs.

- [ ] Configure both services with S3 enabled, bucket, region, and `15m` duration without credentials.
- [ ] Back up both JARs, deploy, restart, and verify health.
- [ ] Verify a restored profile object through an API-issued URL while direct unsigned S3 access remains denied.
- [ ] Deploy the frontend and confirm profile and diary images bypass EC2.
