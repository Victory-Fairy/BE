package kr.co.victoryfairy.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JacksonContractTest {

    private final ObjectMapper objectMapper = new BeanConfig().objectMapper();

    @Test
    void preservesDateEnumNullAndEmptyListJsonShape() throws Exception {
        var payload = new SamplePayload(
                LocalDateTime.of(2026, 9, 1, 18, 30), Status.END, null, List.of());

        assertThat(objectMapper.writeValueAsString(payload))
            .isEqualTo("{\"occurredAt\":\"2026-09-01T18:30:00\",\"status\":\"END\",\"note\":null,\"values\":[]}");
    }

    private record SamplePayload(LocalDateTime occurredAt, Status status, String note, List<String> values) {
    }

    private enum Status {
        END
    }
}
