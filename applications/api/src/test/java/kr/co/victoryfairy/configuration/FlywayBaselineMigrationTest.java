package kr.co.victoryfairy.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Locale;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayBaselineMigrationTest {

    @Test
    void containsSchemaOnlyStatements() throws Exception {
        String baseline = readBaseline().toUpperCase(Locale.ROOT);
        assertThat(baseline).contains("CREATE TABLE");
        assertThat(baseline).doesNotContain(
            "INSERT INTO", "DELETE FROM", "TRUNCATE TABLE", "DROP TABLE",
            "DROP DATABASE", "CREATE DATABASE", "\nUSE ");
    }

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    class MySqlMigration {

        @Container
        static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

        @Test
        void appliesBaselineToEmptyMySql() throws Exception {
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
    }

    private String readBaseline() throws IOException {
        try (var input = getClass().getClassLoader()
            .getResourceAsStream("db/migration/V1__baseline.sql")) {
            assertThat(input).as("V1 baseline resource").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
