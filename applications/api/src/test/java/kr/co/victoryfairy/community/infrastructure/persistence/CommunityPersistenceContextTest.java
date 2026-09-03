package kr.co.victoryfairy.community.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DirtiesContext
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.profiles.active=test",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.data.redis.repositories.enabled=false",
            "jwt.secret-key=test-only-secret-key-test-only-secret-key-test-only-secret-key-1234",
            "jwt.access-token-expire-minutes=30",
            "jwt.refresh-token-expire-days=7",
            "auth.kakao.cas.client_id=test",
            "auth.kakao.cas.client_secret=test",
            "auth.kakao.cas.callback_url=http://localhost/test",
            "auth.google.cas.client_id=test",
            "auth.google.cas.client_secret=test",
            "auth.google.cas.callback_url=http://localhost/test",
            "auth.apple.cas.client_id=test",
            "auth.apple.cas.client_secret=test",
            "auth.apple.cas.team_id=test",
            "auth.apple.cas.key_id=test",
            "auth.apple.cas.callback_url=http://localhost/test",
            "auth.apple.cas.secret_path=test",
            "webhook.slack.url=http://localhost/disabled",
            "victory-fairy.file.storage-path=/tmp/victoryfairy-test",
            "victory-fairy.file.s3-enabled=false"
        })
class CommunityPersistenceContextTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("storage.datasource.core.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("storage.datasource.core.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("storage.datasource.core.username", MYSQL::getUsername);
        registry.add("storage.datasource.core.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.database", () -> 1);
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void registersCommunityJpaRepositoriesInTheApplicationContext() {
        assertThat(context.getBean(CommunityPostJpaRepository.class)).isNotNull();
        assertThat(context.getBean(CommunityPostFileJpaRepository.class)).isNotNull();
        assertThat(context.getBean(CommunityCommentJpaRepository.class)).isNotNull();
    }

}
