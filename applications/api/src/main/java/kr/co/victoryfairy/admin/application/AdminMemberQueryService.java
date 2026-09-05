package kr.co.victoryfairy.admin.application;

import kr.co.victoryfairy.admin.presentation.AdminMemberDto;
import kr.co.victoryfairy.member.infrastructure.persistence.model.MemberModel;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberCustomRepository;
import kr.co.victoryfairy.configuration.MapStructConfig;
import kr.co.victoryfairy.shared.infrastructure.persistence.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMemberQueryService {

    private final Mapper mapper;

    private final MemberCustomRepository memberCustomRepository;

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
