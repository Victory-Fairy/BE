# Flyway Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare the API to manage the existing MySQL schema with Flyway without changing or connecting to the running production database, EC2 instance, or deployed application.

**Architecture:** Only the API owns Flyway and runs versioned SQL at startup; the crawler remains a schema consumer. `V1__baseline.sql` reproduces the latest production schema on an empty database, while an existing production database will later receive an explicit version-1 baseline in a separately approved operational cutover.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Gradle Kotlin DSL, Flyway, MySQL 8.0, JUnit 5, Testcontainers 2.0.5, Docker

**Spec:** `docs/superpowers/specs/2026-09-02-flyway-baseline-design.md`

## Global Constraints

- Do not connect to, query, baseline, migrate, restart, or deploy to production during this implementation.
- Do not change production data or schema in this implementation.
- The first Flyway release contains only `V1__baseline.sql`; no V2 migration or community table is included.
- `spring.flyway.baseline-on-migrate` must remain absent or `false`.
- Flyway dependencies and migration resources belong only to `applications/api`.
- The crawler must not include Flyway and must use Hibernate `validate` only.
- The baseline source must be a current, reviewed production schema-only dump; the ignored 2026-08-18 handover dump and JPA entities are not authoritative.
- Preserve the user's unrelated `.gitignore`, `compose.local.yaml`, and `deploy/scripts/verify-local-compose.sh` changes.

## File Map

- Modify `applications/api/build.gradle.kts`: add Flyway runtime support and migration-test dependencies.
- Create `applications/api/src/main/resources/db/migration/V1__baseline.sql`: reproduce the reviewed production schema on an empty MySQL database.
- Create `applications/api/src/test/java/kr/co/victoryfairy/configuration/FlywayBaselineMigrationTest.java`: reject data/destructive statements and apply V1 to disposable MySQL.
- Modify `applications/api/src/main/resources/application-local.yml`: change local API Hibernate mode from `none` to `validate`.
- Modify `applications/crawler/src/main/resources/application-local.properties`: change local crawler Hibernate mode from `none` to `validate`.
- Modify `deploy/compose.yaml`: force Hibernate `validate` for deployed API and crawler through an environment variable.
- Modify `deploy/scripts/verify-compose.sh`: verify the production Compose definition enforces Hibernate `validate`.

---

### Task 1: Add and prove the production-schema baseline

**Files:**
- Modify: `applications/api/build.gradle.kts`
- Create: `applications/api/src/test/java/kr/co/victoryfairy/configuration/FlywayBaselineMigrationTest.java`
- Create: `applications/api/src/main/resources/db/migration/V1__baseline.sql`

**Interfaces:**
- Consumes: a current production schema-only dump supplied through the approved operations path, the live-read verification of whether `game_record_diary` already has its unique constraint, and a local Docker daemon.
- Produces: Flyway version 1 plus a standard Gradle test that proves Flyway can apply it to disposable MySQL 8.0 and records exactly one successful version-1 migration.

- [ ] **Step 1: Write the failing migration test**

Create `FlywayBaselineMigrationTest.java`:

```java
package kr.co.victoryfairy.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Locale;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class FlywayBaselineMigrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    @Test
    void appliesSchemaOnlyBaselineToEmptyMySql() throws Exception {
        String baseline = readBaseline().toUpperCase(Locale.ROOT);
        assertThat(baseline).contains("CREATE TABLE");
        assertThat(baseline).doesNotContain(
            "INSERT INTO", "DELETE FROM", "TRUNCATE TABLE", "DROP TABLE",
            "DROP DATABASE", "CREATE DATABASE", "\nUSE ");

        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .load()
            .migrate();

        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1");
             var result = statement.executeQuery()) {
            result.next();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }

    private String readBaseline() throws IOException {
        try (var input = getClass().getClassLoader()
            .getResourceAsStream("db/migration/V1__baseline.sql")) {
            assertThat(input).as("V1 baseline resource").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify the Flyway contract is missing**

Run:

```bash
./gradlew :applications:api:test --tests kr.co.victoryfairy.configuration.FlywayBaselineMigrationTest
```

Expected: compilation fails because Flyway and Testcontainers are not dependencies yet.

- [ ] **Step 3: Add only the required dependencies**

Add inside `dependencies` in `applications/api/build.gradle.kts`:

```kotlin
implementation("org.flywaydb:flyway-core")
runtimeOnly("org.flywaydb:flyway-mysql")

testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
testImplementation("org.testcontainers:testcontainers-mysql")
testImplementation("org.testcontainers:testcontainers-junit-jupiter")
```

Spring Boot manages the Flyway version. The explicit Testcontainers BOM keeps its two modules on the same version.

- [ ] **Step 4: Run the test to verify the baseline resource is missing**

Run the same Gradle command.

Expected: the test starts disposable MySQL and fails with `V1 baseline resource` because `V1__baseline.sql` does not exist.

- [ ] **Step 5: Stop if the authoritative input is unavailable**

Verify that the supplied dump was created after the most recent production schema change and contains schema only. Do not substitute `victoryfairy-handover/db/victoryFairy_prod_20260818.sql.gz`, Hibernate-generated DDL, or assumptions from entities. If the current dump is unavailable, this task remains blocked and no V1 is fabricated.

- [ ] **Step 6: Normalize the schema dump into V1**

Create `applications/api/src/main/resources/db/migration/V1__baseline.sql` from the reviewed dump. Preserve every current table, including unused tables such as `free_diary`, and preserve column definitions, PK/FK, unique constraints, and indexes. Remove only these statement classes:

```text
INSERT
DELETE
TRUNCATE TABLE
DROP TABLE
DROP DATABASE
CREATE DATABASE
USE
CREATE USER
GRANT
SET PASSWORD
DEFINER
```

Do not add `IF NOT EXISTS`; V1 must fail on accidental execution against an existing schema rather than hide the wrong target.

- [ ] **Step 7: Check the migration text before execution**

Run:

```bash
rg -n -i 'insert[[:space:]]+into|delete[[:space:]]+from|truncate[[:space:]]+table|drop[[:space:]]+table|drop[[:space:]]+database|create[[:space:]]+database|^[[:space:]]*use[[:space:]]|create[[:space:]]+user|^[[:space:]]*grant[[:space:]]|set[[:space:]]+password|definer' applications/api/src/main/resources/db/migration/V1__baseline.sql
```

Expected: no matches.

- [ ] **Step 8: Apply V1 to disposable MySQL**

Run:

```bash
./gradlew :applications:api:test --tests kr.co.victoryfairy.configuration.FlywayBaselineMigrationTest
```

Expected: PASS; Docker contains all database effects, and no running local or production database is used.

- [ ] **Step 9: Commit the working baseline slice**

```bash
git add applications/api/build.gradle.kts applications/api/src/test/java/kr/co/victoryfairy/configuration/FlywayBaselineMigrationTest.java applications/api/src/main/resources/db/migration/V1__baseline.sql
git commit -m "db: add Flyway production schema baseline"
```

### Task 2: Make Hibernate validation-only in every shipped environment

**Files:**
- Modify: `applications/api/src/main/resources/application-local.yml`
- Modify: `applications/crawler/src/main/resources/application-local.properties`
- Modify: `deploy/compose.yaml`
- Modify: `deploy/scripts/verify-compose.sh`

**Interfaces:**
- Consumes: the V1 schema created by Flyway for new databases or a later explicitly baselined existing database.
- Produces: API and crawler startup that validates mappings but cannot create, update, or drop schema through Hibernate.

- [ ] **Step 1: Add a failing deployment guard**

Add to `deploy/scripts/verify-compose.sh`:

```bash
grep -q 'SPRING_JPA_HIBERNATE_DDL_AUTO: validate' "$repo_root/deploy/compose.yaml"
test "$(grep -c 'SPRING_JPA_HIBERNATE_DDL_AUTO: validate' "$repo_root/deploy/compose.yaml")" -eq 1
```

Run:

```bash
bash deploy/scripts/verify-compose.sh
```

Expected: failure because the shared application environment does not yet define the property.

- [ ] **Step 2: Force validation in deployed API and crawler**

Add one property to the shared `x-app-environment` mapping in `deploy/compose.yaml`:

```yaml
  SPRING_JPA_HIBERNATE_DDL_AUTO: validate
```

Because both `api` and `craw` inherit this mapping, the environment variable takes precedence over externally mounted profile files and prevents an old `update` value from mutating production schema.

- [ ] **Step 3: Change both local profiles to validation**

Change API YAML to:

```yaml
  jpa:
    hibernate:
      ddl-auto: validate
```

Change crawler properties to:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

- [ ] **Step 4: Verify shipped configuration**

Run:

```bash
bash deploy/scripts/verify-compose.sh
rg -n 'ddl-auto: validate|ddl-auto=validate|SPRING_JPA_HIBERNATE_DDL_AUTO: validate' applications/api/src/main/resources/application-local.yml applications/crawler/src/main/resources/application-local.properties deploy/compose.yaml
rg -n 'baseline-on-migrate|ddl-auto: (create|create-drop|update)|ddl-auto=(create|create-drop|update)' applications deploy domain
```

Expected: the first command passes; the second command prints exactly the three validation settings; the final command has no matches.

- [ ] **Step 5: Commit validation-only configuration**

```bash
git add applications/api/src/main/resources/application-local.yml applications/crawler/src/main/resources/application-local.properties deploy/compose.yaml deploy/scripts/verify-compose.sh
git commit -m "build: enforce validation-only schema handling"
```

### Task 3: Verify the non-production release candidate

**Files:**
- Verify only; no production files or systems are mutated.

**Interfaces:**
- Consumes: Tasks 1-2.
- Produces: a build artifact that is safe to rehearse against a restored production snapshot but is not deployed or run against production.

- [ ] **Step 1: Run focused safety checks**

```bash
./gradlew :applications:api:test --tests kr.co.victoryfairy.configuration.FlywayBaselineMigrationTest
bash deploy/scripts/verify-compose.sh
```

Expected: both pass.

- [ ] **Step 2: Run the repository verification**

```bash
./gradlew test :applications:api:bootJar :applications:crawler:bootJar -Pci --parallel --no-build-cache
```

Expected: all tests and both production JAR builds pass.

- [ ] **Step 3: Inspect the packaged API migration**

```bash
unzip -l applications/api/build/libs/*.jar | rg 'BOOT-INF/classes/db/migration/V1__baseline.sql'
```

Expected: exactly one V1 resource is packaged in the API JAR. The crawler JAR is not expected to contain it.

- [ ] **Step 4: Confirm the production boundary**

```bash
git diff HEAD~3..HEAD -- applications/api applications/crawler deploy/compose.yaml deploy/scripts/verify-compose.sh
git status --short
```

Expected: only the planned code/configuration files changed; no AWS, RDS, EC2, SSM, deployment, Flyway CLI, or production database command was executed.

- [ ] **Step 5: Hand off the separately approved operational gate**

Stop after local verification. Before any deployment, operations must separately approve and complete all of the following from the design spec: final RDS snapshot, restore rehearsal, schema comparison, explicit Flyway CLI `baseline -baselineVersion=1`, API deployment, history/health/API smoke checks. A code-ready result is not authorization to perform those production actions.
