package io.dodn.springboot.core.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("develop")
@DisplayName("WbcEnum 테스트")
class WbcEnumTest {

	@Nested
	@DisplayName("Country Enum 테스트")
	class CountryTest {

		@Test
		@DisplayName("대한민국(KOR) 국가 정보가 정상적으로 설정되어 있다")
		void korea_shouldHaveCorrectValues() {
			// given
			var korea = WbcEnum.Country.KOR;

			// when & then
			assertThat(korea.getCode()).isEqualTo("KOR");
			assertThat(korea.getName()).isEqualTo("Korea");
			assertThat(korea.getKoreanName()).isEqualTo("대한민국");
			assertThat(korea.getMlbTeamId()).isEqualTo(1171);
		}

		@Test
		@DisplayName("일본(JPN) 국가 정보가 정상적으로 설정되어 있다")
		void japan_shouldHaveCorrectValues() {
			// given
			var japan = WbcEnum.Country.JPN;

			// when & then
			assertThat(japan.getCode()).isEqualTo("JPN");
			assertThat(japan.getName()).isEqualTo("Japan");
			assertThat(japan.getKoreanName()).isEqualTo("일본");
			assertThat(japan.getMlbTeamId()).isEqualTo(843);
		}

		@Test
		@DisplayName("미국(USA) 국가 정보가 정상적으로 설정되어 있다")
		void usa_shouldHaveCorrectValues() {
			// given
			var usa = WbcEnum.Country.USA;

			// when & then
			assertThat(usa.getCode()).isEqualTo("USA");
			assertThat(usa.getName()).isEqualTo("United States");
			assertThat(usa.getKoreanName()).isEqualTo("미국");
			assertThat(usa.getMlbTeamId()).isEqualTo(940);
		}

		@Test
		@DisplayName("WBC 참가국은 20개국이다")
		void country_shouldHaveTwentyCountries() {
			// when
			var values = WbcEnum.Country.values();

			// then
			assertThat(values).hasSize(20);
		}

		@ParameterizedTest
		@DisplayName("국가 코드로 Country를 찾을 수 있다")
		@CsvSource({ "KOR, 대한민국", "JPN, 일본", "USA, 미국", "TPE, 대만", "NED, 네덜란드" })
		void fromCode_shouldReturnCorrectCountry(String code, String expectedKoreanName) {
			// when
			var country = WbcEnum.Country.fromCode(code);

			// then
			assertThat(country).isNotNull();
			assertThat(country.getKoreanName()).isEqualTo(expectedKoreanName);
		}

		@Test
		@DisplayName("존재하지 않는 국가 코드로 조회하면 null을 반환한다")
		void fromCode_withInvalidCode_shouldReturnNull() {
			// when
			var country = WbcEnum.Country.fromCode("INVALID");

			// then
			assertThat(country).isNull();
		}

		@ParameterizedTest
		@DisplayName("MLB Team ID로 Country를 찾을 수 있다")
		@CsvSource({ "1171, KOR", "843, JPN", "940, USA", "791, TPE", "878, NED" })
		void fromMlbTeamId_shouldReturnCorrectCountry(int mlbTeamId, String expectedCode) {
			// when
			var country = WbcEnum.Country.fromMlbTeamId(mlbTeamId);

			// then
			assertThat(country).isNotNull();
			assertThat(country.getCode()).isEqualTo(expectedCode);
		}

		@Test
		@DisplayName("존재하지 않는 MLB Team ID로 조회하면 null을 반환한다")
		void fromMlbTeamId_withInvalidId_shouldReturnNull() {
			// when
			var country = WbcEnum.Country.fromMlbTeamId(99999);

			// then
			assertThat(country).isNull();
		}

		@ParameterizedTest
		@DisplayName("국가명으로 Country를 찾을 수 있다")
		@CsvSource({ "Korea, KOR", "Japan, JPN", "United States, USA" })
		void fromName_shouldReturnCorrectCountry(String name, String expectedCode) {
			// when
			var country = WbcEnum.Country.fromName(name);

			// then
			assertThat(country).isNotNull();
			assertThat(country.getCode()).isEqualTo(expectedCode);
		}

