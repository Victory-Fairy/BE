package kr.co.victoryfairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoreEventApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CoreEventApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // <-- 여기!
        app.run(args);
    }

}
