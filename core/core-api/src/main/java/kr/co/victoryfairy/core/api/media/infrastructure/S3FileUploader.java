package kr.co.victoryfairy.core.api.media.infrastructure;

import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;

import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.properties.FileProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(prefix = "victory-fairy.file", name = "s3-enabled", havingValue = "true")
public class S3FileUploader {

    private final S3Client s3Client;

    private final FileProperties fileProperties;

    public S3FileUploader(S3Client s3Client, FileProperties fileProperties) {
        this.s3Client = s3Client;
        this.fileProperties = fileProperties;
    }

    public void upload(Path storageRoot, List<Path> files) {
        Path normalizedRoot = storageRoot.toAbsolutePath().normalize();

        try {
            for (Path file : files) {
                Path normalizedFile = file.toAbsolutePath().normalize();
                if (!normalizedFile.startsWith(normalizedRoot)) {
                    throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
                }

                String key = normalizedRoot.relativize(normalizedFile).toString().replace('\\', '/');
                String contentType = URLConnection.guessContentTypeFromName(normalizedFile.getFileName().toString());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(fileProperties.getS3Bucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
                s3Client.putObject(request, RequestBody.fromFile(normalizedFile));
            }
        }
        catch (SdkException e) {
            throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
        }
    }

}
