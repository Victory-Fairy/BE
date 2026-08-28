# VictoryFairy Docker 배포 설계

## 목표

현재 t3.small에서 JAR와 systemd로 실행 중인 VictoryFairy 백엔드를 신규 t4g.medium ARM64 EC2의 Docker Compose 환경으로 이전한다. 첫 이전은 현재 운영 범위만 동일하게 복제하고, 서비스 구조 리팩터링은 안정화 이후 별도 작업으로 진행한다.

## 배포 범위

상시 실행 서비스는 Nginx, `core-api`, `core-file`, `core-admin`, Redis다. `core-craw`는 상시 컨테이너로 실행하지 않고 systemd timer가 일회성 컨테이너를 실행한다.

이번 이전에서 `core-event`와 `core-batch`는 배포하지 않는다. 현재 운영 서버에서도 상시 실행되지 않으며, 미처리 Redis Stream과 FCM 동작을 먼저 정리해야 하기 때문이다.

## 선택한 배포 방식

배포 흐름은 다음과 같다.

```text
main push
  -> GitHub Actions
  -> Gradle 병렬 빌드
  -> ARM64 Docker 이미지 생성
  -> Amazon ECR push
  -> AWS SSM Run Command
  -> 신규 EC2에서 docker compose pull/up
```

GitHub Actions는 장기 AWS Access Key를 저장하지 않고 GitHub OIDC로 AWS 배포 역할을 Assume한다. EC2는 기존 `ec-2-role`을 사용해 ECR 이미지를 읽고 S3에 접근하며 SSM 명령을 받는다.

이미지는 커밋 SHA와 `latest` 두 태그를 사용한다. 배포와 롤백은 SHA 태그를 기준으로 수행하고 `latest`는 운영 중인 최신 이미지 확인용으로만 사용한다.

## Docker 빌드와 캐시 전략

Gradle 빌드와 Docker 이미지 패키징을 분리한다. 멀티 스테이지 Dockerfile에서 네 서비스를 각각 컴파일하면 동일한 Gradle 의존성과 공통 모듈을 반복 처리하므로 사용하지 않는다.

GitHub Actions는 다음 방식으로 빌드한다.

- `actions/setup-java`의 Gradle 캐시를 사용한다.
- `core-api`, `core-file`, `core-admin`, `core-craw`의 `bootJar` 작업을 한 번의 Gradle 실행에서 `--parallel --build-cache -Pci`로 수행한다.
- Docker build context에는 생성된 JAR와 Dockerfile만 포함한다.
- Dockerfile은 변경 빈도가 낮은 JRE 베이스 레이어와 변경되는 애플리케이션 JAR 레이어를 분리한다.
- BuildKit의 GitHub Actions 캐시를 `cache-from`과 `cache-to`에 연결한다.
- `.dockerignore`로 소스, Gradle 캐시, 테스트 결과, 인수인계 파일, 로컬 시크릿을 build context에서 제외한다.

API, 파일, 관리자 런타임 이미지는 ARM64를 지원하는 Java 17 JRE 이미지를 사용한다. 크롤러 이미지는 현재 의존성과 같은 `mcr.microsoft.com/playwright/java:v1.48.0-noble`을 사용한다. 이 태그가 `linux/arm64`를 제공하는 것은 신규 EC2에서 매니페스트로 확인했다. 실행 사용자와 작업 디렉터리를 고정하고, 애플리케이션 JAR 이외의 설정이나 시크릿을 이미지에 넣지 않는다.

## 서버 파일 구조

신규 EC2의 운영 파일은 `/opt/victoryfairy` 아래에 둔다.

```text
/opt/victoryfairy/
  compose.yaml
  .env
  config/
    api/
    file/
    admin/
    craw/
  nginx/
  letsencrypt/
  redis/
```

`.env`, Spring 설정, 인증키와 인증서 파일은 Git에 저장하지 않는다. 첫 이전에서는 Secrets Manager를 추가하지 않고 `/opt/victoryfairy/.env`에 시크릿을 저장한다. 이 파일은 `root:root`, 권한 `600`으로 유지하고 root 권한으로 실행되는 SSM 배포 스크립트만 읽는다. 비밀이 없는 Spring 설정 파일은 컨테이너에 읽기 전용으로 마운트한다.

## Compose 서비스

Nginx만 호스트의 80번과 443번 포트를 공개한다. 애플리케이션과 Redis 포트는 Compose 내부 네트워크에서만 접근한다.

