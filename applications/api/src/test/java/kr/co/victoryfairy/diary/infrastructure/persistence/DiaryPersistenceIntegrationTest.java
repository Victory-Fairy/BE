package kr.co.victoryfairy.diary.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.StadiumEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.domain.MemberGameReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "spring.profiles.active=test", "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.properties.hibernate.generate_statistics=true",
            "spring.data.redis.repositories.enabled=false",
            "jwt.secret-key=test-only-secret-key-test-only-secret-key-test-only-secret-key-1234",
            "jwt.access-token-expire-minutes=30", "jwt.refresh-token-expire-days=7",
            "auth.kakao.cas.client_id=test", "auth.kakao.cas.client_secret=test",
            "auth.kakao.cas.callback_url=http://localhost/test", "auth.google.cas.client_id=test",
            "auth.google.cas.client_secret=test", "auth.google.cas.callback_url=http://localhost/test",
            "auth.apple.cas.client_id=test", "auth.apple.cas.client_secret=test", "auth.apple.cas.team_id=test",
            "auth.apple.cas.key_id=test", "auth.apple.cas.callback_url=http://localhost/test",
            "auth.apple.cas.secret_path=test", "webhook.slack.url=http://localhost/disabled",
            "victory-fairy.file.storage-path=/tmp/victoryfairy-test", "victory-fairy.file.s3-enabled=false" })
class DiaryPersistenceIntegrationTest {
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

    @Autowired DiaryStore diaries;
    @Autowired GameRecordStore records;
    @Autowired GameRecordDomainService gameRecords;
    @Autowired MemberStore members;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactions;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired MemberGameReader memberGames;

    @Test
    void victory_power_reads_multiple_records_in_one_query() {
        Long memberId = transactions.execute(status -> {
            var member = members.saveMember(Member.normal("127.0.0.2", LocalDateTime.of(2026, 9, 5, 1, 0)));
            var away = new TeamEntity(null, "두산", "두산");
            var home = new TeamEntity(null, "롯데", "롯데");
            entityManager.persist(away); entityManager.persist(home);
            for (int i = 1; i <= 3; i++) {
                var stadium = StadiumEntity.builder().fullName("구장" + i).build();
                entityManager.persist(stadium);
                String matchId = "2026090" + i + "DBLT0";
                entityManager.persist(GameMatchEntity.builder().id(matchId).season("2026")
                    .league(MatchEnum.LeagueType.KBO).matchAt(LocalDateTime.of(2026, 9, i, 18, 30))
                    .awayTeamEntity(away).homeTeamEntity(home).stadiumEntity(stadium).awayScore((short) 3)
                    .homeScore((short) 1).status(MatchEnum.MatchStatus.END).build());
                entityManager.flush();
                var diary = diaries.save(new Diary(null, member.id(), matchId, away.getId(), away.getName(),
                        DiaryEnum.ViewType.STADIUM, null, null, null, false, null, null));
                gameRecords.record(diary);
            }
            entityManager.flush();
            entityManager.clear();
            return member.id();
        });
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var powerRecords = transactions.execute(status -> memberGames.findByMemberAndSeason(memberId, "2026"));

        assertThat(powerRecords).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isOne();
    }

    @Test
    void generated_id_roundtrips_nullable_and_audit_fields_and_record_recovery_is_idempotent() {
        var saved = transactions.execute(status -> {
            var member = members.saveMember(Member.normal("127.0.0.1", LocalDateTime.of(2026, 9, 5, 1, 0)));
            var away = new TeamEntity(null, "한화", "한화");
            var home = new TeamEntity(null, "삼성", "삼성");
            var stadium = StadiumEntity.builder().fullName("대전").build();
            entityManager.persist(away); entityManager.persist(home); entityManager.persist(stadium);
            entityManager.persist(GameMatchEntity.builder().id("20260905HHSS0").season("2026")
                .league(MatchEnum.LeagueType.KBO).matchAt(LocalDateTime.of(2026, 9, 5, 18, 30))
                .awayTeamEntity(away).homeTeamEntity(home).stadiumEntity(stadium).awayScore((short) 3)
                .homeScore((short) 1).status(MatchEnum.MatchStatus.END).build());
            entityManager.flush();
            var diary = diaries.save(new Diary(null, member.id(), "20260905HHSS0", away.getId(), away.getName(),
                    DiaryEnum.ViewType.STADIUM, null, null, null, false, null, null));
            boolean first = gameRecords.record(diary);
            boolean second = gameRecords.record(diaries.findByMemberAndId(member.id(), diary.id()).orElseThrow());
            return new Saved(diaries.findByMemberAndId(member.id(), diary.id()).orElseThrow(),
                    records.findByDiaryId(diary.id()).orElseThrow(), first, second);
        });

        assertThat(saved.diary.id()).isNotNull();
        assertThat(saved.diary.createdAt()).isNotNull();
        assertThat(saved.diary.weather()).isNull();
        assertThat(saved.diary.content()).isNull();
        assertThat(saved.diary.rated()).isTrue();
        assertThat(saved.record.diaryId()).isEqualTo(saved.diary.id());
        assertThat(saved.record.result()).isEqualTo(MatchEnum.ResultType.WIN);
        assertThat(saved.first).isTrue();
        assertThat(saved.second).isFalse();
    }

    private record Saved(Diary diary, kr.co.victoryfairy.diary.domain.GameRecord record, boolean first,
            boolean second) {}
}
