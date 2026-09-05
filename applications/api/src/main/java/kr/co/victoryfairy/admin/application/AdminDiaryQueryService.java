package kr.co.victoryfairy.admin.application;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.admin.presentation.AdminDiaryDto;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryFoodEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.PartnerEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.model.DiaryModel;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryCustomRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryFoodRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.PartnerRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.configuration.MapStructConfig;
import kr.co.victoryfairy.shared.infrastructure.persistence.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class AdminDiaryQueryService {

    private final Mapper mapper;

    private final DiaryCustomRepository diaryCustomRepository;

    private final DiaryFoodRepository diaryFoodRepository;

    private final PartnerRepository partnerRepository;

    private final SeatUseHistoryRepository seatUseHistoryRepository;

    public PageResult<AdminDiaryDto.DiaryListResponse> findAll(AdminDiaryDto.DiaryListRequest request) {
        var result = diaryCustomRepository.findAll(mapper.toRequest(request));

        var diaryIds = result.getContents().stream().map(DiaryModel.DiaryListResponse::getId).toList();

        var diaryFoods = diaryFoodRepository.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds)
            .stream()
            .collect(groupingBy(DiaryFoodEntity::getRefId, mapping(DiaryFoodEntity::getFoodName, toList())));

        var partners = partnerRepository.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds)
            .stream()
            .collect(groupingBy(PartnerEntity::getRefId, mapping(PartnerEntity::getName, toList())));

        var seatUseHistories = seatUseHistoryRepository.findAllByDiaryEntityIdIn(diaryIds)
            .stream()
            .filter(entity -> entity.getSeatEntity() != null)
            .collect(groupingBy(entity -> entity.getDiaryEntity().getId(),
                    mapping(entity -> entity.getSeatEntity().getName() + " " + entity.getSeatName(), toList())));

        result.getContents().forEach(diary -> {
            var diaryId = diary.getId();

            diary.setFoods(diaryFoods.getOrDefault(diaryId, List.of()));
            diary.setPartners(partners.getOrDefault(diaryId, List.of()));
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
