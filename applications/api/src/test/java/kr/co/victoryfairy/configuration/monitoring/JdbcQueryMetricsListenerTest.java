package kr.co.victoryfairy.configuration.monitoring;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcQueryMetricsListenerTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final JdbcQueryMetricsListener listener = new JdbcQueryMetricsListener(
            new JdbcQueryMetricsRecorder(meterRegistry));

    @Test
    void recordsOneMetricForEachExecutedQuery() {
        ExecutionInfo executionInfo = mock(ExecutionInfo.class);
        QueryInfo select = mock(QueryInfo.class);
        QueryInfo update = mock(QueryInfo.class);
        when(executionInfo.getElapsedTime()).thenReturn(80L);
        when(select.getQuery()).thenReturn("SELECT * FROM member");
        when(update.getQuery()).thenReturn("UPDATE member SET nick_nm = ?");

        listener.afterQuery(executionInfo, List.of(select, update));

        assertThat(timer("SELECT", "success").count()).isOne();
        assertThat(timer("UPDATE", "success").count()).isOne();
        assertThat(timer("SELECT", "success").max(TimeUnit.MILLISECONDS)).isEqualTo(80.0);
    }

    @Test
    void marksQueryAsErrorWhenJdbcExecutionFails() {
        ExecutionInfo executionInfo = mock(ExecutionInfo.class);
        QueryInfo query = mock(QueryInfo.class);
        when(executionInfo.getElapsedTime()).thenReturn(15L);
        when(executionInfo.getThrowable()).thenReturn(new IllegalStateException("database unavailable"));
        when(query.getQuery()).thenReturn("DELETE FROM diary WHERE id = ?");

        listener.afterQuery(executionInfo, List.of(query));

        assertThat(timer("DELETE", "error").count()).isOne();
    }

    private Timer timer(String operation, String outcome) {
        return meterRegistry.find("victoryfairy.database.query")
                .tags("operation", operation, "outcome", outcome)
                .timer();
    }
}
