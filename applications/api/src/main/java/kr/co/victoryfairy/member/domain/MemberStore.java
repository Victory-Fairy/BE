package kr.co.victoryfairy.member.domain;

import java.util.List;
import java.util.Optional;

public interface MemberStore {

    Optional<Member> findMember(Long id);

    boolean memberExists(Long id);

    Member saveMember(Member member);

    Optional<MemberProfile> findProfile(MemberEnum.SnsType snsType, String snsId);

    Optional<MemberProfile> findProfileByMemberId(Long memberId);

    Optional<MemberProfile> findProfileByNickname(String nickname);

    List<MemberProfile> findProfiles(List<Long> memberIds);

    MemberProfile saveProfile(MemberProfile profile);
}
