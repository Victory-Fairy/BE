package kr.co.victoryfairy.core.file.config;

import kr.co.victoryfairy.support.properties.FileProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "victory-fairy.file", name = "s3-enabled", havingValue = "true")
public class S3FileConfiguration {

    @Bean
    S3Client s3Client(FileProperties fileProperties) {
        return S3Client.builder()
            .region(Region.of(fileProperties.getS3Region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }

}
