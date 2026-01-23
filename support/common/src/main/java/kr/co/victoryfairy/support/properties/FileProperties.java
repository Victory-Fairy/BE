package kr.co.victoryfairy.support.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "victory-fairy.file")
@Getter
@Setter
public class FileProperties {

    private String storagePath;

    private Integer[] imageResizes;

    private Integer[] videoResizes;

}
