package io.dodn.springboot.core.enums;

public interface TeamEnum {

    enum KboTeamNm {

        HT("KIA"), SS("삼성"), LG("LG"), OB("두산"), KT("KT"), SK("SSG"), LT("롯데"), HH("한화"), NC("NC"), WO("키움"),;

        private String desc;

        KboTeamNm(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }

        public static KboTeamNm fromDesc(String desc) {
            for (KboTeamNm team : values()) {
                if (team.getDesc().equals(desc)) {
                    return team;
                }
            }
            return null;
        }

    }

}
