package kr.co.victoryfairy.support.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SlackUtils {

    @Value("${webhook.slack.url}")
    private String slackWebhookUrl;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public void message(String text) {
        if (activeProfile.equals("local")) {
            return;
        }
        RestTemplate restTemplate = new RestTemplate();

        // JSON 데이터 생성
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        // ObjectMapper를 사용하여 Map을 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to convert request body to JSON", e);
        }

        // HttpHeaders 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // HttpEntity 생성
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // POST 요청 보내기
        ResponseEntity<String> response = restTemplate.exchange(slackWebhookUrl, HttpMethod.POST, entity, String.class);
    }

    public void sendSlackCurl(String url, String curl, String responseBody) {
        try {
            if (activeProfile.equals("local")) {
                return;
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            StringBuffer sb = new StringBuffer();

            sb.append("[Victory Fairy / ")
                .append(InetAddress.getLocalHost().getHostName())
                .append(" / ")
                .append(activeProfile)
                .append("]")
                .append("\n\n")

                .append("```\n")
                .append(url)
                .append("\n")
                .append("```")

                .append("\n\n")

                .append("```\n")
                .append(responseBody)
                .append("\n")
                .append("```")

                .append("\n\n")

                .append("```\n")
                .append(curl)
                .append("\n")
                .append("```");

            Map<String, List<Map<String, String>>> body = new HashMap<>();
            Map<String, String> content = new HashMap<>();

            content.put("text", sb.toString());
            content.put("color", "#a84432");
            body.put("attachments", Arrays.asList(content));

            HttpEntity<Map> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(slackWebhookUrl, request, String.class);
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
