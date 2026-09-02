package kr.co.victoryfairy.configuration.monitoring;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcQueryMetricsRecorderTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final JdbcQueryMetricsRecorder recorder = new JdbcQueryMetricsRecorder(meterRegistry);

    @Test
    void recordsSuccessfulSelectQueryDurationWithoutSqlText() {
        recorder.record("  select * from member where id = ?", false, Duration.ofMillis(125));

        Timer timer = meterRegistry.find("victoryfairy.database.query")
                .tags("operation", "SELECT", "outcome", "success")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isOne();
        assertThat(timer.max(TimeUnit.MILLISECONDS)).isEqualTo(125.0);
        assertThat(timer.getId().getTags()).noneMatch(tag -> tag.getKey().equals("query"));
    }

    @Test
    void recordsFailedUnknownQuerySeparately() {
        recorder.record("CALL refresh_statistics()", true, Duration.ofMillis(20));

        Timer timer = meterRegistry.find("victoryfairy.database.query")
                .tags("operation", "OTHER", "outcome", "error")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isOne();
    }
}
