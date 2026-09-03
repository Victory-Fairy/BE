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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("local")
@DirtiesContext
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.profiles.active=local")
class CommunityPersistenceContextTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("storage.datasource.core.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("storage.datasource.core.username", MYSQL::getUsername);
        registry.add("storage.datasource.core.password", MYSQL::getPassword);
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
