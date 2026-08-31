package io.dodn.springboot.core.enums;

public interface MemberEnum {

    enum Status {

        NORMAL("정상"), WITHDRAWAL("탈퇴"), CUTOFF("차단");

        private String desc;

        Status(String desc) {
            this.desc = desc;
        }

    }

    enum SnsType {

        KAKAO, GOOGLE, APPLE

    }

    enum NickStatus {

        AVAILABLE, DUPLICATE

    }

}
