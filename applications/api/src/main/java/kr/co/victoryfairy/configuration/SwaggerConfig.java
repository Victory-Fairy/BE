package kr.co.victoryfairy.configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SecurityScheme(name = "accessToken", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Configuration
public class SwaggerConfig {

    private final Environment env;

    public SwaggerConfig(Environment env) {
        this.env = env;
    }

    private static final String TITLE = "VictoryFairy API Docs";

    private static final String APP_START_TIME = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    @Bean
    public OpenAPI openAPI() {
        String activeProfile = String.join(",", env.getActiveProfiles());

        return new OpenAPI().info(new Info().title("VictoryFairy " + activeProfile + " API Docs")
            .description(String.format("* Application Start Time : %s", APP_START_TIME))
            .version("v1.0"));
    }

}
