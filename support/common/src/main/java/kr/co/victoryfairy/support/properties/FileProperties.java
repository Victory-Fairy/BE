package kr.co.victoryfairy.support.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "victory-fairy.file")
@Getter
@Setter
public class FileProperties {

    private String storagePath;

    private Integer[] imageResizes;

    private Integer[] videoResizes;

    private boolean s3Enabled;

    private String s3Bucket;

    private String s3Region;

    private Duration presignedUrlDuration = Duration.ofMinutes(15);

}
