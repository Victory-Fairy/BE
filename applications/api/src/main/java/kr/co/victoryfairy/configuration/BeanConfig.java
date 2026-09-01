package kr.co.victoryfairy.configuration;

import jakarta.servlet.Filter;
import kr.co.victoryfairy.web.filter.RequestResponseCachingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * 일반적인 Bean으로 등록될 Configuration을 여기에 위치 한다.
 */
@Configuration
public class BeanConfig {

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public Filter requestResponseCachingFilter() {
        return new RequestResponseCachingFilter();
    }

    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    }

}
