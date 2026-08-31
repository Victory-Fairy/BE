package kr.co.victoryfairy.media.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import kr.co.victoryfairy.support.properties.FileProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3FileUploaderTest {

    @Test
    void uploadsFileWithStorageRelativeObjectKey(@TempDir Path storageRoot) throws Exception {
        Path file = storageRoot.resolve("image/profile/202608/sample.jpg");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "image");

        S3Client s3Client = org.mockito.Mockito.mock(S3Client.class);
        FileProperties properties = new FileProperties();
        properties.setS3Bucket("victory-fairy-files");

        new S3FileUploader(s3Client, properties).upload(storageRoot, List.of(file));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("victory-fairy-files");
        assertThat(request.getValue().key()).isEqualTo("image/profile/202608/sample.jpg");
        assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
    }

}
