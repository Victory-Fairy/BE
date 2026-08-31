package kr.co.victoryfairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class VictoryFairyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VictoryFairyApiApplication.class, args);
    }

}
