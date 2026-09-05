package kr.co.victoryfairy.member.domain;

import java.time.LocalDateTime;

public record Member(Long id, MemberEnum.Status status, String lastConnectIp, Boolean isUse, LocalDateTime createdAt,
        LocalDateTime updatedAt, LocalDateTime lastConnectAt) {

    public static Member normal(String ip, LocalDateTime connectedAt) {
        return new Member(null, MemberEnum.Status.NORMAL, ip, true, null, null, connectedAt);
    }

    public Member login(String ip, LocalDateTime connectedAt) {
        return new Member(id, status, ip, isUse, createdAt, updatedAt, connectedAt);
    }
}
