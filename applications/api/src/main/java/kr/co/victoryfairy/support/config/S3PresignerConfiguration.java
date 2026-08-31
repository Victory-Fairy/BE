package kr.co.victoryfairy.support.config;

import kr.co.victoryfairy.support.properties.FileProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(prefix = "victory-fairy.file", name = "s3-enabled", havingValue = "true")
public class S3PresignerConfiguration {

    @Bean
    S3Presigner s3Presigner(FileProperties fileProperties) {
        return S3Presigner.builder()
            .region(Region.of(fileProperties.getS3Region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }

}
