package kr.co.victoryfairy.member.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberInfoEntity;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;

class MemberPersistenceMapperTest {

    @Test
    void memberRoundTripPreservesLoginAndShadowedAuditFields() {
        var createdAt = LocalDateTime.of(2026, 9, 5, 1, 2);
        var updatedAt = LocalDateTime.of(2026, 9, 5, 3, 4);
        var lastLogin = LocalDateTime.of(2026, 9, 5, 5, 6);
        var member = new Member(7L, MemberEnum.Status.NORMAL, "127.0.0.1", false, createdAt, updatedAt, lastLogin);

        MemberEntity entity = MemberPersistenceMapper.toEntity(member);
        Member mapped = MemberPersistenceMapper.toDomain(entity);

        assertThat(mapped).isEqualTo(member);
    }

    @Test
    void profileRoundTripPreservesNullableFieldsAndGeneratedMemberLink() {
        var profile = new MemberProfile(9L, 7L, null, "sns", null, null, MemberEnum.SnsType.APPLE, null, null);

        MemberInfoEntity entity = MemberPersistenceMapper.toEntity(profile, MemberEntity.builder().id(7L).build(), null);
        MemberProfile mapped = MemberPersistenceMapper.toDomain(entity);

        assertThat(mapped).isEqualTo(profile);
    }

    @Test
    void adapterUpdatesManagedMemberAndProfileWithoutErasingNullableAuditValues() {
        var members = mock(MemberRepository.class);
        var profiles = mock(MemberInfoRepository.class);
        var teams = mock(TeamRepository.class);
        var memberRow = MemberEntity.builder().id(7L).status(MemberEnum.Status.NORMAL).isUse(true).build();
        var profileRow = MemberInfoEntity.builder().id(9L).memberEntity(memberRow).snsId("sns").email(null)
            .snsType(MemberEnum.SnsType.APPLE).createdAt(null).build();
        when(members.findById(7L)).thenReturn(Optional.of(memberRow));
        when(members.getReferenceById(7L)).thenReturn(memberRow);
        when(members.save(memberRow)).thenReturn(memberRow);
        when(profiles.findById(9L)).thenReturn(Optional.of(profileRow));
        when(profiles.save(profileRow)).thenReturn(profileRow);
        var adapter = new MemberPersistenceAdapter(members, profiles, teams);

        var savedMember = adapter.saveMember(MemberPersistenceMapper.toDomain(memberRow).login("new-ip",
                LocalDateTime.of(2026, 9, 5, 7, 8)));
        var savedProfile = adapter.saveProfile(MemberPersistenceMapper.toDomain(profileRow).withNickname("nick"));

        assertThat(savedMember.lastConnectIp()).isEqualTo("new-ip");
        assertThat(savedProfile.nickNm()).isEqualTo("nick");
        assertThat(savedProfile.email()).isNull();
        assertThat(savedProfile.createdAt()).isNull();
    }
}
