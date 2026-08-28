# VictoryFairy Docker Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the current VictoryFairy production service set to the new ARM64 EC2 with Docker Compose, ECR, GitHub Actions, and SSM while preserving the current RDS, S3, Redis sessions, domain, and rollback path.

**Architecture:** GitHub Actions builds the four Spring Boot JARs once, packages minimal ARM64 runtime images, pushes them to one ECR repository, and asks SSM to run a preinstalled deployment script on `i-01d540e5b0ed6e30d`. Docker Compose runs Nginx, API, file, admin, and Redis continuously; systemd timers run the crawler as an ephemeral Compose job.

**Tech Stack:** Java 17, Gradle, Docker Engine 29, Docker Compose 2.40, Amazon ECR, GitHub Actions OIDC, AWS SSM, Nginx, Redis 7, Playwright Java 1.48, Ubuntu 24.04 ARM64

**Spec:** `docs/superpowers/specs/2026-08-28-docker-deployment-design.md`

## Global Constraints

- Target platform is `linux/arm64` on EC2 `i-01d540e5b0ed6e30d` in `ap-northeast-2`.
- Deploy only `core-api`, `core-file`, `core-admin`, `core-craw`, Nginx, and Redis.
- Do not deploy `core-event` or `core-batch`.
- Do not store application secrets, OAuth values, JWT values, certificates, or AWS long-lived keys in Git or Docker images.
- Build all boot JARs in one Gradle invocation with `--parallel --build-cache -Pci`.
- Publish immutable commit-SHA tags and use `latest` only as a convenience tag.
- Do not move Elastic IP `15.164.199.125` until pre-cutover verification passes.
- Keep the old t3.small instance `i-0f73862889a8955fd` available for rollback for at least two days.

---

### Task 1: Minimal ARM64 runtime images

**Files:**
- Create: `deploy/Dockerfile`
- Create: `deploy/Dockerfile.craw`
- Create: `deploy/Dockerfile.dockerignore`
- Create: `deploy/Dockerfile.craw.dockerignore`
- Create: `deploy/scripts/stage-jars.sh`

**Interfaces:**
- Consumes: Gradle output from `core/core-{api,file,admin,craw}/build/libs/core-*.jar`.
- Produces: four build contexts at `build/docker/{api,file,admin,craw}/app.jar` and two Dockerfiles that run `/app/app.jar`.

- [ ] **Step 1: Add the JAR staging check**

Create `deploy/scripts/stage-jars.sh` with strict shell mode, a fixed service list, exactly-one-JAR validation, and copy to `build/docker/<service>/app.jar`:

```bash
#!/usr/bin/env bash
set -euo pipefail

for service in api file admin craw; do
  jar_dir="core/core-${service}/build/libs"
  jar_file="${jar_dir}/core-${service}.jar"
  test -f "$jar_file"
  install -D -m 0644 "$jar_file" "build/docker/${service}/app.jar"
done
```

- [ ] **Step 2: Verify staging fails before JARs exist**

Run:

```bash
rm -rf build/docker
bash deploy/scripts/stage-jars.sh
```

Expected: non-zero exit because one or more required JARs are absent.

- [ ] **Step 3: Add the runtime Dockerfiles**

`deploy/Dockerfile` uses `eclipse-temurin:17-jre`, creates UID/GID 10001, copies only `app.jar`, and starts Java with `JAVA_TOOL_OPTIONS` supplied by Compose.

`deploy/Dockerfile.craw` uses `mcr.microsoft.com/playwright/java:v1.48.0-noble`, copies only `app.jar`, uses the existing `pwuser`, and starts the same Java command. Add Dockerfile-specific ignore files containing only:

```dockerignore
**
!app.jar
```

- [ ] **Step 4: Build all boot JARs once**

Run:

```bash
./gradlew :core:core-api:bootJar :core:core-file:bootJar :core:core-admin:bootJar :core:core-craw:bootJar -Pci --parallel --build-cache
bash deploy/scripts/stage-jars.sh
```

