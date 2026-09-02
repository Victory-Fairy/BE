package kr.co.victoryfairy.configuration.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class JdbcQueryMetricsRecorder {

    private static final String METRIC_NAME = "victoryfairy.database.query";

    private final MeterRegistry meterRegistry;

    public JdbcQueryMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String sql, boolean failed, Duration duration) {
        Timer.builder(METRIC_NAME)
                .tags("operation", operationOf(sql), "outcome", failed ? "error" : "success")
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5))
                .register(meterRegistry)
                .record(duration);
    }

    private String operationOf(String sql) {
        if (sql == null || sql.isBlank()) {
            return "OTHER";
        }

        String operation = sql.strip().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        return switch (operation) {
            case "SELECT", "INSERT", "UPDATE", "DELETE" -> operation;
            default -> "OTHER";
        };
    }
}
