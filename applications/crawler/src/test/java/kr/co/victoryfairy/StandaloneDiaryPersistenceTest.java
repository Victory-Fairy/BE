package kr.co.victoryfairy;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.diary.infrastructure.persistence.DiaryPersistenceAdapter;
import kr.co.victoryfairy.diary.infrastructure.persistence.GameRecordPersistenceAdapter;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.GamePersistenceAdapter;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.StadiumEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.impl.GameMatchCustomRepositoryImpl;
import kr.co.victoryfairy.shared.infrastructure.persistence.config.DBConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = StandaloneDiaryPersistenceTest.PersistenceApplication.class,
        properties = { "spring.jpa.hibernate.ddl-auto=create", "spring.flyway.enabled=false" })
class StandaloneDiaryPersistenceTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired DiaryStore diaries;
    @Autowired GameRecordStore records;
    @Autowired GameRecordDomainService gameRecords;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactions;

    @Test
    void recordsViewingResultWithoutApiMemberClasses() {
        assertThatThrownByClassName("kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity");

        var saved = transactions.execute(status -> {
            var away = new TeamEntity(null, "한화", "한화");
            var home = new TeamEntity(null, "삼성", "삼성");
            var stadium = StadiumEntity.builder().fullName("대전").build();
            entityManager.persist(away);
            entityManager.persist(home);
            entityManager.persist(stadium);
            entityManager.persist(GameMatchEntity.builder().id("20260905HHSS0").season("2026")
                    .league(MatchEnum.LeagueType.KBO).matchAt(LocalDateTime.of(2026, 9, 5, 18, 30))
                    .awayTeamEntity(away).homeTeamEntity(home).stadiumEntity(stadium).awayScore((short) 3)
                    .homeScore((short) 1).status(MatchEnum.MatchStatus.END).build());
            entityManager.flush();
            var diary = diaries.save(new Diary(null, 4242L, "20260905HHSS0", away.getId(), away.getName(),
                    DiaryEnum.ViewType.STADIUM, null, null, null, false, null, null));
            assertThat(gameRecords.record(diary)).isTrue();
            entityManager.flush();
            entityManager.clear();
            return records.findByDiaryId(diary.id()).orElseThrow();
        });

        assertThat(saved.memberId()).isEqualTo(4242L);
        assertThat(saved.result()).isEqualTo(MatchEnum.ResultType.WIN);
    }

    private static void assertThatThrownByClassName(String name) {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> Class.forName(name)))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "kr.co.victoryfairy.diary.infrastructure.persistence.entity",
            "kr.co.victoryfairy.game.infrastructure.persistence.entity" })
    @EnableJpaRepositories(basePackages = { "kr.co.victoryfairy.diary.infrastructure.persistence.repository",
            "kr.co.victoryfairy.game.infrastructure.persistence.repository" })
    @Import({ DiaryPersistenceAdapter.class, GameRecordPersistenceAdapter.class, GamePersistenceAdapter.class,
            GameMatchCustomRepositoryImpl.class, GameRecordDomainService.class, DBConfig.class })
    static class PersistenceApplication {
        @Bean
        AuditorAware<Long> awareAuditService() {
            return java.util.Optional::empty;
        }
    }
}