- `nginx`: `/v2/api/`, `/v2/file/`, `/v2/admin/`을 각 컨테이너로 전달하고 TLS를 종료한다.
- `api`: 내부 포트 8081, Redis 호스트는 `redis`, Redis DB는 1을 사용한다.
- `file`: 내부 포트 8082, RDS와 S3에 접근한다.
- `admin`: 내부 포트 8084, Redis와 RDS에 접근한다.
- `redis`: 영속 볼륨을 사용하고 외부 포트를 공개하지 않는다.
- `craw`: Compose profile `jobs`에 두며 systemd timer에서 `docker compose run --rm craw`로 실행한다.

상시 서비스는 `restart: unless-stopped`를 사용한다. JVM 메모리는 t4g.medium의 4GiB 안에서 전체 컨테이너가 함께 동작하도록 서비스별 상한을 둔다.

## 크롤러 실행

기존 실행 시각을 유지한다.

- 당일 경기 동기화: 매일 06:00, 23:30 KST
- 전일 경기 재시도: 매일 00:30 KST

systemd oneshot 서비스는 중복 실행 방지를 위해 `flock`을 사용하고, 실행 제한 시간과 메모리 제한을 유지한다. Playwright는 Chromium만 설치하며 ARM64 이미지를 빌드할 때 필요한 브라우저와 시스템 라이브러리를 포함한다.

## 설정과 시크릿

Spring 설정은 EC2에 마운트하고 환경별 값은 환경변수로 덮어쓴다. 최소한 다음 값이 필요하다.

- RDS JDBC URL, 사용자명, 비밀번호
- Redis 호스트 `redis`, 포트 6379, DB 1
- JWT Secret
- Kakao와 Google OAuth 값
- S3 버킷과 리전
- 서비스별 Spring profile과 JVM 옵션

GitHub Actions는 애플리케이션 시크릿을 다루지 않는다. 운영 시크릿은 EC2의 `/opt/victoryfairy/.env`에서 Compose가 컨테이너 환경변수로 주입한다. Secrets Manager 전환은 운영 안정화 이후 별도 작업으로 진행한다.

## 데이터 이전과 전환

RDS와 S3는 이미 외부 서비스이므로 신규 서버가 같은 리소스에 연결한다. Redis는 현재 DB 1에 유효한 refresh token 13개와 `write_diary` Stream 23건이 있으므로 전환 직전에 최종 복사한다.

전환 순서는 다음과 같다.

1. 신규 서버에서 임시 IP로 컨테이너와 외부 의존성을 검증한다.
2. 점검 시간을 시작하고 기존 API 쓰기 요청을 중단한다.
3. 기존 Redis를 최종 저장하고 신규 Redis 볼륨에 복원한다.
4. 기존 Let's Encrypt 인증서를 신규 서버에 복사한다.
5. 신규 서버의 전체 API를 내부에서 검증한다.
6. 기존 Elastic IP를 신규 EC2에 연결한다.
7. 도메인, OAuth 로그인, 일기, 이미지, 크롤러를 확인한다.

기존 EC2는 전환 후 최소 2~3일 유지한다. 장애가 발생하면 Elastic IP를 기존 EC2로 다시 연결해 롤백한다.

## 실패 처리와 검증

GitHub Actions는 테스트나 이미지 빌드가 실패하면 ECR push와 배포를 실행하지 않는다. EC2 배포 스크립트는 Compose 설정 검증, 이미지 pull, 컨테이너 교체, 상태 출력 순으로 실행하고 실패 시 비정상 종료한다.

최소 검증 항목은 다음과 같다.

- `docker compose config` 성공
- 모든 이미지가 `linux/arm64`로 생성됨
- API, 파일, 관리자 컨테이너가 재시작 후 정상 기동됨
- RDS 연결과 기존 데이터 조회가 정상임
- S3 이미지 조회와 업로드가 정상임
- Kakao와 Google 로그인 및 refresh token 갱신이 정상임
- 크롤러 일회성 실행과 중복 실행 방지가 정상임
- Redis DB 1의 refresh token 개수가 이전 전후 동일함
- Nginx HTTPS와 4xx/5xx 로그가 정상 범위임

## 제외 범위

이번 작업에서 애플리케이션 리팩터링, `core-event` 복구, `core-batch` 복구, Redis 외부 서비스 전환, 모니터링 스택 도입, 무중단 다중 인스턴스 배포는 진행하지 않는다.
