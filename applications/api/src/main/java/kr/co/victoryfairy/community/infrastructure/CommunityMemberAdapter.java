package kr.co.victoryfairy.community.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.community.application.CommunityMemberReader;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.domain.MemberStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityMemberAdapter implements CommunityMemberReader {

    private final MemberStore members;

    private final FileReferenceService fileReferences;

    @Override
    public boolean exists(Long memberId) {
        return memberId != null && members.memberExists(memberId);
    }

    @Override
    public Map<Long, Author> findAuthors(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        var infoByMemberId = members.findProfiles(memberIds).stream()
            .collect(Collectors.toMap(MemberProfile::memberId, info -> info));
        var profileByMemberId = fileReferences.findImageMapByRefIds(RefType.PROFILE, memberIds);
        var result = new LinkedHashMap<Long, Author>();
        memberIds.forEach(memberId -> {
            MemberProfile info = infoByMemberId.get(memberId);
            var profile = profileByMemberId.get(memberId);
            result.put(memberId, new Author(memberId, info == null ? null : info.nickNm(),
                    profile == null ? null : profile.url()));
        });
        return result;
    }

}
