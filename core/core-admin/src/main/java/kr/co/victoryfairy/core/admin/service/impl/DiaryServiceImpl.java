package kr.co.victoryfairy.core.admin.service.impl;

import kr.co.victoryfairy.core.admin.domain.DiaryDomain;
import kr.co.victoryfairy.core.admin.service.DiaryService;
import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.storage.db.core.repository.DiaryCustomRepository;
import kr.co.victoryfairy.support.config.MapStructConfig;
import kr.co.victoryfairy.support.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {
    private final Mapper mapper;
    private final DiaryCustomRepository diaryCustomRepository;

    @Override
    public PageResult<DiaryDomain.DiaryListResponse> findAll(DiaryDomain.DiaryListRequest request) {
        var result = diaryCustomRepository.findAll(mapper.toRequest(request));
        return mapper.toPageResult(result);
    }

    @org.mapstruct.Mapper(config = MapStructConfig.class)
    public interface Mapper {
        DiaryModel.DiaryListRequest toRequest(DiaryDomain.DiaryListRequest request);

        List<DiaryDomain.DiaryListResponse> toDiaryListResponse(List<DiaryModel.DiaryListResponse> diaryList);
        default PageResult<DiaryDomain.DiaryListResponse> toPageResult(PageResult<DiaryModel.DiaryListResponse> pageResult) {
            var response = toDiaryListResponse(pageResult.getContents());
            return new PageResult<>(response, pageResult.getTotal());
        }
    }
}