		@Test
		@DisplayName("부분 국가명으로도 Country를 찾을 수 있다")
		void fromName_withPartialName_shouldReturnCorrectCountry() {
			// when
			var country = WbcEnum.Country.fromName("Netherlands");

			// then
			assertThat(country).isNotNull();
			assertThat(country.getCode()).isEqualTo("NED");
		}

		@Test
		@DisplayName("존재하지 않는 국가명으로 조회하면 null을 반환한다")
		void fromName_withInvalidName_shouldReturnNull() {
			// when
			var country = WbcEnum.Country.fromName("InvalidCountry");

			// then
			assertThat(country).isNull();
		}

		@Test
		@DisplayName("Pool A 국가들이 정상적으로 포함되어 있다")
		void poolA_countriesShouldBeIncluded() {
			// Pool A: TPE, NED, CUB, ITA, PAN
			assertThat(WbcEnum.Country.TPE).isNotNull();
			assertThat(WbcEnum.Country.NED).isNotNull();
			assertThat(WbcEnum.Country.CUB).isNotNull();
			assertThat(WbcEnum.Country.ITA).isNotNull();
			assertThat(WbcEnum.Country.PAN).isNotNull();
		}

		@Test
		@DisplayName("Pool B 국가들이 정상적으로 포함되어 있다")
		void poolB_countriesShouldBeIncluded() {
			// Pool B: JPN, KOR, AUS, CZE, CHN
			assertThat(WbcEnum.Country.JPN).isNotNull();
			assertThat(WbcEnum.Country.KOR).isNotNull();
			assertThat(WbcEnum.Country.AUS).isNotNull();
			assertThat(WbcEnum.Country.CZE).isNotNull();
			assertThat(WbcEnum.Country.CHN).isNotNull();
		}

		@Test
		@DisplayName("Pool C 국가들이 정상적으로 포함되어 있다")
		void poolC_countriesShouldBeIncluded() {
			// Pool C: USA, MEX, COL, CAN, GBR
			assertThat(WbcEnum.Country.USA).isNotNull();
			assertThat(WbcEnum.Country.MEX).isNotNull();
			assertThat(WbcEnum.Country.COL).isNotNull();
			assertThat(WbcEnum.Country.CAN).isNotNull();
			assertThat(WbcEnum.Country.GBR).isNotNull();
		}

		@Test
		@DisplayName("Pool D 국가들이 정상적으로 포함되어 있다")
		void poolD_countriesShouldBeIncluded() {
			// Pool D: PRI, VEN, DOM, NCA, ISR
			assertThat(WbcEnum.Country.PRI).isNotNull();
			assertThat(WbcEnum.Country.VEN).isNotNull();
			assertThat(WbcEnum.Country.DOM).isNotNull();
			assertThat(WbcEnum.Country.NCA).isNotNull();
			assertThat(WbcEnum.Country.ISR).isNotNull();
		}

	}

	@Nested
	@DisplayName("SeriesType Enum 테스트")
	class SeriesTypeTest {

		@Test
		@DisplayName("WBC SeriesType은 7개의 값을 가진다")
		void seriesType_shouldHaveSevenValues() {
			// when
			var values = WbcEnum.SeriesType.values();

			// then
			assertThat(values).hasSize(7);
		}

		@Test
		@DisplayName("Pool A가 정상적으로 설정되어 있다")
		void poolA_shouldHaveCorrectValues() {
			// given
			var poolA = WbcEnum.SeriesType.POOL_A;

			// when & then
			assertThat(poolA.getCode()).isEqualTo("PA");
			assertThat(poolA.getDesc()).isEqualTo("Pool A");
		}

		@Test
		@DisplayName("결승(CHAMPIONSHIP)이 정상적으로 설정되어 있다")
		void championship_shouldHaveCorrectValues() {
			// given
			var championship = WbcEnum.SeriesType.CHAMPIONSHIP;

			// when & then
			assertThat(championship.getCode()).isEqualTo("CH");
			assertThat(championship.getDesc()).isEqualTo("결승");
		}

	}

}
