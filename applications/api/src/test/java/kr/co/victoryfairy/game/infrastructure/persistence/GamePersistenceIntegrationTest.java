package kr.co.victoryfairy.game.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.StadiumEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "spring.profiles.active=test", "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.properties.hibernate.generate_statistics=true", "spring.data.redis.repositories.enabled=false",
            "jwt.secret-key=test-only-secret-key-test-only-secret-key-test-only-secret-key-1234",
            "jwt.access-token-expire-minutes=30", "jwt.refresh-token-expire-days=7",
            "auth.kakao.cas.client_id=test", "auth.kakao.cas.client_secret=test",
            "auth.kakao.cas.callback_url=http://localhost/test", "auth.google.cas.client_id=test",
            "auth.google.cas.client_secret=test", "auth.google.cas.callback_url=http://localhost/test",
            "auth.apple.cas.client_id=test", "auth.apple.cas.client_secret=test", "auth.apple.cas.team_id=test",
            "auth.apple.cas.key_id=test", "auth.apple.cas.callback_url=http://localhost/test",
            "auth.apple.cas.secret_path=test", "webhook.slack.url=http://localhost/disabled",
            "victory-fairy.file.storage-path=/tmp/victoryfairy-test", "victory-fairy.file.s3-enabled=false" })
class GamePersistenceIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("storage.datasource.core.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("storage.datasource.core.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("storage.datasource.core.username", MYSQL::getUsername);
        registry.add("storage.datasource.core.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.database", () -> 1);
    }

    @Autowired GameMatchRepository matches;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired TransactionTemplate transactions;

    @Test
    void dateReadMapsIdsInOneQueryAndSaveKeepsIdentityAndAudit() {
        var ids = transactions.execute(status -> {
            var away = new TeamEntity(null, "KT", "KT");
            var home = new TeamEntity(null, "LG", "LG");
            var stadium = StadiumEntity.builder().shortName("잠실").build();
            entityManager.persist(away);
            entityManager.persist(home);
            entityManager.persist(stadium);
            entityManager.persist(GameMatchEntity.builder().id("20260905KTLG0").league(MatchEnum.LeagueType.KBO)
                .matchAt(LocalDateTime.of(2026, 9, 5, 18, 30)).awayTeamEntity(away).homeTeamEntity(home)
                .stadiumEntity(stadium).status(MatchEnum.MatchStatus.READY).build());
            entityManager.flush();
            return new Long[] { away.getId(), home.getId(), stadium.getId() };
        });

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var match = matches.findByDate(LocalDate.of(2026, 9, 5), MatchEnum.LeagueType.KBO).getFirst();

        assertThat(statistics.getPrepareStatementCount()).isOne();
        assertThat(statistics.getEntityFetchCount()).isZero();
        assertThat(match.awayTeamId()).isEqualTo(ids[0]);
        assertThat(match.homeTeamId()).isEqualTo(ids[1]);
        assertThat(match.stadiumId()).isEqualTo(ids[2]);
        assertThat(match.awayScore()).isNull();
        assertThat(match.createdAt()).isNotNull();

        var saved = matches.save(match.updateLive(MatchEnum.MatchStatus.END, null, (short) 1, (short) 0));
        assertThat(saved.id()).isEqualTo(match.id());
        assertThat(saved.createdAt()).isEqualTo(match.createdAt());
        assertThat(saved.updatedAt()).isNotNull();
    }
}
