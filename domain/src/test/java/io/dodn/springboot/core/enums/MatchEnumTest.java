package io.dodn.springboot.core.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("develop")
@DisplayName("MatchEnum 테스트")
class MatchEnumTest {

    @Nested
    @DisplayName("LeagueType Enum 테스트")
    class LeagueTypeTest {

        @Test
        @DisplayName("KBO 리그 타입이 정상적으로 생성된다")
        void kboLeagueType_shouldHaveCorrectValues() {
            // given
            var kbo = MatchEnum.LeagueType.KBO;

            // when & then
            assertThat(kbo.getCode()).isEqualTo("KBO");
            assertThat(kbo.getDesc()).isEqualTo("한국프로야구");
        }

        @Test
        @DisplayName("WBC 리그 타입이 정상적으로 생성된다")
        void wbcLeagueType_shouldHaveCorrectValues() {
            // given
            var wbc = MatchEnum.LeagueType.WBC;

            // when & then
            assertThat(wbc.getCode()).isEqualTo("WBC");
            assertThat(wbc.getDesc()).isEqualTo("월드베이스볼클래식");
        }

        @Test
        @DisplayName("MLB 리그 타입이 정상적으로 생성된다")
        void mlbLeagueType_shouldHaveCorrectValues() {
            // given
            var mlb = MatchEnum.LeagueType.MLB;

            // when & then
            assertThat(mlb.getCode()).isEqualTo("MLB");
            assertThat(mlb.getDesc()).isEqualTo("메이저리그");
        }

        @Test
        @DisplayName("LeagueType은 3개의 값을 가진다")
        void leagueType_shouldHaveThreeValues() {
            // when
            var values = MatchEnum.LeagueType.values();

            // then
            assertThat(values).hasSize(3);
            assertThat(values).containsExactly(MatchEnum.LeagueType.KBO, MatchEnum.LeagueType.WBC,
                    MatchEnum.LeagueType.MLB);
        }

        @Test
        @DisplayName("문자열로 LeagueType을 찾을 수 있다")
        void leagueType_shouldBeFoundByName() {
            // when
            var kbo = MatchEnum.LeagueType.valueOf("KBO");
            var wbc = MatchEnum.LeagueType.valueOf("WBC");
            var mlb = MatchEnum.LeagueType.valueOf("MLB");

            // then
            assertThat(kbo).isEqualTo(MatchEnum.LeagueType.KBO);
            assertThat(wbc).isEqualTo(MatchEnum.LeagueType.WBC);
            assertThat(mlb).isEqualTo(MatchEnum.LeagueType.MLB);
        }

    }

}
