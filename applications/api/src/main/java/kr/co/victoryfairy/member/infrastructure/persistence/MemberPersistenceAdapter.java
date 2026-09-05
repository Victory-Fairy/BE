package kr.co.victoryfairy.member.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MemberPersistenceAdapter implements MemberStore {

    private final MemberRepository members;
    private final MemberInfoRepository profiles;
    private final TeamRepository teams;

    public MemberPersistenceAdapter(MemberRepository members, MemberInfoRepository profiles, TeamRepository teams) {
        this.members = members;
        this.profiles = profiles;
        this.teams = teams;
    }

    public Optional<Member> findMember(Long id) {
        return members.findById(id).map(MemberPersistenceMapper::toDomain);
    }

    public boolean memberExists(Long id) {
        return members.existsById(id);
    }

    public Member saveMember(Member member) {
        var source = MemberPersistenceMapper.toEntity(member);
        var entity = member.id() == null ? source : members.findById(member.id()).map(existing -> {
            existing.updateFrom(source);
            return existing;
        }).orElse(source);
        return MemberPersistenceMapper.toDomain(members.save(entity));
    }

    public Optional<MemberProfile> findProfile(MemberEnum.SnsType snsType, String snsId) {
        return profiles.findBySnsTypeAndSnsId(snsType, snsId).map(MemberPersistenceMapper::toDomain);
    }

    public Optional<MemberProfile> findProfileByMemberId(Long memberId) {
        return profiles.findByMemberEntity_Id(memberId).map(MemberPersistenceMapper::toDomain);
    }

    public Optional<MemberProfile> findProfileByNickname(String nickname) {
        return profiles.findByNickNm(nickname).map(MemberPersistenceMapper::toDomain);
    }

    public List<MemberProfile> findProfiles(List<Long> memberIds) {
        return profiles.findByMemberEntity_IdIn(memberIds).stream().map(MemberPersistenceMapper::toDomain).toList();
    }

    public MemberProfile saveProfile(MemberProfile profile) {
        var member = members.getReferenceById(profile.memberId());
        var team = profile.teamId() == null ? null : teams.getReferenceById(profile.teamId());
        var source = MemberPersistenceMapper.toEntity(profile, member, team);
        var entity = profile.id() == null ? source : profiles.findById(profile.id()).map(existing -> {
            existing.updateFrom(source);
            return existing;
        }).orElse(source);
        return MemberPersistenceMapper.toDomain(profiles.save(entity));
    }
}
