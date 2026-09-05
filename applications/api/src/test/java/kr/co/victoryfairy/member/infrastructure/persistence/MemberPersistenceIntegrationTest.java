package kr.co.victoryfairy.member.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.domain.MemberStore;
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
class MemberPersistenceIntegrationTest {

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

    @Autowired MemberStore members;
    @Autowired TransactionTemplate transactions;

    @Test
    void generatedMemberIdLinksProfileAndManagedUpdatesKeepAuditAndNullableFields() {
        var saved = transactions.execute(status -> {
            var member = members.saveMember(Member.normal("old-ip", LocalDateTime.of(2026, 9, 5, 1, 0)));
            var profile = members.saveProfile(MemberProfile.social(member.id(), MemberEnum.SnsType.APPLE, "sns",
                    "member@test.com"));
            var createdAt = member.createdAt();

            member = members.saveMember(member.login("new-ip", LocalDateTime.of(2026, 9, 5, 2, 0)));
            profile = members.saveProfile(profile.withNickname("nickname"));
            return new Saved(member, profile, createdAt);
        });

        assertThat(saved.profile.memberId()).isEqualTo(saved.member.id());
        assertThat(saved.member.lastConnectIp()).isEqualTo("new-ip");
        assertThat(saved.member.createdAt()).isEqualTo(saved.createdAt);
        assertThat(saved.profile.nickNm()).isEqualTo("nickname");
        assertThat(saved.profile.teamId()).isNull();
    }

    private record Saved(Member member, MemberProfile profile, LocalDateTime createdAt) {
    }
}
