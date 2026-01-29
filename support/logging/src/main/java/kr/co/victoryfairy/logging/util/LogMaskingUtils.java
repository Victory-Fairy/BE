package kr.co.victoryfairy.logging.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그에서 민감정보를 마스킹하는 유틸리티
 */
public final class LogMaskingUtils {

    private static final Set<String> SENSITIVE_PARAM_NAMES = new HashSet<>(Arrays.asList(
            "password", "pwd", "secret", "token", "accessToken", "refreshToken",
            "authorization", "apiKey", "apiSecret", "clientSecret", "privateKey",
            "credential", "code" // OAuth code
    ));

    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]*\\.eyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*");
    private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+([A-Za-z0-9_.-]+)");

    private LogMaskingUtils() {
    }

    /**
     * JWT 토큰 마스킹 (앞 10자 + ... + 뒤 5자)
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 5);
    }

    /**
     * 파라미터 이름이 민감정보인지 확인
     */
    public static boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lowerName = paramName.toLowerCase();
        return SENSITIVE_PARAM_NAMES.stream()
                .anyMatch(sensitive -> lowerName.contains(sensitive.toLowerCase()));
    }

    /**
     * 파라미터 값 마스킹 (민감정보면 마스킹, 아니면 그대로)
     */
    public static String maskIfSensitive(String paramName, Object value) {
        if (value == null) {
            return "null";
        }
        if (isSensitiveParam(paramName)) {
            return maskValue(value.toString());
        }
        return truncateIfLong(value.toString(), 100);
    }

    /**
     * 값 마스킹 (앞 3자 + *** + 뒤 2자)
     */
    public static String maskValue(String value) {
        if (value == null || value.length() < 8) {
            return "***";
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 2);
    }

    /**
     * 문자열 내 JWT 토큰 마스킹
     */
    public static String maskJwtInString(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = JWT_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, maskToken(matcher.group()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Bearer 토큰 마스킹
     */
    public static String maskBearerToken(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = BEARER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "Bearer " + maskToken(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 긴 문자열 자르기
     */
    public static String truncateIfLong(String value, int maxLength) {
        if (value == null) {
            return "null";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated, total: " + value.length() + ")";
    }

    /**
     * 객체를 로그용 문자열로 변환 (민감정보 마스킹 포함)
     */
    public static String toLogString(Object obj) {
        if (obj == null) {
            return "null";
        }
        String str = obj.toString();
        str = maskJwtInString(str);
        str = maskBearerToken(str);
        return truncateIfLong(str, 500);
    }
}
