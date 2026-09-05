package kr.co.victoryfairy.diary.application.admin;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.presentation.admin.AdminDiaryDto;
import kr.co.victoryfairy.diary.domain.DiaryModel;
import kr.co.victoryfairy.diary.domain.SeatUseStore;
import kr.co.victoryfairy.diary.application.DiaryFoodDomainService;
import kr.co.victoryfairy.diary.application.PartnerDomainService;
import kr.co.victoryfairy.configuration.MapStructConfig;
import kr.co.victoryfairy.shared.domain.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDiaryQueryService {

    private final Mapper mapper;

    private final AdminDiaryQueryStore diaryCustomRepository;

    private final DiaryFoodDomainService diaryFoods;

    private final PartnerDomainService partners;

    private final SeatUseStore seatUses;

    public PageResult<AdminDiaryDto.DiaryListResponse> findAll(AdminDiaryDto.DiaryListRequest request) {
        var result = diaryCustomRepository.findAll(mapper.toRequest(request));

        var diaryIds = result.getContents().stream().map(DiaryModel.DiaryListResponse::getId).toList();

        var diaryFoods = this.diaryFoods.findFoodMapByRefIds(RefType.DIARY, diaryIds);
        var partnerNames = partners.findPartnerNameMapByRefIds(RefType.DIARY, diaryIds);
        var seatUseHistories = seatUses.findDescriptions(diaryIds);

        result.getContents().forEach(diary -> {
            var diaryId = diary.getId();

            diary.setFoods(diaryFoods.getOrDefault(diaryId, List.of()));
            diary.setPartners(partnerNames.getOrDefault(diaryId, List.of()));
            diary.setSeatUseHistories(seatUseHistories.getOrDefault(diaryId, List.of()));
        });

        return mapper.toPageResult(result);
    }

    @org.mapstruct.Mapper(config = MapStructConfig.class)
    public interface Mapper {

        DiaryModel.DiaryListRequest toRequest(AdminDiaryDto.DiaryListRequest request);

        List<AdminDiaryDto.DiaryListResponse> toDiaryListResponse(List<DiaryModel.DiaryListResponse> diaryList);

        default PageResult<AdminDiaryDto.DiaryListResponse> toPageResult(
                PageResult<DiaryModel.DiaryListResponse> pageResult) {
            var response = toDiaryListResponse(pageResult.getContents());
            return new PageResult<>(response, pageResult.getTotal());
        }

    }

}
