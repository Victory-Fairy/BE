package kr.co.victoryfairy.core.admin.service.impl;

import kr.co.victoryfairy.core.admin.domain.MemberDomain;
import kr.co.victoryfairy.core.admin.service.MemberService;
import kr.co.victoryfairy.storage.db.core.model.MemberModel;
import kr.co.victoryfairy.storage.db.core.repository.MemberCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.support.config.MapStructConfig;
import kr.co.victoryfairy.support.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final Mapper mapper;

    private final MemberRepository memberRepository;
    private final MemberCustomRepository memberCustomRepository;

    @Override
    public PageResult<MemberDomain.MemberListResponse> findList(MemberDomain.MemberListRequest request) {
        var result = memberCustomRepository.findAll(mapper.toRequest(request));
        return mapper.toPageResult(result);
    }

    @org.mapstruct.Mapper(config = MapStructConfig.class)
    public interface Mapper {
        MemberModel.MemberListRequest toRequest(MemberDomain.MemberListRequest request);

        List<MemberDomain.MemberListResponse> toMemberListResponse(List<MemberModel.MemberListResponse> response);
        default PageResult<MemberDomain.MemberListResponse> toPageResult(PageResult<MemberModel.MemberListResponse> pageResult) {
            var response = toMemberListResponse(pageResult.getContents());
            return new PageResult<>(response, pageResult.getTotal());
        }
    }
}
