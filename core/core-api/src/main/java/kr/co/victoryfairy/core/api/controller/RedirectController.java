package kr.co.victoryfairy.core.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping()
@RequiredArgsConstructor
public class RedirectController {

    private final ObjectMapper objectMapper;

    private final Logger log = LoggerFactory.getLogger(RedirectController.class);

    /*
     * @PostMapping(value = "/auth/callbackApple", consumes =
     * MediaType.APPLICATION_FORM_URLENCODED_VALUE) public void
     * callbackApple(@ModelAttribute dto code, HttpServletResponse response) {
     * log.info("code : {}", code.getCode()); }
     */

    @PostMapping(value = "/redirect/apple", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void callbackApple(@ModelAttribute Dto dto, HttpServletResponse response) {
        String name = "";
        String email = "";

        try {
            if (dto.getUser() != null) {
                // user 필드 값을 UserDTO 객체로 변환
                log.info("Raw user JSON: " + dto.getUser());
                UserDTO user = objectMapper.readValue(dto.getUser(), UserDTO.class);
                if (user.getName() != null) {
                    name = user.getName().getLastName() + user.getName().getFirstName();
                    // 이름 인코딩
                    name = URLEncoder.encode(name, StandardCharsets.UTF_8.toString());
                }
                if (user.getEmail() != null) {
                    email = user.getEmail();
                }
            }
        }
        catch (Exception e) {
            log.error("Failed to process user JSON", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String fullRedirectUrl = UriComponentsBuilder.fromUriString("https://seungyo.shop/auth/callbackApple")
            .queryParam("code", dto.getCode())
            .queryParam("name", name)
            .queryParam("email", email)
            .build()
            .toUriString();

        log.info("Redirecting to: " + fullRedirectUrl);

        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", fullRedirectUrl);
    }

    class Dto {

        private String code;

        private String user;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

    }

    public static class UserDTO {

        private Name name;

        private String email;

        public Name getName() {
            return name;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public static class Name {

            private String firstName;

            private String lastName;

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

        }

    }

}
