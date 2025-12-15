package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.FreeDiaryDomain;
import kr.co.victoryfairy.core.api.service.FreeDiaryService;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FreeDiaryServiceImpl implements FreeDiaryService {

    private final FreeDiaryRepository freeDiaryRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final FileRepository fileRepository;
    private final FileRefRepository fileRefRepository;
    private final DiaryFoodRepository diaryFoodRepository;
    private final PartnerRepository partnerRepository;

    @Override
    @Transactional
    public FreeDiaryDomain.WriteResponse write(FreeDiaryDomain.WriteRequest request) {
        var memberId = RequestUtils.getId();
        if (memberId == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        FreeDiaryEntity freeDiaryEntity = FreeDiaryEntity.builder()
                .member(member)
                .matchStatus(request.matchStatus())
                .homeTeamName(request.homeTeamName())
                .awayTeamName(request.awayTeamName())
                .homeScore(request.homeScore())
                .awayScore(request.awayScore())
                .stadiumName(request.stadiumName())
                .matchAt(request.matchAt())
                .teamName(request.teamName())
                .viewType(request.viewType())
                .moodType(request.mood())
                .weatherType(request.weather())
                .content(request.content())
                .seatReview(request.seatReview())
                .build();
        freeDiaryRepository.save(freeDiaryEntity);

        // 이미지 저장
        if (request.fileIdList() != null && !request.fileIdList().isEmpty()) {
            var fileEntities = fileRepository.findAllById(request.fileIdList());
            var fileRefEntities = fileEntities.stream()
                    .map(file -> FileRefEntity.builder()
                            .fileEntity(file)
                            .refId(freeDiaryEntity.getId())
                            .refType(RefType.FREE_DIARY)
                            .build()
                    )
                    .toList();
            fileRefRepository.saveAll(fileRefEntities);
        }

        // 음식 저장
        if (request.foodNameList() != null && !request.foodNameList().isEmpty()) {
            List<DiaryFoodEntity> foodList = request.foodNameList().stream()
                    .map(food -> DiaryFoodEntity.builder()
                            .refId(freeDiaryEntity.getId())
                            .refType(RefType.FREE_DIARY)
                            .foodName(food)
                            .build()
                    )
                    .toList();
            diaryFoodRepository.saveAll(foodList);
        }

        // 파트너 저장
        if (request.partnerList() != null && !request.partnerList().isEmpty()) {
            List<PartnerEntity> partnerEntityList = new ArrayList<>();
            for (FreeDiaryDomain.PartnerDto partnerDto : request.partnerList()) {
                TeamEntity partnerTeamEntity = null;
                String partnerTeamName = null;

                if (partnerDto.teamId() != null) {
                    partnerTeamEntity = teamRepository.findById(partnerDto.teamId()).orElse(null);
                    if (partnerTeamEntity != null) {
                        partnerTeamName = partnerTeamEntity.getName();
                    }
                }

                PartnerEntity partnerEntity = PartnerEntity.builder()
                        .refId(freeDiaryEntity.getId())
                        .refType(RefType.FREE_DIARY)
                        .name(partnerDto.name())
                        .teamName(partnerTeamName)
                        .teamEntity(partnerTeamEntity)
                        .build();
                partnerEntityList.add(partnerEntity);
            }
            partnerRepository.saveAll(partnerEntityList);
        }

        return new FreeDiaryDomain.WriteResponse(freeDiaryEntity.getId());
    }

    @Override
    @Transactional
    public void update(Long id, FreeDiaryDomain.UpdateRequest request) {
        var memberId = RequestUtils.getId();
        if (memberId == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        FreeDiaryEntity freeDiaryEntity = freeDiaryRepository.findByMemberIdAndId(memberId, id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        freeDiaryEntity.updateFreeDiary(
                request.matchStatus(),
                request.homeTeamName(),
                request.awayTeamName(),
                request.homeScore(),
                request.awayScore(),
                request.stadiumName(),
                request.matchAt(),
                request.teamName(),
                request.viewType(),
                request.mood(),
                request.weather(),
                request.content(),
                request.seatReview()
        );

        // 기존 이미지 삭제 후 새로 저장
        var existingFileRefs = fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(RefType.FREE_DIARY, id);
        if (!existingFileRefs.isEmpty()) {
            fileRefRepository.deleteAll(existingFileRefs);
        }

        if (request.fileIdList() != null && !request.fileIdList().isEmpty()) {
            var fileEntities = fileRepository.findAllById(request.fileIdList());
            var fileRefEntities = fileEntities.stream()
                    .map(file -> FileRefEntity.builder()
                            .fileEntity(file)
                            .refId(freeDiaryEntity.getId())
                            .refType(RefType.FREE_DIARY)
                            .build()
                    )
                    .toList();
            fileRefRepository.saveAll(fileRefEntities);
        }

        // 기존 음식 삭제 후 새로 저장
        var existingFoods = diaryFoodRepository.findByRefTypeAndRefId(RefType.FREE_DIARY, id);
        if (!existingFoods.isEmpty()) {
            diaryFoodRepository.deleteAll(existingFoods);
        }

        if (request.foodNameList() != null && !request.foodNameList().isEmpty()) {
            List<DiaryFoodEntity> foodList = request.foodNameList().stream()
                    .map(food -> DiaryFoodEntity.builder()
                            .refId(freeDiaryEntity.getId())
                            .refType(RefType.FREE_DIARY)
                            .foodName(food)
                            .build()
                    )
                    .toList();
            diaryFoodRepository.saveAll(foodList);
        }

        // 기존 파트너 삭제 후 새로 저장
        var existingPartners = partnerRepository.findByRefTypeAndRefId(RefType.FREE_DIARY, id);
        if (!existingPartners.isEmpty()) {
            partnerRepository.deleteAll(existingPartners);
        }

        if (request.partnerList() != null && !request.partnerList().isEmpty()) {
            List<PartnerEntity> partnerEntityList = new ArrayList<>();
            for (FreeDiaryDomain.PartnerDto partnerDto : request.partnerList()) {
                TeamEntity partnerTeamEntity = null;
                String partnerTeamName = null;

                if (partnerDto.teamId() != null) {
                    partnerTeamEntity = teamRepository.findById(partnerDto.teamId()).orElse(null);
                    if (partnerTeamEntity != null) {
                        partnerTeamName = partnerTeamEntity.getName();
                    }
                }

                PartnerEntity partnerEntity = PartnerEntity.builder()
                        .refId(freeDiaryEntity.getId())
                        .refType(RefType.FREE_DIARY)
                        .name(partnerDto.name())
                        .teamName(partnerTeamName)
                        .teamEntity(partnerTeamEntity)
                        .build();
                partnerEntityList.add(partnerEntity);
            }
            partnerRepository.saveAll(partnerEntityList);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var memberId = RequestUtils.getId();
        if (memberId == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        FreeDiaryEntity freeDiaryEntity = freeDiaryRepository.findByMemberIdAndId(memberId, id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        // 연관 데이터 삭제
        var fileRefs = fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(RefType.FREE_DIARY, id);
        if (!fileRefs.isEmpty()) {
            fileRefRepository.deleteAll(fileRefs);
        }

        var foods = diaryFoodRepository.findByRefTypeAndRefId(RefType.FREE_DIARY, id);
        if (!foods.isEmpty()) {
            diaryFoodRepository.deleteAll(foods);
        }

        var partners = partnerRepository.findByRefTypeAndRefId(RefType.FREE_DIARY, id);
        if (!partners.isEmpty()) {
            partnerRepository.deleteAll(partners);
        }

        freeDiaryRepository.delete(freeDiaryEntity);
    }

    @Override
    public FreeDiaryDomain.DetailResponse findById(Long id) {
        var memberId = RequestUtils.getId();
        if (memberId == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        FreeDiaryEntity entity = freeDiaryRepository.findByMemberIdAndId(memberId, id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        // 이미지 조회
        var images = fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(RefType.FREE_DIARY, id).stream()
                .map(ref -> {
                    var file = ref.getFileEntity();
                    return new FreeDiaryDomain.ImageDto(file.getId(), file.getPath(), file.getSaveName(), file.getExt());
                })
                .toList();

        // 음식 조회
        var foodList = diaryFoodRepository.findByRefTypeAndRefId(RefType.FREE_DIARY, id).stream()
                .map(DiaryFoodEntity::getFoodName)
                .toList();

        // 파트너 조회
        var partnerList = partnerRepository.findByRefTypeAndRefId(RefType.FREE_DIARY, id).stream()
                .map(p -> new FreeDiaryDomain.PartnerDto(
                        p.getName(),
                        p.getTeamEntity() != null ? p.getTeamEntity().getId() : null
                ))
                .toList();

        return new FreeDiaryDomain.DetailResponse(
                entity.getId(),
                entity.getMatchStatus(),
                entity.getHomeTeamName(),
                entity.getAwayTeamName(),
                entity.getHomeScore(),
                entity.getAwayScore(),
                entity.getStadiumName(),
                entity.getMatchAt(),
                entity.getTeamName(),
                entity.getViewType(),
                entity.getMoodType(),
                entity.getWeatherType(),
                entity.getContent(),
                entity.getSeatReview(),
                images,
                foodList,
                partnerList,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Override
    public List<FreeDiaryDomain.ListResponse> findList(YearMonth date) {
        var memberId = RequestUtils.getId();

        var startDate = date.atDay(1);
        var endDate = date.atEndOfMonth();

        var monthOfDays = IntStream.rangeClosed(1, date.lengthOfMonth())
                .mapToObj(date::atDay)
                .toList();

        if (memberId == null) {
            return monthOfDays.stream()
                    .map(day -> new FreeDiaryDomain.ListResponse(null, day, null, List.of()))
                    .toList();
        }

        var startDateTime = startDate.atStartOfDay();
        var endDateTime = endDate.atTime(LocalTime.MAX);

        var diaryList = freeDiaryRepository.findByMemberIdAndMatchAtBetween(memberId, startDateTime, endDateTime);

        if (diaryList.isEmpty()) {
            return monthOfDays.stream()
                    .map(day -> new FreeDiaryDomain.ListResponse(null, day, null, List.of()))
                    .toList();
        }

        var diaryIds = diaryList.stream().map(FreeDiaryEntity::getId).toList();

        var fileMap = fileRefRepository.findByRefTypeAndRefIdInAndIsUseTrue(RefType.FREE_DIARY, diaryIds).stream()
                .collect(Collectors.toMap(
                        FileRefEntity::getRefId,
                        entity -> entity,
                        (existing, replacement) -> existing
                ));

        // 날짜별로 일기 그룹핑
        var diaryMap = diaryList.stream()
                .collect(Collectors.groupingBy(entity -> entity.getMatchAt().toLocalDate()));

        return monthOfDays.stream()
                .map(day -> {
                    var diaries = diaryMap.get(day);

                    if (diaries == null || diaries.isEmpty()) {
                        return new FreeDiaryDomain.ListResponse(null, day, null, List.of());
                    }

                    // 최신 일기 찾기
                    var latestDiary = diaries.stream()
                            .max((d1, d2) -> {
                                var time1 = d1.getUpdatedAt() != null ? d1.getUpdatedAt() : d1.getCreatedAt();
                                var time2 = d2.getUpdatedAt() != null ? d2.getUpdatedAt() : d2.getCreatedAt();
                                return time1.compareTo(time2);
                            })
                            .orElse(diaries.get(0));

                    // 최신 일기의 이미지
                    var fileRefEntity = fileMap.get(latestDiary.getId());
                    FreeDiaryDomain.ImageDto imageDto = null;
                    if (fileRefEntity != null) {
                        var fileEntity = fileRefEntity.getFileEntity();
                        imageDto = new FreeDiaryDomain.ImageDto(fileEntity.getId(), fileEntity.getPath(), fileEntity.getSaveName(), fileEntity.getExt());
                    }

                    // 해당 날짜의 모든 일기 이미지 수집
                    var images = diaries.stream()
                            .map(diary -> fileMap.get(diary.getId()))
                            .filter(Objects::nonNull)
                            .map(ref -> {
                                var fileEntity = ref.getFileEntity();
                                return new FreeDiaryDomain.ImageDto(fileEntity.getId(), fileEntity.getPath(), fileEntity.getSaveName(), fileEntity.getExt());
                            })
                            .toList();

                    return new FreeDiaryDomain.ListResponse(latestDiary.getId(), day, imageDto, images);
                })
                .toList();
    }

    @Override
    public List<FreeDiaryDomain.DailyListResponse> findDailyList(LocalDate date) {
        var memberId = RequestUtils.getId();

        if (memberId == null) {
            return new ArrayList<>();
        }

        var startDateTime = date.atStartOfDay();
        var endDateTime = date.atTime(LocalTime.MAX);

        var diaryList = freeDiaryRepository.findByMemberIdAndMatchAtBetween(memberId, startDateTime, endDateTime);

        if (diaryList.isEmpty()) {
            return new ArrayList<>();
        }

        var diaryIds = diaryList.stream().map(FreeDiaryEntity::getId).toList();

        var fileMap = fileRefRepository.findByRefTypeAndRefIdInAndIsUseTrue(RefType.FREE_DIARY, diaryIds).stream()
                .collect(Collectors.toMap(
                        FileRefEntity::getRefId,
                        entity -> entity,
                        (existing, replacement) -> existing
                ));

        return diaryList.stream()
                .sorted((e1, e2) -> {
                    var t1 = e1.getUpdatedAt() != null ? e1.getUpdatedAt() : e1.getCreatedAt();
                    var t2 = e2.getUpdatedAt() != null ? e2.getUpdatedAt() : e2.getCreatedAt();
                    return t2.compareTo(t1); // 최신순
                })
                .map(entity -> {
                    var fileRefEntity = fileMap.get(entity.getId());
                    FreeDiaryDomain.ImageDto imageDto = null;

                    if (fileRefEntity != null) {
                        var fileEntity = fileRefEntity.getFileEntity();
                        imageDto = new FreeDiaryDomain.ImageDto(fileEntity.getId(), fileEntity.getPath(), fileEntity.getSaveName(), fileEntity.getExt());
                    }

                    var homeTeam = new FreeDiaryDomain.TeamDto(entity.getHomeTeamName(), entity.getHomeScore());
                    var awayTeam = new FreeDiaryDomain.TeamDto(entity.getAwayTeamName(), entity.getAwayScore());

                    return new FreeDiaryDomain.DailyListResponse(
                            entity.getId(),
                            entity.getStadiumName(),
                            entity.getMatchAt().toLocalDate(),
                            entity.getMatchAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                            entity.getTeamName(),
                            homeTeam,
                            awayTeam,
                            entity.getContent(),
                            entity.getMatchStatus(),
                            imageDto,
                            entity.getCreatedAt()
                    );
                })
                .toList();
    }
}