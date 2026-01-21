package io.dodn.springboot.core.enums;

public interface MatchEnum {

    enum MatchType {
        EXHIBITION("시범경기", "1"),
        REGULAR("정규시즌", "0,9,6"),
        POST("포스트시즌", "3,4,5,7"),
        TIEBREAKER("타이브레이커", "6")
        ;

        private String desc;
        private String value;

        MatchType(String desc, String value) {
            this.desc = desc;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }
    }

    enum SeriesType {
        EXHIBITION("시범경기", "1"),
        REGULAR("정규시즌", "0"),
        WILDCARD("와일드카드", "4"),
        TIEBREAKER("정규시즌", "6"),
        SEMI_PLAYOFF("준플레이오프", "3"),
        PLAYOFF("플레이오프", "5"),
        KOREA("한국시리즈", "7")
        ;

        private String desc;
        private String value;

        SeriesType(String desc, String value) {
            this.desc = desc;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }
    }

    enum MatchStatus {
        READY("경기예정"),
        PROGRESS("진행중"),
        END("종료"),
        CANCELED("경기취소")
        ;

        private String desc;

        MatchStatus(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }

    enum ResultType {
        WIN("승"),
        LOSS("패"),
        DRAW("무"),
        CANCEL("취소")
        ;

        private String desc;

        ResultType(String desc) { this.desc = desc; }

        public String getDesc() {
            return desc;
        }
    }

    enum LeagueType {
        KBO("KBO", "한국프로야구"),
        WBC("WBC", "월드베이스볼클래식"),
        MLB("MLB", "메이저리그")
        ;

        private final String code;
        private final String desc;

        LeagueType(String code, String desc) {
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
