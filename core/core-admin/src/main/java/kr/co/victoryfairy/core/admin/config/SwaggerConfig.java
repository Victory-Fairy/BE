package kr.co.victoryfairy.core.admin.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import kr.co.victoryfairy.support.utils.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SecurityScheme(
        name = "accessToken",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Configuration
public class SwaggerConfig {

    private static final String TITLE = "VictoryFairy Admin API Docs";
    private static final String APP_START_TIME = DateUtils.now(DateUtils.Format.DATETIME_FORMAT_HYPEN.getPattern());

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(TITLE)
                        .description(String.format("* Application Start Time : %s", APP_START_TIME))
                        .version("v1.0"));
    }
}
