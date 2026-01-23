package kr.co.victoryfairy.support.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public enum Format {

        DATE_FORMAT_HYPEN("yyyy-MM-dd"), DATE_FORMAT_TRIM("yyyyMMdd"), DATE_FORMAT_SLASH("yyyy/MM/dd"),
        DATETIME_FORMAT_HYPEN("yyyy-MM-dd HH:mm:ss"), DATETIME_FORMAT_HYPEN_MS("yyyy-MM-dd HH:mm:ss.SSS"),
        DATETIME_FORMAT_TRIM("yyyyMMddHHmmss"), DATETIME_FORMAT_YEAR_TRIM("yyyy"), DATETIME_FORMAT_MONTH_TRIM("yyyyMM"),
        DATETIME_FORMAT_DAY_TRIM("yyyyMMdd"), DATETIME_FORMAT_HOUR_TRIM("yyyyMMddHH"),
        DATETIME_FORMAT_SLASH("yyyy/MM/dd HH:mm:ss"), DATE_FORMAT_DOT("yy.MM.dd"),
        DATETIME_FORMAT_HYPEN_SHORT("yy-MM-dd");

        private final String pattern;

        public String getPattern() {
            return pattern;
        }

        Format(String pattern) {
            this.pattern = pattern;
        }

    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static String now(String pattern) {
        return toFormatter(pattern).format(LocalDateTime.now());
    }

    public static String toString(Long timestamp, String pattern) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp),
                ZoneId.of("Asia/Seoul"));
        return toFormatter(pattern).format(localDateTime);
    }

    public static String toString(LocalDateTime localDateTime, String pattern) {
        return toFormatter(pattern).format(localDateTime);
    }

    public static String toString(LocalDate localDate, String pattern) {
        return toFormatter(pattern).format(localDate);
    }

    public static LocalDate toDate(String date, String pattern) {
        return LocalDate.parse(date, toFormatter(pattern));
    }

    public static LocalDateTime toDateTime(String dateTime, String pattern) {
        return LocalDateTime.parse(dateTime, toFormatter(pattern));
    }

    private static DateTimeFormatter toFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern);
    }

}