Expected: four `build/docker/*/app.jar` files exist.

- [ ] **Step 5: Build and inspect ARM64 images**

Run:

```bash
docker buildx build --platform linux/arm64 --load -f deploy/Dockerfile -t victory-fairy-api:test build/docker/api
docker buildx build --platform linux/arm64 --load -f deploy/Dockerfile.craw -t victory-fairy-craw:test build/docker/craw
docker image inspect victory-fairy-api:test --format '{{.Architecture}}'
docker image inspect victory-fairy-craw:test --format '{{.Architecture}}'
```

Expected: both outputs are `arm64`.

- [ ] **Step 6: Commit runtime image files**

```bash
git add deploy/Dockerfile deploy/Dockerfile.craw deploy/Dockerfile.dockerignore deploy/Dockerfile.craw.dockerignore deploy/scripts/stage-jars.sh
git commit -m "build: add ARM64 runtime images"
```

### Task 2: Compose, Nginx, and crawler timers

**Files:**
- Create: `deploy/compose.yaml`
- Create: `deploy/env.example`
- Create: `deploy/nginx/victoryfairy.conf`
- Create: `deploy/systemd/victoryfairy-game-sync@.service`
- Create: `deploy/systemd/victoryfairy-game-sync-today.timer`
- Create: `deploy/systemd/victoryfairy-game-sync-yesterday.timer`
- Create: `deploy/scripts/verify-compose.sh`

**Interfaces:**
- Consumes: `${ECR_REGISTRY}`, `${ECR_REPOSITORY}`, `${IMAGE_TAG}`, Spring environment variables, mounted config files, and mounted certificates.
- Produces: HTTP routes `/v2/api/`, `/v2/file/`, `/v2/admin/` and scheduled crawler execution through the `jobs` profile.

- [ ] **Step 1: Add a failing Compose validation script**

Create `deploy/scripts/verify-compose.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
docker compose --env-file deploy/env.example -f deploy/compose.yaml config --quiet
grep -q 'proxy_pass http://api:8081' deploy/nginx/victoryfairy.conf
grep -q 'proxy_pass http://file:8082' deploy/nginx/victoryfairy.conf
grep -q 'proxy_pass http://admin:8084' deploy/nginx/victoryfairy.conf
```

Run `bash deploy/scripts/verify-compose.sh` and expect failure because the referenced files do not exist.

- [ ] **Step 2: Add Compose services**

Create `deploy/compose.yaml` with:

- `nginx:1.27-alpine` publishing 80 and 443 only.
- `redis:7.0-alpine` using bind mount `/opt/victoryfairy/redis:/data` and no host port.
- API, file, and admin images using `${ECR_REGISTRY}/${ECR_REPOSITORY}:<service>-${IMAGE_TAG}`.
- Crawler image using the same tag convention and `profiles: [jobs]`.
- Read-only config mounts from `/opt/victoryfairy/config/<service>` to `/config`.
- Read-only Nginx and `/etc/letsencrypt` mounts.
- `SPRING_PROFILES_ACTIVE=prod`, `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/`, Redis host `redis`, port `6379`, database `1`.
- `restart: unless-stopped` for Nginx, Redis, API, file, and admin.
- JVM limits: API `-Xms128m -Xmx768m`, file `-Xms128m -Xmx512m`, admin `-Xms64m -Xmx384m`, crawler `-Xms64m -Xmx512m`.

- [ ] **Step 3: Add the non-secret environment template**

Create `deploy/env.example` containing exact variable names with safe dummy values:

