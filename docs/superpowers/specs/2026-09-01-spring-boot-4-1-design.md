# Spring Boot 4.1 마이그레이션 설계

## 목표

- 운영 API 계약을 유지하면서 Spring Boot 3.5.16을 4.1.1로 올린다.
- Java 21과 Gradle 8.14.5는 유지한다.
- Boot 4의 모듈형 스타터와 Jackson 3를 바로 적용해 임시 호환 계층을 남기지 않는다.

## 변경하지 않는 것

- API 경로, HTTP 상태 코드, 요청·응답 JSON 구조
- 기존 JWT 발급·검증 방식과 JJWT 0.11.2
- DB 스키마와 운영 데이터
- 도메인 구조, Apple 계정 이전, 알림·모니터링 기능
- 배포 인프라와 브랜치 전략

## 의존성 변경

| 대상 | 현재 | 변경 |
|---|---|---|
| Spring Boot | 3.5.16 | 4.1.1 |
| Java | 21 | 유지 |
| Gradle | 8.14.5 | 유지 |
| MVC 스타터 | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| springdoc | 2.8.17 | 3.1.0 |
| P6Spy 스타터 | 1.12.1 | 2.0.1 |
| Jackson | 2 (`com.fasterxml.jackson.databind`) | 3 (`tools.jackson.databind`) |

Jackson annotation 패키지는 Jackson 3에서도 `com.fasterxml.jackson.annotation`이므로 변경하지 않는다. QueryDSL 5.0.0과 그 밖의 라이브러리는 컴파일 또는 실행 검증에서 실제 호환 문제가 확인된 경우에만 최소 변경한다.

## 구현 순서

1. Boot·springdoc·P6Spy 버전과 MVC 스타터를 변경하고 컴파일 오류를 수집한다.
2. ObjectMapper 및 Jackson 모듈 사용부를 Jackson 3 API로 전환한다. OAuth, JWT, Redis 직렬화 결과가 기존 계약과 같은지 회귀 테스트로 고정한다.
3. Spring Framework 7, Spring Data 4, Hibernate 7에서 발생한 실제 컴파일·테스트 오류만 수정한다. QueryDSL은 선제 업그레이드하지 않는다.
4. 삭제·변경된 설정 속성을 점검한다. 속성 마이그레이터가 필요하면 로컬 진단에만 사용하고 최종 산출물에서는 제거한다.
5. 전체 테스트, API PIT, assemble, ARM64 Docker 빌드와 로컬 실행을 검증한다.
6. `develop`에서 전체 빌드·테스트·이미지 검증을 마친 뒤 `main`에 승격한다. 별도 develop 서버가 없으므로 운영 배포 직후 Swagger, 로그인, DB 조회 API를 스모크 테스트한다.

## 호환성 기준

- 기존 클라이언트가 수정 없이 같은 API를 호출할 수 있어야 한다.
- 기존 access/refresh token이 만료 전까지 계속 검증되어야 한다.
- Kakao·Google OAuth 시작 경로와 콜백 처리가 동일해야 한다.
- 날짜·시간, enum, null, 컬렉션을 포함한 JSON 직렬화 결과가 바뀌지 않아야 한다.
- JPA 조회 결과와 트랜잭션 경계가 기존과 같아야 한다.
- Swagger UI와 OpenAPI 문서 엔드포인트가 정상 응답해야 한다.

## 검증

- 기준선: `clean test assemble :applications:api:pitest -Pci --parallel --no-build-cache` 통과, 18/18 변이 제거
- 전체 단위·컨텍스트 테스트와 API 계약 테스트
- JWT 발급·검증 회귀 테스트 및 기존 토큰 샘플 검증
- ObjectMapper 기반 OAuth/JWT/Redis 직렬화 회귀 테스트
- JPA·QueryDSL 저장소 테스트
- API·crawler ARM64 이미지 빌드 및 Compose 기동 확인
- 배포 후 health, Swagger, Kakao/Google auth-path, 경기·팀 조회 스모크 테스트

## 배포와 롤백

- `develop`의 전체 검증 전에는 `main`과 운영 배포를 변경하지 않는다.
- 배포 이미지는 커밋 SHA 태그로 식별하고, 장애 시 직전 정상 이미지로 되돌린다.
- 현재 배포 파이프라인의 자동 health-check 롤백 부재는 알려진 위험으로 남긴다. 이번 버전업에 배포 구조 개선을 섞지 않고, 배포 직후 스모크 테스트로 차단한다.

## 완료 조건

- 빌드·테스트·PIT·ARM64 이미지 검증이 모두 통과한다.
- API 및 JWT 계약 변경이 없다.
- `develop`의 전체 검증과 운영 배포 직후 스모크 테스트가 통과한다.
- 임시 Jackson 2 호환 모듈과 속성 마이그레이터가 최종 의존성에 남지 않는다.
