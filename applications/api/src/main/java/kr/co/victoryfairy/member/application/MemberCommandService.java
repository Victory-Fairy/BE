package kr.co.victoryfairy.member.application;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.media.domain.FileReference;
import kr.co.victoryfairy.media.domain.FileReferenceRepository;
import kr.co.victoryfairy.media.domain.MediaFileRepository;
import kr.co.victoryfairy.game.domain.TeamReader;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberStore memberStore;

    private final TeamReader teamReader;

    private final MediaFileRepository fileRepository;

    private final FileReferenceRepository fileRefRepository;

    @Transactional
    public void updateTeam(MemberDomain.MemberTeamUpdateRequest request) {
        var id = authenticatedMemberId();
        var profile = memberStore.findProfileByMemberId(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        teamReader.findById(request.teamId())
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberStore.saveProfile(profile.withTeam(request.teamId()));
    }

    @Transactional
    public void updateMemberProfile(MemberDomain.MemberProfileUpdateRequest request) {
        var id = authenticatedMemberId();
        fileRefRepository.deactivateFirstActive(RefType.PROFILE, id);

        if (request.fileId() != null) {
            var file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
            fileRefRepository.save(FileReference.active(file, id, RefType.PROFILE));
        }
    }

    @Transactional
    public void updateMemberNickNm(MemberDomain.MemberNickNmUpdateRequest request) {
        var id = authenticatedMemberId();
        if (memberStore.findProfileByNickname(request.nickNm()).isPresent()) {
            throw new CustomException(MessageEnum.CheckNick.DUPLICATE);
        }

        var profile = memberStore.findProfileByMemberId(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberStore.saveProfile(profile.withNickname(request.nickNm()));
    }

    private Long authenticatedMemberId() {
        var id = CurrentRequest.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        return id;
    }

}