```dotenv
ECR_REGISTRY=411511125457.dkr.ecr.ap-northeast-2.amazonaws.com
ECR_REPOSITORY=victory-fairy/backend
IMAGE_TAG=local
SPRING_PROFILES_ACTIVE=prod
TZ=Asia/Seoul
STORAGE_DATASOURCE_CORE_JDBC_URL=jdbc:mysql://db.invalid:3306/victoryFairy
STORAGE_DATASOURCE_CORE_USERNAME=invalid
STORAGE_DATASOURCE_CORE_PASSWORD=invalid
JWT_SECRET_KEY=invalid
AUTH_KAKAO_CAS_CLIENT_SECRET=invalid
AUTH_GOOGLE_CAS_CLIENT_SECRET=invalid
WEBHOOK_SLACK_URL=http://127.0.0.1/disabled
VICTORY_FAIRY_FILE_S3_ENABLED=true
VICTORY_FAIRY_FILE_S3_BUCKET=invalid
VICTORY_FAIRY_FILE_S3_REGION=ap-northeast-2
VICTORY_FAIRY_FILE_STORAGE_PATH=/tmp/victoryfairy
```

- [ ] **Step 4: Add Nginx routing and TLS**

Create `deploy/nginx/victoryfairy.conf` with server name `api.seungyo.shop`, HTTP-to-HTTPS redirect, TLS certificate paths under `/etc/letsencrypt/live/api.seungyo.shop/`, request body limit 10 MiB, forwarded headers, and upstream routes to `api:8081`, `file:8082`, and `admin:8084`.

- [ ] **Step 5: Port the crawler timers to Compose**

Keep the current schedules:

```ini
OnCalendar=*-*-* 06:00:00 Asia/Seoul
OnCalendar=*-*-* 23:30:00 Asia/Seoul
```

and:

```ini
OnCalendar=*-*-* 00:30:00 Asia/Seoul
```

The templated service runs:

```ini
ExecStart=/usr/bin/flock --nonblock /run/victoryfairy-game-sync/job.lock /usr/bin/docker compose --env-file /opt/victoryfairy/.env -f /opt/victoryfairy/compose.yaml --profile jobs run --rm craw --spring.main.web-application-type=none --game-recovery.enabled=true --game-recovery.days-ago=%i --game-recovery.dry-run=false
```

- [ ] **Step 6: Validate and commit Compose assets**

Run:

```bash
bash -n deploy/scripts/verify-compose.sh
bash deploy/scripts/verify-compose.sh
```

Expected: both commands pass.

```bash
git add deploy/compose.yaml deploy/env.example deploy/nginx deploy/systemd deploy/scripts/verify-compose.sh
git commit -m "build: add production Compose stack"
```

### Task 3: GitHub Actions build, push, and SSM deployment

**Files:**
- Create: `.github/workflows/deploy-backend.yml`
- Create: `deploy/scripts/deploy.sh`

**Interfaces:**
- Consumes: GitHub OIDC role `arn:aws:iam::411511125457:role/victory-fairy-github-deploy`, ECR repository `victory-fairy/backend`, EC2 `i-01d540e5b0ed6e30d`.
- Produces: ECR tags `api-<sha>`, `file-<sha>`, `admin-<sha>`, `craw-<sha>` and an SSM deployment using the same SHA.

- [ ] **Step 1: Add the server deployment script**

Create `deploy/scripts/deploy.sh` that requires one 40-character Git SHA, logs into ECR using the instance role, updates only `IMAGE_TAG` in `/opt/victoryfairy/.env`, validates Compose, pulls the three continuous application images, starts the continuous stack, and prints `docker compose ps`. It must not run the crawler during a normal deployment.

- [ ] **Step 2: Add workflow validation before deployment**

Create `.github/workflows/deploy-backend.yml` for pushes to `main` and manual dispatch. Use Java 17 with Gradle caching and run:

```bash
./gradlew unitTest :core:core-api:bootJar :core:core-file:bootJar :core:core-admin:bootJar :core:core-craw:bootJar -Pci --parallel --build-cache
bash deploy/scripts/stage-jars.sh
```

The workflow must stop before AWS authentication if tests or staging fail.

- [ ] **Step 3: Add ARM64 image publishing with BuildKit cache**

