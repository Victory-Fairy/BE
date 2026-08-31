package kr.co.victoryfairy.support.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import kr.co.victoryfairy.support.properties.FileProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3PresignedUrlServiceTest {

    @Test
    void signsExactObjectKeyForConfiguredDuration() {
        FileProperties properties = new FileProperties();
        properties.setS3Enabled(true);
        properties.setS3Bucket("victory-fairy-files");
        properties.setPresignedUrlDuration(Duration.ofMinutes(15));

        try (S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
            .build()) {
            String url = new S3PresignedUrlService(Optional.of(presigner), properties).create("image/profile/202608",
                    "sample", "jpg");
            URI uri = URI.create(url);

            assertThat(uri.getPath()).endsWith("/image/profile/202608/sample.jpg");
            assertThat(uri.getQuery()).contains("X-Amz-Expires=900");
        }
    }

    @Test
    void returnsNullWhenS3IsDisabled() {
        FileProperties properties = new FileProperties();
        properties.setS3Enabled(false);

        assertThat(new S3PresignedUrlService(Optional.empty(), properties).create("image/profile/202608", "sample",
                "jpg")).isNull();
    }

}
