package kr.co.victoryfairy.community.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("local")
@DirtiesContext
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.profiles.active=local")
class CommunityPersistenceContextTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
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
