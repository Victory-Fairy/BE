package kr.co.victoryfairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CoreCrawApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreCrawApplication.class, args);
    }

}
