package kr.co.victoryfairy.game.infrastructure.persistence.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.types.dsl.BooleanExpression;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GameMatchCustomRepositoryImplTest {

    @Test
    void matchesDatesWithInclusiveStartAndExclusiveEnd() {
        var repository = new GameMatchCustomRepositoryImpl(null);

        var predicate = (BooleanExpression) ReflectionTestUtils.invokeMethod(
            repository, "eqMatchAt", LocalDate.of(2026, 9, 3)
        );

        assertThat(predicate).hasToString(
            "gameMatchEntity.matchAt >= 2026-09-03T00:00 && gameMatchEntity.matchAt < 2026-09-04T00:00"
        );
    }
}
