# 운영 DB 보존형 Flyway 전환 설계

## 목표

- 운영 중인 MySQL 데이터와 스키마를 그대로 보존하면서 Flyway 이력 관리를 시작한다.
- 이후의 모든 스키마 변경을 버전 SQL로 재현 가능하게 만든다.
- Flyway 도입과 커뮤니티 기능 스키마 추가를 서로 다른 배포로 분리한다.

## 변경하지 않는 것

- 첫 Flyway 배포에서는 운영 테이블, 컬럼, 인덱스, 제약조건과 데이터를 변경하지 않는다.
- API 계약과 애플리케이션 기능을 변경하지 않는다.
- crawler가 마이그레이션을 실행하게 만들지 않는다.
- Liquibase, 별도 마이그레이션 서버, 커스텀 실행기를 추가하지 않는다.

## 현재 상태와 위험

- 애플리케이션은 Flyway 없이 실행되고, 현재 저장소 설정의 Hibernate DDL은 `none`이다.
- 과거에는 환경에 따라 Hibernate `update`와 수동 SQL을 함께 사용했다. 따라서 엔티티만으로는 실제 운영 스키마를 정확히 복원할 수 없다.
- `deploy/migrations/20260831_game_record_diary_unique.sql`은 배포 파이프라인에서 자동 실행되지 않는다. 운영 반영 여부를 DB에서 확인해야 한다.
- 현재 엔티티에 없는 `free_diary`처럼 운영 DB에만 남아 있을 수 있는 테이블도 기준선에서 임의로 제거하면 안 된다.
- MySQL DDL은 실패 시 전체가 원자적으로 롤백되지 않을 수 있으므로 운영 반영 전 복원 리허설이 필수다.

## 결정

### 실행 주체

Flyway 의존성과 마이그레이션 파일은 `applications/api`에만 둔다. 현재 운영은 API 인스턴스가 하나이므로 API 시작 시 Spring Boot가 Flyway를 실행하는 가장 단순한 구조를 사용한다. crawler는 같은 DB를 사용하더라도 스키마 소유자가 아니다.

추후 API가 여러 인스턴스로 확장되거나 무중단 배포가 필요해지면 배포 파이프라인의 단일 migration job으로 분리한다. 이번 전환에는 넣지 않는다.

### 기준선

`V1__baseline.sql`은 엔티티 생성 결과나 오래된 dump가 아니라 전환 직전 운영 DB의 최신 schema-only dump를 기반으로 만든다. 다음 항목을 포함한다.

- 현재 운영의 모든 애플리케이션 테이블
- 컬럼 타입, 기본값, null 조건
- PK, FK, unique constraint, index
- 운영에 존재하지만 현재 코드가 참조하지 않는 테이블

다음 항목은 제외한다.

- `INSERT`와 실제 운영 데이터
- `DROP TABLE`, `DROP DATABASE`, `CREATE DATABASE`, `USE`
- 사용자, 권한, 비밀번호
- 환경에 종속된 DEFINER 또는 서버 설정

V1은 빈 DB를 현재 운영 스키마와 동일하게 만드는 용도다. 이미 같은 스키마가 존재하는 운영 DB에서는 V1 SQL을 실행하지 않고 Flyway의 명시적 `baseline` 명령으로 버전 1의 이력만 기록한다.

### 설정과 파일 위치

- `applications/api/build.gradle.kts`
  - `org.flywaydb:flyway-core` 추가
  - MySQL 지원 모듈 `org.flywaydb:flyway-mysql`을 runtime 의존성으로 추가
- `applications/api/src/main/resources/db/migration/V1__baseline.sql`
  - 운영 최신 스키마의 재현 가능한 기준선
- API와 crawler의 JPA 설정
  - `spring.jpa.hibernate.ddl-auto=validate`
- 운영 비밀 설정
  - Flyway는 기존 단일 primary `DataSource`를 사용하며 별도 DB 계정을 이번 범위에 추가하지 않는다.

`spring.flyway.baseline-on-migrate=true`는 사용하지 않는다. 잘못된 DB를 자동으로 기준선 처리해 검증을 우회할 수 있기 때문이다. 운영 baseline은 애플리케이션과 같은 Flyway 버전의 표준 CLI로, 작업자가 대상 DB와 현재 스키마를 확인한 뒤 한 번만 명시적으로 수행한다. 저장소에 일회용 baseline 애플리케이션 코드는 추가하지 않는다.

