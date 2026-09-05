package kr.co.victoryfairy.member.application.admin;

import kr.co.victoryfairy.member.presentation.admin.AdminMemberDto;
import kr.co.victoryfairy.member.domain.MemberModel;
import kr.co.victoryfairy.member.domain.MemberQueryStore;
import kr.co.victoryfairy.configuration.MapStructConfig;
import kr.co.victoryfairy.shared.domain.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMemberQueryService {

    private final Mapper mapper;

    private final MemberQueryStore memberCustomRepository;

    public PageResult<AdminMemberDto.MemberListResponse> findList(AdminMemberDto.MemberListRequest request) {
        var result = memberCustomRepository.findAll(mapper.toRequest(request));
        return mapper.toPageResult(result);
    }

    @org.mapstruct.Mapper(config = MapStructConfig.class)
    public interface Mapper {

        MemberModel.MemberListRequest toRequest(AdminMemberDto.MemberListRequest request);

        List<AdminMemberDto.MemberListResponse> toMemberListResponse(List<MemberModel.MemberListResponse> response);

        default PageResult<AdminMemberDto.MemberListResponse> toPageResult(
                PageResult<MemberModel.MemberListResponse> pageResult) {
            var response = toMemberListResponse(pageResult.getContents());
            return new PageResult<>(response, pageResult.getTotal());
        }

    }

}
