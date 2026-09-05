package kr.co.victoryfairy.member.domain;

import java.time.LocalDateTime;

public record MemberProfile(Long id, Long memberId, Long teamId, String snsId, String email, String nickNm,
        MemberEnum.SnsType snsType, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static MemberProfile social(Long memberId, MemberEnum.SnsType snsType, String snsId, String email) {
        return new MemberProfile(null, memberId, null, snsId, email, null, snsType, null, null);
    }

    public MemberProfile withTeam(Long teamId) {
        return new MemberProfile(id, memberId, teamId, snsId, email, nickNm, snsType, createdAt, updatedAt);
    }

    public MemberProfile withNickname(String nickname) {
        return new MemberProfile(id, memberId, teamId, snsId, email, nickname, snsType, createdAt, updatedAt);
    }
}
