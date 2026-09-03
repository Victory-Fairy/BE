package kr.co.victoryfairy.community.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;

import kr.co.victoryfairy.community.domain.CommunityReport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
class JdbcCommunityReportRepositoryIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    private JdbcCommunityReportRepository repository;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .load()
            .migrate();
    }

    @BeforeEach
    void setUp() throws Exception {
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM community_post_report");
            statement.executeUpdate("DELETE FROM community_post");
            statement.executeUpdate("DELETE FROM member");
            statement.executeUpdate("INSERT INTO member (id, created_at) VALUES (7, NOW()), (8, NOW())");
            statement.executeUpdate("""
                INSERT INTO community_post (id, member_id, title, content)
                VALUES (99, 8, '제목', '내용')
                """);
        }
        var dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        repository = new JdbcCommunityReportRepository(
                new NamedParameterJdbcTemplate(dataSource), new ObjectMapper());
    }

    @Test
    void createsListsResolvesAndSoftDeletesPost() throws Exception {
        var report = new CommunityReport(null, CommunityReport.TargetType.POST, 99L, 7L, 8L,
                CommunityReport.Reason.SPAM, CommunityReport.Status.PENDING, "반복 게시",
                new CommunityReport.Snapshot(99L, "제목", "내용"), null);

        var reportId = repository.save(report).orElseThrow();

        assertThat(repository.save(report)).isEmpty();
        assertThat(repository.find(CommunityReport.TargetType.POST, CommunityReport.Status.PENDING, null, 20))
            .singleElement()
            .satisfies(saved -> assertThat(saved.snapshot().content()).isEqualTo("내용"));
        assertThat(repository.resolve(
                CommunityReport.TargetType.POST, reportId, CommunityReport.Status.ACCEPTED)).isTrue();
        repository.softDeleteTarget(CommunityReport.TargetType.POST, 99L);
        assertThat(deletedPostCount()).isEqualTo(1);
    }

    private int deletedPostCount() throws Exception {
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM community_post WHERE id = 99 AND deleted_at IS NOT NULL");
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

}
