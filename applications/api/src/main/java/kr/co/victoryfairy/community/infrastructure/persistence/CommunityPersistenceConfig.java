package kr.co.victoryfairy.community.infrastructure.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = CommunityPostJpaEntity.class)
@EnableJpaRepositories(basePackageClasses = CommunityPostJpaRepository.class)
class CommunityPersistenceConfig {

}
