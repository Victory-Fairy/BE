package kr.co.victoryfairy.logging.sql;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 현재 스레드에서 실행된 SQL을 저장하는 ThreadLocal 홀더
 * 에러 발생 시 ExceptionAdvice에서 조회하여 로깅
 */
public final class SqlLoggingHolder {

    private static final ThreadLocal<Deque<SqlInfo>> SQL_HOLDER = ThreadLocal.withInitial(ArrayDeque::new);
    private static final int MAX_SQL_COUNT = 50; // 최대 저장 개수

    private SqlLoggingHolder() {
    }

    /**
     * SQL 정보 저장
     */
    public static void addSql(String sql, long executionTimeMs) {
        Deque<SqlInfo> deque = SQL_HOLDER.get();
        if (deque.size() >= MAX_SQL_COUNT) {
            deque.pollFirst(); // 오래된 것 제거
        }
        deque.addLast(new SqlInfo(sql, executionTimeMs, System.currentTimeMillis()));
    }

    /**
     * 저장된 모든 SQL 조회
     */
    public static Deque<SqlInfo> getSqlList() {
        return SQL_HOLDER.get();
    }

    /**
     * 마지막 SQL 조회
     */
    public static SqlInfo getLastSql() {
        Deque<SqlInfo> deque = SQL_HOLDER.get();
        return deque.isEmpty() ? null : deque.peekLast();
    }

    /**
     * 초기화 (요청 종료 시 호출)
     */
    public static void clear() {
        SQL_HOLDER.remove();
    }

    /**
     * SQL 정보 레코드
     */
    public record SqlInfo(String sql, long executionTimeMs, long timestamp) {

        public String toFormattedString() {
            return String.format("[%dms] %s", executionTimeMs, formatSql(sql));
        }

        private static String formatSql(String sql) {
            if (sql == null) {
                return "";
            }
            // 기본 포맷팅: 키워드 앞에 줄바꿈
            return sql.trim()
                    .replaceAll("(?i)\\s+(SELECT|FROM|WHERE|AND|OR|JOIN|LEFT|RIGHT|INNER|OUTER|ON|ORDER|GROUP|HAVING|LIMIT|OFFSET|INSERT|UPDATE|DELETE|SET|VALUES)\\s+",
                            "\n    $1 ")
                    .replaceAll("\\s+", " ")
                    .replaceAll("\n\\s+", "\n    ");
        }
    }
}
