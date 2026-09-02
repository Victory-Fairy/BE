package kr.co.victoryfairy.configuration.monitoring;

import java.time.Duration;
import java.util.List;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JdbcQueryMetricsListener implements QueryExecutionListener {

    private final JdbcQueryMetricsRecorder recorder;

    public JdbcQueryMetricsListener(JdbcQueryMetricsRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
        // Metrics are emitted only after JDBC execution finishes with its final duration and outcome.
    }

    @Override
    public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
        Duration duration = Duration.ofMillis(executionInfo.getElapsedTime());
        boolean failed = executionInfo.getThrowable() != null;

        queryInfoList.forEach(queryInfo -> recorder.record(queryInfo.getQuery(), failed, duration));
    }
}