Authenticate through GitHub OIDC, log into ECR, configure Buildx, then build each service with:

```text
platforms: linux/arm64
cache-from: type=gha,scope=victory-fairy-<service>
cache-to: type=gha,mode=max,scope=victory-fairy-<service>
tags: <registry>/victory-fairy/backend:<service>-<sha>,<registry>/victory-fairy/backend:<service>-latest
```

Use `deploy/Dockerfile.craw` only for `craw`; use `deploy/Dockerfile` for the other services.

- [ ] **Step 4: Add SSM deployment and result polling**

Send `AWS-RunShellScript` to `i-01d540e5b0ed6e30d` with:

```bash
sudo /opt/victoryfairy/bin/deploy.sh "$GITHUB_SHA"
```

Poll `get-command-invocation` until success or failure and print only deployment output without environment values.

- [ ] **Step 5: Validate workflow syntax and commit**

Run:

```bash
bash -n deploy/scripts/deploy.sh
./gradlew unitTest
```

Expected: shell syntax and unit tests pass.

```bash
git add .github/workflows/deploy-backend.yml deploy/scripts/deploy.sh
git commit -m "ci: deploy backend images through ECR and SSM"
```

### Task 4: Provision ECR, OIDC, and the new EC2

**Files:**
- Deploy from: `deploy/compose.yaml`
- Deploy from: `deploy/nginx/victoryfairy.conf`
- Deploy from: `deploy/scripts/deploy.sh`
- Deploy from: `deploy/systemd/*`

**Interfaces:**
- Consumes: AWS account `411511125457`, GitHub repository `Victory-Fairy/BE`, existing EC2 role `ec-2-role`.
- Produces: ECR repository, restricted GitHub deploy role, `/opt/victoryfairy` runtime layout, and enabled crawler timers.

- [ ] **Step 1: Create the ECR repository**

Create `victory-fairy/backend` in `ap-northeast-2` with tag immutability disabled only because each `*-latest` tag is intentionally updated. Enable scan-on-push and configure lifecycle retention for the newest 20 untagged images.

- [ ] **Step 2: Create the GitHub OIDC deploy role**

Trust only:

```text
repo:Victory-Fairy/BE:ref:refs/heads/main
```

Grant only ECR push actions for `victory-fairy/backend`, `ecr:GetAuthorizationToken`, and SSM send/read actions for `i-01d540e5b0ed6e30d` using `AWS-RunShellScript`.

- [ ] **Step 3: Verify the EC2 instance role**

Ensure `ec-2-role` can use SSM and pull from `victory-fairy/backend`. Keep its existing S3 permissions unchanged. Verify the instance appears online in Systems Manager before enabling the workflow.

- [ ] **Step 4: Install AWS CLI v2 ARM64 and create directories**

On `43.203.125.41`, install the official AWS CLI v2 ARM64 package, then create:

```bash
sudo install -d -m 0755 /opt/victoryfairy/bin /opt/victoryfairy/config/api /opt/victoryfairy/config/file /opt/victoryfairy/config/admin /opt/victoryfairy/config/craw /opt/victoryfairy/nginx /opt/victoryfairy/letsencrypt /opt/victoryfairy/redis
```

- [ ] **Step 5: Copy deployment assets, production configuration, and TLS certificates**

Install Compose, Nginx, deployment script, and timers under `/opt/victoryfairy`. Create the Spring configuration from the handover structure, keeping callback URLs and non-secret production values, and move the current RDS, JWT, Kakao, Google, and S3 values into `/opt/victoryfairy/.env`. Override Redis host with `SPRING_DATA_REDIS_HOST=redis`.

Create `/opt/victoryfairy/.env` as `root:root` mode `600`. Preserve values without printing them. Copy `/etc/letsencrypt` from old EC2 `15.164.199.125` to `/opt/victoryfairy/letsencrypt` with private-key permissions preserved so the pre-cutover Nginx container can start.

- [ ] **Step 6: Install and enable timers without running them**

