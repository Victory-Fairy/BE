package kr.co.victoryfairy.core.file.config;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.victoryfairy.support.properties.FileProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

class S3FileConfigurationTest {

    @Test
    void createsS3ClientWithRuntimeDependencies() {
        FileProperties properties = new FileProperties();
        properties.setS3Region("ap-northeast-2");

        try (S3Client client = new S3FileConfiguration().s3Client(properties)) {
            assertThat(client).isNotNull();
        }
    }

}
