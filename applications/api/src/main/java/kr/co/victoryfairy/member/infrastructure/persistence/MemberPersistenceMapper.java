package kr.co.victoryfairy.member.infrastructure.persistence;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberInfoEntity;

public final class MemberPersistenceMapper {

    private MemberPersistenceMapper() {
    }

    public static Member toDomain(MemberEntity entity) {
        return new Member(entity.getId(), entity.getStatus(), entity.getLastConnectIp(), entity.getIsUse(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getLastConnectAt());
    }

    public static MemberProfile toDomain(MemberInfoEntity entity) {
        return new MemberProfile(entity.getId(), entity.getMemberEntity().getId(),
                entity.getTeamEntity() == null ? null : entity.getTeamEntity().getId(), entity.getSnsId(),
                entity.getEmail(), entity.getNickNm(), entity.getSnsType(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static MemberEntity toEntity(Member member) {
        return MemberEntity.builder().id(member.id()).status(member.status()).lastConnectIp(member.lastConnectIp())
            .isUse(member.isUse()).createdAt(member.createdAt()).updatedAt(member.updatedAt())
            .lastConnectAt(member.lastConnectAt()).build();
    }

    public static MemberInfoEntity toEntity(MemberProfile profile, MemberEntity member, TeamEntity team) {
        return MemberInfoEntity.builder().id(profile.id()).memberEntity(member).teamEntity(team).snsId(profile.snsId())
            .email(profile.email()).nickNm(profile.nickNm()).snsType(profile.snsType()).createdAt(profile.createdAt())
            .updatedAt(profile.updatedAt()).build();
    }
}
