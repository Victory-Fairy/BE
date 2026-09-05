package kr.co.victoryfairy.shared.infrastructure.persistence.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = { "kr.co.victoryfairy.admin.infrastructure.persistence.entity",
        "kr.co.victoryfairy.diary.infrastructure.persistence.entity",
        "kr.co.victoryfairy.game.infrastructure.persistence.entity",
        "kr.co.victoryfairy.media.infrastructure.persistence.entity",
        "kr.co.victoryfairy.member.infrastructure.persistence.entity" })
@EnableJpaRepositories(basePackages = { "kr.co.victoryfairy.admin.infrastructure.persistence.repository",
        "kr.co.victoryfairy.diary.infrastructure.persistence.repository",
        "kr.co.victoryfairy.game.infrastructure.persistence.repository",
        "kr.co.victoryfairy.media.infrastructure.persistence.repository",
        "kr.co.victoryfairy.member.infrastructure.persistence.repository" })
class CoreJpaConfig {

}
