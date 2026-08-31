package kr.co.victoryfairy.support.service;

import java.util.Optional;

import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.properties.FileProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class S3PresignedUrlService {

    private final Optional<S3Presigner> s3Presigner;

    private final FileProperties fileProperties;

    public S3PresignedUrlService(Optional<S3Presigner> s3Presigner, FileProperties fileProperties) {
        this.s3Presigner = s3Presigner;
        this.fileProperties = fileProperties;
    }

    public String create(String path, String saveName, String ext) {
        if (!fileProperties.isS3Enabled()) {
            return null;
        }

        String key = (path + "/" + saveName + "." + ext).replace('\\', '/').replaceFirst("^/+", "");
        GetObjectRequest objectRequest = GetObjectRequest.builder()
            .bucket(fileProperties.getS3Bucket())
            .key(key)
            .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(fileProperties.getPresignedUrlDuration())
            .getObjectRequest(objectRequest)
            .build();

        try {
            return s3Presigner.orElseThrow(() -> new CustomException(MessageEnum.File.FAIL_DOWNLOAD))
                .presignGetObject(presignRequest)
                .url()
                .toString();
        }
        catch (SdkException e) {
            throw new CustomException(MessageEnum.File.FAIL_DOWNLOAD);
        }
    }

}
