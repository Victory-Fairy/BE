package kr.co.victoryfairy.core.event.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Component
public class FCMInitializer {

    @PostConstruct
    public void initialize() throws IOException {
        Resource resource = ResourcePatternUtils
                .getResourcePatternResolver(new DefaultResourceLoader())
                .getResource("classpath:cert/victory-fairy-69eb5-firebase-adminsdk-fbsvc-01c6b5ea5a.json");
        InputStream inputStream = resource.getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(inputStream))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

}
