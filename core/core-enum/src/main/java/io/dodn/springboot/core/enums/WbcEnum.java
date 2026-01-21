package io.dodn.springboot.core.enums;

import java.util.Arrays;

public interface WbcEnum {

	/**
	 * WBC 참가국 Enum
	 * - MLB Stats API /api/v1/teams?sportId=51 기반
	 * - 2023 WBC 참가 20개국
	 */
	enum Country {
		// Pool A
		TPE("TPE", "Chinese Taipei", "대만", 791),
		NED("NED", "Kingdom of the Netherlands", "네덜란드", 878),
		CUB("CUB", "Cuba", "쿠바", 798),
		ITA("ITA", "Italy", "이탈리아", 811),
		PAN("PAN", "Panama", "파나마", 890),

		// Pool B
		JPN("JPN", "Japan", "일본", 843),
		KOR("KOR", "Korea", "대한민국", 1171),
		AUS("AUS", "Australia", "호주", 829),
		CZE("CZE", "Czech Republic", "체코", 799),
		CHN("CHN", "China", "중국", 790),

		// Pool C
		USA("USA", "United States", "미국", 940),
		MEX("MEX", "Mexico", "멕시코", 867),
		COL("COL", "Colombia", "콜롬비아", 793),
		CAN("CAN", "Canada", "캐나다", 789),
		GBR("GBR", "Great Britain", "영국", 807),

		// Pool D
		PRI("PRI", "Puerto Rico", "푸에르토리코", 917),
		VEN("VEN", "Venezuela", "베네수엘라", 969),
		DOM("DOM", "Dominican Republic", "도미니카공화국", 800),
		NCA("NCA", "Nicaragua", "니카라과", 884),
		ISR("ISR", "Israel", "이스라엘", 812);

		private final String code;

		private final String name;

		private final String koreanName;

		private final int mlbTeamId;

		Country(String code, String name, String koreanName, int mlbTeamId) {
			this.code = code;
			this.name = name;
			this.koreanName = koreanName;
			this.mlbTeamId = mlbTeamId;
		}

		public String getCode() {
			return code;
		}

		public String getName() {
			return name;
		}

		public String getKoreanName() {
			return koreanName;
		}

		public int getMlbTeamId() {
			return mlbTeamId;
		}

		public static Country fromCode(String code) {
			return Arrays.stream(values())
				.filter(c -> c.code.equalsIgnoreCase(code))
				.findFirst()
				.orElse(null);
		}

		public static Country fromMlbTeamId(int mlbTeamId) {
			return Arrays.stream(values())
				.filter(c -> c.mlbTeamId == mlbTeamId)
				.findFirst()
				.orElse(null);
		}

		public static Country fromName(String name) {
			return Arrays.stream(values())
				.filter(c -> c.name.equalsIgnoreCase(name)
						|| c.name.toLowerCase().contains(name.toLowerCase()))
				.findFirst()
				.orElse(null);
		}

	}

	enum SeriesType {

		POOL_A("PA", "Pool A"), POOL_B("PB", "Pool B"), POOL_C("PC", "Pool C"), POOL_D("PD", "Pool D"),
		QUARTERFINAL("QF", "8강"), SEMIFINAL("SF", "4강"), CHAMPIONSHIP("CH", "결승");

		private final String code;

		private final String desc;

		SeriesType(String code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public String getCode() {
			return code;
		}

		public String getDesc() {
			return desc;
		}

	}

}
