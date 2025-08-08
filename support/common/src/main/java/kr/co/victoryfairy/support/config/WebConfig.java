package kr.co.victoryfairy.support.config;

import kr.co.victoryfairy.support.interceptor.CurlCommandErrorInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web에 사용되는 Configuration을 여기에 위치 한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurlCommandErrorInterceptor curlCommandErrorInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v2/api/**")
                .allowedOrigins(
                        "http://localhost:8080",
                        "http://localhost:3000",
                        "https://victory-fairy.duckdns.org",
                        "https://victoryfairy.shop",
                        "https://seungyo.shop"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/v2/file/**")
                .allowedOrigins(
                        "http://localhost:8080",
                        "http://localhost:3000",
                        "https://victory-fairy.duckdns.org",
                        "https://victoryfairy.shop",
                        "https://seungyo.shop"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/v2/admin/**")
                .allowedOrigins(
                        "http://localhost:8080",
                        "http://localhost:3000",
                        "https://victory-fairy.duckdns.org",
                        "https://victoryfairy.shop",
                        "https://seungyo.shop"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(curlCommandErrorInterceptor)
                .addPathPatterns("/**"); // 모든 요청에 적용
    }

}
