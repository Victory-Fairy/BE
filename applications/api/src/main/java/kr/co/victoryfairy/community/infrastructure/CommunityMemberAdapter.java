package kr.co.victoryfairy.community.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.community.application.CommunityMemberReader;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberInfoEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityMemberAdapter implements CommunityMemberReader {

    private final MemberRepository members;

    private final MemberInfoRepository memberInfos;

    private final FileReferenceService fileReferences;

    @Override
    public boolean exists(Long memberId) {
        return memberId != null && members.existsById(memberId);
    }

    @Override
    public Map<Long, Author> findAuthors(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        var infoByMemberId = memberInfos.findByMemberEntity_IdIn(memberIds).stream()
            .collect(Collectors.toMap(info -> info.getMemberEntity().getId(), Function.identity()));
        var profileByMemberId = fileReferences.findImageMapByRefIds(RefType.PROFILE, memberIds);
        var result = new LinkedHashMap<Long, Author>();
        memberIds.forEach(memberId -> {
            MemberInfoEntity info = infoByMemberId.get(memberId);
            var profile = profileByMemberId.get(memberId);
            result.put(memberId, new Author(memberId, info == null ? null : info.getNickNm(),
                    profile == null ? null : profile.url()));
        });
        return result;
    }

}
