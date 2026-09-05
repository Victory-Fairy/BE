package kr.co.victoryfairy.media.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileEntity;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileRefEntity;
import kr.co.victoryfairy.media.infrastructure.persistence.repository.FileRefRepository;
import kr.co.victoryfairy.member.application.MemberCommandService;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.web.error.CustomException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
class MediaPersistenceIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
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

    @Autowired private FileReferenceService fileReferences;
    @Autowired private MemberCommandService members;
    @Autowired private FileRefRepository fileRefRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private TransactionTemplate transactions;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void deleteUsesTheSingleFetchQueryRegardlessOfReferenceCount() {
        createReferences(RefType.DIARY, 10L, 3);

        var sql = transactions.execute(status -> {
            statistics().clear();
            fileReferences.deleteFileRefs(RefType.DIARY, 10L);
            return metrics();
        });

        assertThat(sql.preparedStatements()).isEqualTo(1L);
        assertThat(sql.queryExecutions()).isEqualTo(1L);
        assertThat(sql.entityFetches()).isZero();
        assertThat(activeReferences(RefType.DIARY, 10L)).isZero();
    }

    @Test
    void replaceAddsOnlyTheExistingBatchFetchAndNewFileBatchFetch() {
        createReferences(RefType.DIARY, 20L, 3);
        var replacements = createFiles(2);

        var sql = transactions.execute(status -> {
            statistics().clear();
            fileReferences.replaceFileRefs(RefType.DIARY, 20L, replacements);
            return metrics();
        });

        assertThat(sql.preparedStatements()).isEqualTo(4L);
        assertThat(sql.queryExecutions()).isEqualTo(2L);
        assertThat(sql.entityFetches()).isZero();
        assertThat(activeReferences(RefType.DIARY, 20L)).isEqualTo(2L);
    }

    @Test
    void missingProfileFileRollsBackPreviousProfileSoftDelete() {
        createReferences(RefType.PROFILE, 30L, 1);
        authenticate(30L);

        assertThatThrownBy(() -> members.updateMemberProfile(new MemberDomain.MemberProfileUpdateRequest(Long.MAX_VALUE)))
            .isInstanceOf(CustomException.class);

        assertThat(activeReferences(RefType.PROFILE, 30L)).isOne();
    }

    private void createReferences(RefType type, Long refId, int count) {
        transactions.executeWithoutResult(status -> createFilesInCurrentTransaction(count).forEach(fileId -> {
            var file = entityManager.getReference(FileEntity.class, fileId);
            entityManager.persist(FileRefEntity.builder().fileEntity(file).refId(refId).refType(type).build());
        }));
    }

    private List<Long> createFiles(int count) {
        return transactions.execute(status -> createFilesInCurrentTransaction(count));
    }

    private List<Long> createFilesInCurrentTransaction(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(index -> {
            var file = FileEntity.builder().name("file-" + index).saveName("saved-" + index).path("image/test")
                .ext("jpg").size(1L).build();
            entityManager.persist(file);
            entityManager.flush();
            return file.getId();
        }).toList();
    }

    private long activeReferences(RefType type, Long refId) {
        return transactions.execute(status -> (long) fileRefRepository
            .findAllByRefTypeAndRefIdAndIsUseTrue(type, refId).size());
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private SqlMetrics metrics() {
        var statistics = statistics();
        return new SqlMetrics(statistics.getPrepareStatementCount(), statistics.getQueryExecutionCount(),
                statistics.getEntityFetchCount());
    }

    private void authenticate(Long memberId) {
        var request = new MockHttpServletRequest();
        request.setAttribute("accountByToken", MemberAccount.builder().id(memberId).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private record SqlMetrics(long preparedStatements, long queryExecutions, long entityFetches) {
    }

}
