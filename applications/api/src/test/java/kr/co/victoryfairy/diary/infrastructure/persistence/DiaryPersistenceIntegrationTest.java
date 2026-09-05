package kr.co.victoryfairy.diary.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.victoryfairy.diary.application.admin.AdminDiaryQueryStore;
import kr.co.victoryfairy.diary.domain.DiaryModel;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.diary.presentation.ViewingStatisticsDomain;
import kr.co.victoryfairy.diary.presentation.admin.AdminDiaryDto;
import kr.co.victoryfairy.game.presentation.CommonDomain;
import kr.co.victoryfairy.member.presentation.admin.AdminMemberDto;
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
import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberInfoEntity;
import kr.co.victoryfairy.diary.domain.ViewingRecordReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
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
    @Autowired ViewingRecordReader memberGames;
    @Autowired AdminDiaryQueryStore adminDiaries;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMappings;
    @Autowired WebApplicationContext webContext;
    MockMvc mockMvc;

    @BeforeEach
    void createMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Test
    void springRegistersTheBaselineControllerRoutesExactlyOnce() {
        var actual = handlerMappings.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("kr.co.victoryfairy"))
                .flatMap(entry -> entry.getKey().getMethodsCondition().getMethods().stream()
                        .flatMap(method -> entry.getKey().getPatternValues().stream().map(path -> method + " " + path)))
                .collect(Collectors.toSet());

        assertThat(actual).containsExactlyInAnyOrderElementsOf(BASELINE_ROUTES);
        assertThat(actual).hasSize(49);
    }

    @Test
    void generatedSchemasMatchTheBaselineDtoFixtureFromGit0b7e488() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        assertSchemas(BaselineMyPageDomain.VictoryPowerResponse.class, ViewingStatisticsDomain.VictoryPowerResponse.class);
        assertSchemas(BaselineMyPageDomain.ReportResponse.class, ViewingStatisticsDomain.ReportResponse.class);
        assertSchemas(BaselineCommonDomain.TeamListResponse.class, CommonDomain.TeamListResponse.class);
        assertSchemas(BaselineCommonDomain.SeatListResponse.class, CommonDomain.SeatListResponse.class);
        assertSchemas(BaselineAdminDiaryDto.DiaryListRequest.class, AdminDiaryDto.DiaryListRequest.class);
        assertSchemas(BaselineAdminDiaryDto.DiaryListResponse.class, AdminDiaryDto.DiaryListResponse.class);
        assertSchemas(BaselineAdminMemberDto.MemberListRequest.class, AdminMemberDto.MemberListRequest.class);
        assertSchemas(BaselineAdminMemberDto.MemberListResponse.class, AdminMemberDto.MemberListResponse.class);
    }

    @Test
    void adminDiaryQueryUsesProfileJoinFiltersAndDescendingPagination() {
        var date = LocalDate.of(2026, 9, 5);
        var ids = transactions.execute(status -> {
            var team = new TeamEntity(null, "한화", "한화");
            entityManager.persist(team);
            var first = memberWithProfile("첫째", team);
            var second = memberWithProfile("둘째", team);
            var unmatched = MemberEntity.builder().status(MemberEnum.Status.NORMAL).isUse(true).build();
            entityManager.persist(unmatched);
            var away = new TeamEntity(null, "두산", "두산");
            entityManager.persist(away);
            var firstMatch = match("admin-1", date.atTime(18, 0), MatchEnum.MatchStatus.END, away, team);
            var secondMatch = match("admin-2", date.atTime(19, 0), MatchEnum.MatchStatus.END, away, team);
            var canceledMatch = match("admin-3", date.atTime(20, 0), MatchEnum.MatchStatus.CANCELED, away, team);
            var otherDateMatch = match("admin-4", date.plusDays(1).atTime(18, 0), MatchEnum.MatchStatus.END, away, team);
            entityManager.persist(firstMatch); entityManager.persist(secondMatch);
            entityManager.persist(canceledMatch); entityManager.persist(otherDateMatch);
            var older = diary(first.getId(), firstMatch, team, "older");
            var newer = diary(second.getId(), secondMatch, team, "newer");
            entityManager.persist(older); entityManager.persist(newer);
            entityManager.persist(diary(first.getId(), canceledMatch, team, "canceled"));
            entityManager.persist(diary(first.getId(), otherDateMatch, team, "other-date"));
            entityManager.persist(diary(unmatched.getId(), secondMatch, team, "no-profile"));
            entityManager.flush(); entityManager.clear();
            return new Long[] { older.getId(), newer.getId(), second.getId(), team.getId() };
        });

        var firstPage = transactions.execute(status -> adminDiaries.findAll(
                new DiaryModel.DiaryListRequest(date, MatchEnum.MatchStatus.END, 1, 1)));
        var secondPage = transactions.execute(status -> adminDiaries.findAll(
                new DiaryModel.DiaryListRequest(date, MatchEnum.MatchStatus.END, 2, 1)));

        assertThat(firstPage.total()).isEqualTo(2);
        assertThat(firstPage.contents()).hasSize(1);
        assertThat(firstPage.contents().getFirst()).satisfies(row -> {
            assertThat(row.getId()).isEqualTo(ids[1]);
            assertThat(row.getMemberId()).isEqualTo(ids[2]);
            assertThat(row.getNickNm()).isEqualTo("둘째");
            assertThat(row.getTeamId()).isEqualTo(ids[3]);
            assertThat(row.getTeamName()).isEqualTo("한화");
            assertThat(row.getContent()).isEqualTo("newer");
            assertThat(row.getStatus()).isEqualTo(MatchEnum.MatchStatus.END);
        });
        assertThat(secondPage.contents()).extracting(DiaryModel.DiaryListResponse::getId).containsExactly(ids[0]);
    }

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

    private MemberEntity memberWithProfile(String nickname, TeamEntity team) {
        var member = MemberEntity.builder().status(MemberEnum.Status.NORMAL).isUse(true).build();
        entityManager.persist(member);
        entityManager.persist(MemberInfoEntity.builder().memberEntity(member).teamEntity(team)
                .snsId("sns-" + nickname).email(nickname + "@test.invalid").nickNm(nickname)
                .snsType(MemberEnum.SnsType.KAKAO).build());
        return member;
    }

    private GameMatchEntity match(String id, LocalDateTime at, MatchEnum.MatchStatus status, TeamEntity away,
            TeamEntity home) {
        return GameMatchEntity.builder().id(id).season("2026").league(MatchEnum.LeagueType.KBO).matchAt(at)
                .awayTeamEntity(away).homeTeamEntity(home).awayScore((short) 1).homeScore((short) 2)
                .status(status).build();
    }

    private DiaryEntity diary(Long memberId, GameMatchEntity match, TeamEntity team, String content) {
        return DiaryEntity.builder().memberId(memberId).gameMatchEntity(match).teamName(team.getName())
                .teamEntity(team).viewType(DiaryEnum.ViewType.STADIUM).moodType(DiaryEnum.MoodType.HAPPY)
                .weatherType(DiaryEnum.WeatherType.SUNNY).content(content).build();
    }

    private void assertSchemas(Class<?> baseline, Class<?> current) {
        var baselineSchemas = new ModelConverters().readAll(baseline);
        var currentSchemas = new ModelConverters().readAll(current);
        assertThat(currentSchemas.keySet()).as("schema names for %s", current.getSimpleName())
                .containsExactlyInAnyOrderElementsOf(baselineSchemas.keySet());
        baselineSchemas.forEach((name, schema) -> assertThat(structure(currentSchemas.get(name)))
                .as("structural OpenAPI schema of %s", name).isEqualTo(structure(schema)));
    }

    private Map<String, Object> structure(io.swagger.v3.oas.models.media.Schema<?> schema) {
        var result = new LinkedHashMap<String, Object>();
        result.put("type", schema.getType());
        result.put("format", schema.getFormat());
        result.put("ref", schema.get$ref());
        result.put("nullable", schema.getNullable());
        result.put("required", schema.getRequired() == null ? List.of() : schema.getRequired());
        result.put("enum", schema.getEnum() == null ? List.of() : schema.getEnum());
        result.put("items", schema.getItems() == null ? null : structure(schema.getItems()));
        var properties = new LinkedHashMap<String, Object>();
        if (schema.getProperties() != null) schema.getProperties().forEach((name, value) -> properties.put(name,
                structure((io.swagger.v3.oas.models.media.Schema<?>) value)));
        result.put("properties", properties);
        return result;
    }

    // Exact DTO fixture transcribed from git 0b7e488 before running ModelConverters.
    private interface BaselineMyPageDomain {
        record VictoryPowerResponse(Short level, Short power) {}
        record ReportResponse(ViewTypeDto stadium, ViewTypeDto home, ViewStatisticsDto viewStatistics) {}
        record ViewTypeDto(Short winAvg, Short win, Short lose, Short draw, Short cancel) {}
        record ViewStatisticsDto(String winTeam, String lossTeam, String stadium, Short winningStreak,
                Short homeWinAvg, Short stadiumWinAvg) {}
    }

    private interface BaselineCommonDomain {
        @Schema(name = "Common.TeamListResponse")
        record TeamListResponse(Long id, String name, String label, MatchEnum.LeagueType league, String countryCode) {}
        record SeatListResponse(Long id, String name) {}
    }

    private interface BaselineAdminDiaryDto {
        @Schema(name = "Diary.DiaryListRequest")
        record DiaryListRequest(LocalDate date,
                @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) MatchEnum.MatchStatus status,
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer page,
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer size) {}
        @Schema(name = "Diary.DiaryListResponse")
        record DiaryListResponse(Long id, String teamName, String content, String nickNm, LocalDateTime matchAt,
                MatchEnum.MatchStatus status, DiaryEnum.MoodType moodType, DiaryEnum.ViewType viewType,
                DiaryEnum.WeatherType weatherType, java.util.List<String> foods, java.util.List<String> partners,
                java.util.List<String> useHistories) {}
    }

    private interface BaselineAdminMemberDto {
        @Schema(name = "Member.MemberListRequest")
        record MemberListRequest(@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) MemberEnum.SnsType snsType,
                @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String keyword,
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer page,
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer size) {}
        record MemberListResponse(Long id, String nickNm, MemberEnum.SnsType snsType, String email, Long teamId,
                String teamName, String sponsorNm) {}
    }

    private static final Set<String> BASELINE_ROUTES = Set.of(
            "DELETE /api/community/posts/{postId}", "DELETE /api/community/posts/{postId}/comments/{commentId}",
            "DELETE /api/diary/{id}", "DELETE /api/my-page/delete-account", "GET /admin/community/reports",
            "GET /admin/diary/list", "GET /admin/member/list", "GET /api/common/health", "GET /api/common/seat/{id}",
            "GET /api/common/team", "GET /api/community/posts", "GET /api/community/posts/{postId}",
            "GET /api/community/posts/{postId}/comments", "GET /api/diary/daily-list", "GET /api/diary/list",
            "GET /api/diary/{id}", "GET /api/match/list", "GET /api/match/record/{id}", "GET /api/match/today",
            "GET /api/match/{id}", "GET /api/member/auth-path", "GET /api/member/login",
            "GET /api/member/match-today", "GET /api/member/win-rate", "GET /api/my-page/member",
            "GET /api/my-page/report", "GET /api/my-page/victory-power", "PATCH /admin/auth/refresh-token",
            "PATCH /admin/community/reports/{targetType}/{reportId}", "PATCH /api/auth/refresh-token",
            "PATCH /api/community/posts/{postId}", "PATCH /api/community/posts/{postId}/comments/{commentId}",
            "PATCH /api/community/posts/{postId}/comments/{commentId}/likes", "PATCH /api/community/posts/{postId}/likes",
            "PATCH /api/diary/{id}", "PATCH /api/member/nick-name", "PATCH /api/member/profile",
            "PATCH /api/member/refresh-token", "POST /admin/auth/login", "POST /api/community/posts",
            "POST /api/community/posts/{postId}/comments", "POST /api/community/posts/{postId}/comments/{commentId}/reports",
            "POST /api/community/posts/{postId}/reports", "POST /api/diary", "POST /api/member/check-nick-duplicate",
            "POST /api/member/logout", "POST /api/redirect/apple", "POST /file/upload", "PUT /api/member/team");

    private record Saved(Diary diary, kr.co.victoryfairy.diary.domain.GameRecord record, boolean first,
            boolean second) {}
}