Install units into `/etc/systemd/system`, run `systemctl daemon-reload`, and enable both timers. Keep them stopped until the crawler image passes a manual dry run.

### Task 5: Pre-cutover deployment and verification

**Files:**
- No repository changes.

**Interfaces:**
- Consumes: new EC2 temporary IP `43.203.125.41`, ECR images, existing RDS and S3.
- Produces: a verified stack that is ready to receive the Elastic IP.

- [ ] **Step 1: Push the feature branch and open a PR**

Push `feature/docker-deployment`, create a PR into `main`, and wait for build validation. Merge only after all repository tests and Compose validation pass.

- [ ] **Step 2: Run the first image deployment**

Run the workflow manually for the merged commit and verify all continuous containers are running with the same SHA tag.

- [ ] **Step 3: Verify through the temporary IP**

Use a Host header without changing DNS:

```bash
curl --resolve api.seungyo.shop:443:43.203.125.41 https://api.seungyo.shop/v2/api/swagger-ui/index.html
```

Verify API, file, and admin routes, RDS reads, and an S3 image read. Do not create production diary data during this check.

- [ ] **Step 4: Run one crawler dry run**

Run:

```bash
sudo docker compose --env-file /opt/victoryfairy/.env -f /opt/victoryfairy/compose.yaml --profile jobs run --rm craw --spring.main.web-application-type=none --game-recovery.enabled=true --game-recovery.days-ago=0 --game-recovery.dry-run=true
```

Expected: Chromium launches on ARM64, the job exits successfully, and no DB write occurs.

- [ ] **Step 5: Enable crawler timers**

Start both timers and verify `systemctl list-timers 'victoryfairy-game-sync*'` shows 00:30, 06:00, and 23:30 KST schedules.

### Task 6: Redis, TLS, Elastic IP cutover, and rollback check

**Files:**
- No repository changes.

**Interfaces:**
- Consumes: old Redis DB 1, old Let's Encrypt certificate, Elastic IP `15.164.199.125`.
- Produces: production traffic on the new EC2 with preserved login sessions.

- [ ] **Step 1: Verify certificates before maintenance**

Compare the active certificate files on both EC2 instances and repeat the secure copy only if Certbot renewed them after initial provisioning. Verify Nginx configuration using `nginx -t` inside the container.

- [ ] **Step 2: Record the pre-cutover baseline**

Record Redis DB 1 key counts by type and prefix, current healthy endpoints, the old Elastic IP association, and the deployed image SHA. Do not print token values.

- [ ] **Step 3: Start maintenance and freeze writes**

Stop the old API, file, and admin services, then confirm Nginx returns the maintenance response. Save Redis only after application writes have stopped.

- [ ] **Step 4: Transfer Redis RDB**

Run a final Redis save on the old EC2, transfer `dump.rdb` to `/opt/victoryfairy/redis/dump.rdb` on the new EC2, fix ownership for the Redis container, and restart only Redis. Verify 13 refresh-token keys and the `write_diary` stream before starting applications.

- [ ] **Step 5: Start and verify the new stack**

Start the new continuous stack, verify application logs contain no RDS, Redis, S3, or configuration errors, and repeat the Host-header HTTPS checks.

- [ ] **Step 6: Reassociate the Elastic IP**

Move the allocation currently serving `15.164.199.125` from `i-0f73862889a8955fd` to `i-01d540e5b0ed6e30d`. Do not change Gabia DNS or OAuth callback URLs.

- [ ] **Step 7: Run production smoke tests**

Verify Kakao and Google login, refresh-token rotation, diary list, profile image, diary image, file upload, game list, and Nginx 4xx/5xx logs.

- [ ] **Step 8: Prove rollback is available**

Confirm the old EC2 remains running and document the single rollback action: reassociate Elastic IP `15.164.199.125` to `i-0f73862889a8955fd`. Keep the old instance for at least 48 hours without deleting its disk or Redis data.