## 전환 순서

### 1. 운영 스키마 확정

1. 배포 직전 운영 DB snapshot을 생성하고 복구 가능 여부를 확인한다.
2. 읽기 전용으로 테이블, 컬럼, PK/FK, unique constraint, index 목록을 추출한다.
3. `game_record_diary` unique constraint의 실제 존재 여부를 확인한다.
4. 최신 schema-only dump를 정리해 `V1__baseline.sql`을 만든다.

### 2. 비운영 검증

1. 빈 MySQL DB에 V1을 적용한다.
2. API를 시작해 Flyway 성공과 Hibernate `validate` 성공을 확인한다.
3. 운영 snapshot을 별도 DB에 복원한다.
4. 복원 DB에 명시적 baseline version 1을 기록한 뒤 API를 시작한다.
5. `flyway_schema_history`에 baseline 1이 한 번만 기록되고 V1 SQL이 기존 테이블에 재실행되지 않았는지 확인한다.
6. 로그인, 일기 생성·조회·수정, 경기 기록 조회 등 DB 핵심 API를 스모크 테스트한다.

### 3. 첫 운영 배포

1. 변경 배포를 잠시 중지하고 최종 DB snapshot을 만든다.
2. 대상 DB의 이름과 스키마가 리허설 대상과 일치하는지 다시 확인한다.
3. 애플리케이션과 같은 버전의 Flyway CLI로 운영 DB에 `baseline` version 1을 명시적으로 한 번 실행한다.
4. Flyway가 포함된 API를 배포한다.
5. 시작 로그, `flyway_schema_history`, health check와 DB 핵심 API를 확인한다.
6. crawler를 시작하거나 유지하고 Hibernate `validate` 성공을 확인한다.

이 배포에는 V2 이상의 SQL이나 커뮤니티 기능을 포함하지 않는다.

### 4. 후속 마이그레이션

- `game_record_diary` unique constraint가 운영에 없다면 V2에서 추가한다. 이미 있다면 V2로 중복 생성하지 않는다.
- 커뮤니티의 글·댓글·좋아요·비공개 신고·검색용 인덱스는 그다음 사용 가능한 버전부터 별도 배포한다.
- 이미 적용된 migration 파일은 수정하지 않는다. 변경은 항상 새 버전의 forward migration으로 추가한다.
- 삭제·rename·NOT NULL 강화 같은 파괴적 변경은 expand-contract 방식으로 나누고, 이전 애플리케이션 버전과 함께 동작하는 단계를 먼저 배포한다.

## 실패와 롤백

- baseline 전에 검증이 실패하면 DB를 변경하지 않고 중단한다.
- API 시작 시 새 migration이 실패하면 API를 정상 상태로 올리지 않는다. 실패 원인을 수정한 새 migration을 만들고 snapshot으로 복구 가능한 상태를 유지한다.
- 첫 도입 배포는 스키마를 변경하지 않으므로 애플리케이션 장애 시 직전 API 이미지로 롤백할 수 있다. `flyway_schema_history`의 baseline 행은 유지해도 기존 애플리케이션 동작에 영향을 주지 않는다.
- 적용된 migration을 되돌리기 위해 파일을 수정하거나 history 행을 수동 삭제하지 않는다. 데이터 손실 위험이 있는 경우 snapshot 복원 여부를 먼저 판단한다.

## 검증 기준

- 빈 DB에서 V1 적용 후 API가 시작되고 Hibernate schema validation이 통과한다.
- V1으로 만든 스키마와 운영 schema-only 결과의 구조 차이가 없다.
- 운영 snapshot 복원 DB에서 명시적 baseline 후 API와 crawler가 정상 시작한다.
- 운영 DB에는 baseline 1만 먼저 기록되고 기존 테이블 생성 SQL이 실행되지 않는다.
- 첫 배포 전후의 테이블 수, 주요 행 수와 핵심 인덱스·제약조건이 동일하다.
- 기존 API의 DB 핵심 스모크 테스트가 통과한다.

## 완료 조건

- Flyway가 운영 DB의 유일한 스키마 변경 이력 도구가 된다.
- Hibernate는 모든 환경에서 `validate`만 수행하고 스키마를 변경하지 않는다.
- 운영 데이터와 스키마가 첫 전환 배포 전후로 동일하다.
- 이후 개발자는 검토된 새 versioned SQL 없이 운영 스키마를 수동 변경하지 않는다.
