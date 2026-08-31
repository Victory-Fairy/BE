package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import kr.co.victoryfairy.storage.db.core.repository.FileRefRepository;
import kr.co.victoryfairy.storage.db.core.repository.FileRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberInfoRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberRepository memberRepository;

    private final MemberInfoRepository memberInfoRepository;

    private final TeamRepository teamRepository;

    private final FileRepository fileRepository;

    private final FileRefRepository fileRefRepository;

    @Transactional
    public void updateTeam(MemberDomain.MemberTeamUpdateRequest request) {
        var id = authenticatedMemberId();
        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var teamEntity = teamRepository.findById(request.teamId())
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoRepository.save(memberInfoEntity.toBuilder().teamEntity(teamEntity).build());
    }

    @Transactional
    public void updateMemberProfile(MemberDomain.MemberProfileUpdateRequest request) {
        var id = authenticatedMemberId();
        var fileRefEntity = fileRefRepository.findByRefTypeAndRefIdAndIsUseTrue(RefType.PROFILE, id).orElse(null);
        if (fileRefEntity != null) {
            fileRefEntity.delete();
        }

        if (request.fileId() != null) {
            var fileEntity = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
            fileRefRepository.save(FileRefEntity.builder()
                .fileEntity(fileEntity)
                .refId(id)
                .refType(RefType.PROFILE)
                .build());
        }
    }

    @Transactional
    public void updateMemberNickNm(MemberDomain.MemberNickNmUpdateRequest request) {
        var id = authenticatedMemberId();
        if (memberInfoRepository.findByNickNm(request.nickNm()).isPresent()) {
            throw new CustomException(MessageEnum.CheckNick.DUPLICATE);
        }

        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoRepository.save(memberInfoEntity.toBuilder().nickNm(request.nickNm()).build());
    }

    private Long authenticatedMemberId() {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        return id;
    }

}
