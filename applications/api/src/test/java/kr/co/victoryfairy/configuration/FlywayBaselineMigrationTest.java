package kr.co.victoryfairy.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Locale;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayBaselineMigrationTest {

    @Test
    void containsSchemaOnlyStatements() throws Exception {
        String baseline = readBaseline().toUpperCase(Locale.ROOT);
        assertThat(baseline).contains("CREATE TABLE");
        assertThat(baseline).doesNotContain(
            "INSERT INTO", "DELETE FROM", "TRUNCATE TABLE", "DROP TABLE",
            "DROP DATABASE", "CREATE DATABASE", "\nUSE ");
    }

    @Test
    void containsGameQueryIndexMigration() throws Exception {
        String migration = readMigration("V3__optimize_game_query_indexes.sql").toUpperCase(Locale.ROOT);

        assertThat(migration).contains(
            "IDX_GAME_MATCH_LEAGUE_MATCH_AT",
            "IDX_DIARY_MEMBER_GAME_MATCH",
            "IDX_PITCHER_RECORD_GAME_MATCH",
            "IDX_HITTER_RECORD_GAME_MATCH"
        );
    }

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    class MySqlMigration {

        @Container
        static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

        @Test
        void appliesAllMigrationsToEmptyMySql() throws Exception {
            Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .load()
                .migrate();

            assertThat(successfulMigrationCount(MYSQL, "1")).isEqualTo(1);
            assertThat(successfulMigrationCount(MYSQL, "2")).isEqualTo(1);
            assertThat(applicationTableCount(MYSQL)).isEqualTo(29);
            assertThat(commonCodeDetailCount(MYSQL)).isEqualTo(7);
            assertThat(indexCount(MYSQL, "file_ref", "idx_file_ref_ref_type_ref_id_is_use")).isEqualTo(1);
            assertThat(indexCount(MYSQL, "community_post_report", "idx_community_post_report_status_id"))
                .isEqualTo(1);
            assertThat(indexCount(MYSQL, "community_comment_report", "idx_community_comment_report_status_id"))
                .isEqualTo(1);
            assertGameQueryIndexes(MYSQL);
            assertThat(columnCount(MYSQL, "community_post", "updated_at")).isEqualTo(1);
            assertThat(columnCount(MYSQL, "community_comment", "updated_at")).isEqualTo(1);
        }

    }

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    class ExistingSchemaBaseline {

        @Container
        static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("existing_schema")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("db/migration/V1__baseline.sql"),
                "/tmp/V1__baseline.sql"
            );

        @Test
        void baselinesExistingSchemaWithoutReapplyingV1() throws Exception {
            var importResult = MYSQL.execInContainer(
                "sh", "-c",
                "mysql -u" + MYSQL.getUsername() + " -p" + MYSQL.getPassword()
                    + " " + MYSQL.getDatabaseName() + " < /tmp/V1__baseline.sql"
            );
            assertThat(importResult.getExitCode()).isZero();

            assertThat(applicationTableCount(MYSQL)).isEqualTo(20);
            insertSentinel(MYSQL);
            assertThat(sentinelCount(MYSQL)).isEqualTo(1);

            Flyway unbaselinedFlyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .load();

            assertThatThrownBy(unbaselinedFlyway::migrate)
                .isInstanceOf(FlywayException.class);
            assertThat(historyTableExists(MYSQL)).isFalse();

            Flyway flyway = Flyway.configure()
                .baselineVersion("1")
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .load();

            flyway.baseline();

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
            assertThat(applicationTableCount(MYSQL)).isEqualTo(29);
            assertThat(sentinelCount(MYSQL)).isEqualTo(1);
            assertThat(historyEntryCount(MYSQL)).isEqualTo(1);
            assertThat(successfulMigrationCount(MYSQL, "2")).isEqualTo(1);
            assertThat(commonCodeDetailCount(MYSQL)).isEqualTo(7);
            assertThat(indexCount(MYSQL, "file_ref", "idx_file_ref_ref_type_ref_id_is_use")).isEqualTo(1);
            assertThat(indexCount(MYSQL, "community_post_report", "idx_community_post_report_status_id"))
                .isEqualTo(1);
            assertThat(indexCount(MYSQL, "community_comment_report", "idx_community_comment_report_status_id"))
                .isEqualTo(1);
            assertGameQueryIndexes(MYSQL);
            assertThat(columnCount(MYSQL, "community_post", "updated_at")).isEqualTo(1);
            assertThat(columnCount(MYSQL, "community_comment", "updated_at")).isEqualTo(1);
        }
    }

    private int applicationTableCount(MySQLContainer<?> mysql) throws Exception {
        return count(mysql, """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name <> 'flyway_schema_history'
            """);
    }

    private int historyEntryCount(MySQLContainer<?> mysql) throws Exception {
        return count(mysql, """
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE version = '1' AND type = 'BASELINE' AND success = 1
            """);
    }

    private int successfulMigrationCount(MySQLContainer<?> mysql, String version) throws Exception {
        try (var connection = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             var statement = connection.prepareStatement("""
                 SELECT COUNT(*)
                 FROM flyway_schema_history
                 WHERE version = ? AND type = 'SQL' AND success = 1
                 """)) {
            statement.setString(1, version);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private int commonCodeDetailCount(MySQLContainer<?> mysql) throws Exception {
        return count(mysql, "SELECT COUNT(*) FROM common_code_detail");
    }

    private void assertGameQueryIndexes(MySQLContainer<?> mysql) throws Exception {
        assertThat(successfulMigrationCount(mysql, "3")).isEqualTo(1);
        assertThat(indexCount(mysql, "game_match", "idx_game_match_league_match_at")).isEqualTo(1);
        assertThat(indexCount(mysql, "diary", "idx_diary_member_game_match")).isEqualTo(1);
        assertThat(indexCount(mysql, "pitcher_record", "idx_pitcher_record_game_match")).isEqualTo(1);
        assertThat(indexCount(mysql, "hitter_record", "idx_hitter_record_game_match")).isEqualTo(1);
    }

    private int indexCount(MySQLContainer<?> mysql, String tableName, String indexName) throws Exception {
        try (var connection = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             var statement = connection.prepareStatement("""
                 SELECT COUNT(DISTINCT index_name)
                 FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = ?
                   AND index_name = ?
                 """)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private int columnCount(MySQLContainer<?> mysql, String tableName, String columnName) throws Exception {
        try (var connection = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             var statement = connection.prepareStatement("""
                 SELECT COUNT(*)
                 FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = ?
                   AND column_name = ?
                 """)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private boolean historyTableExists(MySQLContainer<?> mysql) throws Exception {
        return count(mysql, """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'flyway_schema_history'
            """) == 1;
    }

    private void insertSentinel(MySQLContainer<?> mysql) throws Exception {
        try (var connection = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             var statement = connection.prepareStatement("""
                 INSERT INTO withdrawal_reason (created_at, is_use, updated_at, reason)
                 VALUES (CURRENT_TIMESTAMP(6), b'1', CURRENT_TIMESTAMP(6), 'flyway-baseline-sentinel')
                 """)) {
            statement.executeUpdate();
        }
    }

    private int sentinelCount(MySQLContainer<?> mysql) throws Exception {
        return count(mysql, """
            SELECT COUNT(*)
            FROM withdrawal_reason
            WHERE reason = 'flyway-baseline-sentinel'
            """);
    }

    private int count(MySQLContainer<?> mysql, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private String readBaseline() throws IOException {
        try (var input = getClass().getClassLoader()
            .getResourceAsStream("db/migration/V1__baseline.sql")) {
            assertThat(input).as("V1 baseline resource").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readMigration(String filename) throws IOException {
        try (var input = getClass().getClassLoader()
            .getResourceAsStream("db/migration/" + filename)) {
            assertThat(input).as("%s resource", filename).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
