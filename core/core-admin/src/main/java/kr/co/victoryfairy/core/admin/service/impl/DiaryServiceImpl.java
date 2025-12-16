package kr.co.victoryfairy.core.admin.service.impl;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.admin.domain.DiaryDomain;
import kr.co.victoryfairy.core.admin.service.DiaryService;
import kr.co.victoryfairy.storage.db.core.entity.DiaryFoodEntity;
import kr.co.victoryfairy.storage.db.core.entity.PartnerEntity;
import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.storage.db.core.repository.DiaryCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.DiaryFoodRepository;
import kr.co.victoryfairy.storage.db.core.repository.PartnerRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.support.config.MapStructConfig;
import kr.co.victoryfairy.support.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {
    private final Mapper mapper;
    private final DiaryCustomRepository diaryCustomRepository;
    private final DiaryFoodRepository diaryFoodRepository;
    private final PartnerRepository partnerRepository;
    private final SeatUseHistoryRepository seatUseHistoryRepository;

    @Override
    public PageResult<DiaryDomain.DiaryListResponse> findAll(DiaryDomain.DiaryListRequest request) {
        var result = diaryCustomRepository.findAll(mapper.toRequest(request));

        var diaryIds = result.getContents().stream()
                .map(DiaryModel.DiaryListResponse::getId)
                .toList();

        var diaryFoods = diaryFoodRepository.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds)
                .stream()
                .collect(groupingBy(
                        DiaryFoodEntity::getRefId,
                        mapping(DiaryFoodEntity::getFoodName, toList())
                ));

        var partners = partnerRepository.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds)
                .stream()
                .collect(groupingBy(
                        PartnerEntity::getRefId,
                        mapping(PartnerEntity::getName, toList())
                ));

        var seatUseHistories = seatUseHistoryRepository.findAllByDiaryEntityIdIn(diaryIds)
                .stream()
                .filter(entity -> entity.getSeatEntity() != null)
                .collect(groupingBy(
                        entity -> entity.getDiaryEntity().getId(),
                        mapping(
                                entity -> entity.getSeatEntity().getName() + " " + entity.getSeatName(),
                                toList()
                        )
                ));

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
        DiaryModel.DiaryListRequest toRequest(DiaryDomain.DiaryListRequest request);

        List<DiaryDomain.DiaryListResponse> toDiaryListResponse(List<DiaryModel.DiaryListResponse> diaryList);
        default PageResult<DiaryDomain.DiaryListResponse> toPageResult(PageResult<DiaryModel.DiaryListResponse> pageResult) {
            var response = toDiaryListResponse(pageResult.getContents());
            return new PageResult<>(response, pageResult.getTotal());
        }
    }
}