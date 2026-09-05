package kr.co.victoryfairy.member.application;

import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import kr.co.victoryfairy.member.domain.MemberQueryStore;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import kr.co.victoryfairy.member.presentation.MyPageDomain;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageQueryService {
    private final MemberQueryStore memberCustomRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    public MyPageDomain.MemberInfoForMyPageResponse findMemberInfoForMyPage() {
        var id = CurrentRequest.getId();
        if (id == null) return new MyPageDomain.MemberInfoForMyPageResponse(null, null, null, null, null);
        var member = memberCustomRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var team = member.getTeamId() == null ? null
                : new MyPageDomain.TeamDto(member.getTeamId(), member.getTeamName(), member.getSponsorNm());
        var image = member.getFileId() == null ? null : new MyPageDomain.ImageDto(member.getFileId(), member.getPath(),
                member.getSaveName(), member.getExt(), s3PresignedUrlService.create(member.getPath(),
                        member.getSaveName(), member.getExt()));
        return new MyPageDomain.MemberInfoForMyPageResponse(member.getId(), image, member.getNickNm(),
                member.getSnsType(), team);
    }
}
