package kr.co.victoryfairy.storage.db.core.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import kr.co.victoryfairy.logging.sql.SqlLoggingHolder;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * P6Spy 설정 - 에러 발생 시 쿼리 로깅을 위해 SQL을 ThreadLocal에 저장
 */
@Configuration
public class P6SpyConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(P6SpyCustomFormatter.class.getName());
    }

    public static class P6SpyCustomFormatter implements MessageFormattingStrategy {

        @Override
        public String formatMessage(int connectionId, String now, long elapsed, String category,
                String prepared, String sql, String url) {

            if (sql == null || sql.trim().isEmpty()) {
                return "";
            }

            // SQL을 ThreadLocal에 저장 (에러 시 조회용)
            SqlLoggingHolder.addSql(sql, elapsed);

            // 일반 로깅은 하지 않음 (에러 시에만 ExceptionAdvice에서 로깅)
            // 느린 쿼리만 로깅 (500ms 이상)
            if (elapsed > 500) {
                return formatSlowQueryLog(connectionId, elapsed, category, sql);
            }

            return ""; // 빈 문자열 반환하면 로깅 안 됨
        }

        private String formatSlowQueryLog(int connectionId, long elapsed, String category, String sql) {
            String formattedSql = formatSql(category, sql);
            return String.format("\n[SLOW QUERY - %dms] connection=%d\n%s",
                    elapsed, connectionId, formattedSql);
        }

        private String formatSql(String category, String sql) {
            if (sql == null || sql.trim().isEmpty()) {
                return sql;
            }

            // DDL, DCL은 그대로
            if (Category.STATEMENT.getName().equals(category)) {
                String trimmedSql = sql.trim().toLowerCase(Locale.ROOT);
                if (trimmedSql.startsWith("create") || trimmedSql.startsWith("alter") ||
                        trimmedSql.startsWith("drop") || trimmedSql.startsWith("comment")) {
                    return FormatStyle.DDL.getFormatter().format(sql);
                }
            }

            // DML은 보기 좋게 포맷팅
            return FormatStyle.BASIC.getFormatter().format(sql);
        }
    }
}
