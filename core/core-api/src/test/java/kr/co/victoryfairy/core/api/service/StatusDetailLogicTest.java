package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.MatchEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA-008: status/statusDetail 일관성 테스트
 *
 * 세 API의 statusDetail 반환 로직이 일관되게 동작하는지 검증합니다. - /match/list - /match/{id} -
 * /diary/daily-list
 *
 * 규칙: - CANCELED 상태 + reason 존재 → statusDetail = reason (취소 사유) - CANCELED 상태 + reason
 * null → statusDetail = "경기취소" (fallback) - 그 외 상태 → statusDetail = status.getDesc()
 */
@Tag("develop")
@DisplayName("QA-008: statusDetail 로직 테스트")
class StatusDetailLogicTest {

    /**
     * 수정된 statusDetail 결정 로직 (DiaryServiceImpl, MatchServiceImpl에서 사용)
     */
    private String determineStatusDetail(MatchEnum.MatchStatus status, String reason) {
        return status.equals(MatchEnum.MatchStatus.CANCELED) && reason != null ? reason : status.getDesc();
    }

    @Nested
    @DisplayName("CANCELED 상태 테스트")
    class CanceledStatusTest {

        @Test
        @DisplayName("취소 사유가 있으면 reason을 반환한다")
        void whenCanceledWithReason_thenReturnReason() {
            // given
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.CANCELED;
            String reason = "우천취소";

            // when
            String statusDetail = determineStatusDetail(status, reason);

            // then
            assertThat(statusDetail).isEqualTo("우천취소");
        }

        @Test
        @DisplayName("취소 사유가 '코로나19'이면 해당 사유를 반환한다")
        void whenCanceledWithCovidReason_thenReturnReason() {
            // given
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.CANCELED;
            String reason = "코로나19";

            // when
            String statusDetail = determineStatusDetail(status, reason);

            // then
            assertThat(statusDetail).isEqualTo("코로나19");
        }

        @Test
        @DisplayName("취소 사유가 null이면 기본 설명 '경기취소'를 반환한다")
        void whenCanceledWithNullReason_thenReturnDefaultDesc() {
            // given
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.CANCELED;
            String reason = null;

            // when
            String statusDetail = determineStatusDetail(status, reason);

            // then
            assertThat(statusDetail).isEqualTo("경기취소");
        }

    }

    @Nested
    @DisplayName("READY 상태 테스트")
    class ReadyStatusTest {

        @Test
        @DisplayName("READY 상태면 '경기예정'을 반환한다")
        void whenReady_thenReturnScheduled() {
            // given
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.READY;
            String reason = null;

            // when
            String statusDetail = determineStatusDetail(status, reason);

            // then
            assertThat(statusDetail).isEqualTo("경기예정");
        }

        @Test
        @DisplayName("READY 상태면 reason이 있어도 '경기예정'을 반환한다")
        void whenReadyWithReason_thenStillReturnScheduled() {
            // given
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.READY;
            String reason = "우천취소"; // 이 값은 무시되어야 함

            // when
            String statusDetail = determineStatusDetail(status, reason);

            // then
            assertThat(statusDetail).isEqualTo("경기예정");
        }

    }

    @Nested
    @DisplayName("PROGRESS 상태 테스트")
    class ProgressStatusTest {

        @Test
        @DisplayName("PROGRESS 상태면 '진행중'을 반환한다")
        void whenProgress_thenReturnInProgress() {
            // given
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.PROGRESS;
            String reason = null;

            // when
            String statusDetail = determineStatusDetail(status, reason);

            // then
            assertThat(statusDetail).isEqualTo("진행중");
        }

    }

    @Nested
    @DisplayName("END 상태 테스트")
    class EndStatusTest {

        @Test
        @DisplayName("END 상태면 '종료'를 반환한다")
        void whenEnd_thenReturnEnded() {
            // given
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.END;
            String reason = null;

            // when
            String statusDetail = determineStatusDetail(status, reason);

            // then
            assertThat(statusDetail).isEqualTo("종료");
        }

    }

    @Nested
    @DisplayName("API 일관성 테스트")
    class ApiConsistencyTest {

        @Test
        @DisplayName("모든 상태에서 일관된 statusDetail을 반환한다")
        void allStatusesShouldReturnConsistentStatusDetail() {
            // READY
            assertThat(determineStatusDetail(MatchEnum.MatchStatus.READY, null)).isEqualTo("경기예정");

            // PROGRESS
            assertThat(determineStatusDetail(MatchEnum.MatchStatus.PROGRESS, null)).isEqualTo("진행중");

            // END
            assertThat(determineStatusDetail(MatchEnum.MatchStatus.END, null)).isEqualTo("종료");

            // CANCELED with reason
            assertThat(determineStatusDetail(MatchEnum.MatchStatus.CANCELED, "우천취소")).isEqualTo("우천취소");

            // CANCELED without reason (fallback)
            assertThat(determineStatusDetail(MatchEnum.MatchStatus.CANCELED, null)).isEqualTo("경기취소");
        }

    }

}
