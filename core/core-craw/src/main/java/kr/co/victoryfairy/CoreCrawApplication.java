package kr.co.victoryfairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CoreCrawApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CoreCrawApplication.class, args);
        if (context.getEnvironment().getProperty("game-recovery.enabled", Boolean.class, false)) {
            SpringApplication.exit(context);
        }
    }

}
