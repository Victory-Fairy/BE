package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.FreeDiaryDomain;
import kr.co.victoryfairy.core.api.service.FreeDiaryService;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.common.service.DiaryFoodDomainService;
import kr.co.victoryfairy.common.service.FileRefDomainService;
import kr.co.victoryfairy.common.service.PartnerDomainService;
import kr.co.victoryfairy.storage.db.core.entity.FreeDiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.repository.FreeDiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FreeDiaryServiceImpl implements FreeDiaryService {

    private final FreeDiaryRepository freeDiaryRepository;

    private final MemberRepository memberRepository;

    private final FileRefDomainService fileRefDomainService;

    private final DiaryFoodDomainService diaryFoodDomainService;

    private final PartnerDomainService partnerDomainService;

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

        // 도메인 서비스를 통한 연관 데이터 저장
        fileRefDomainService.saveFileRefs(RefType.FREE_DIARY, freeDiaryEntity.getId(), request.fileIdList());
        diaryFoodDomainService.saveFoods(RefType.FREE_DIARY, freeDiaryEntity.getId(), request.foodNameList());
        partnerDomainService.savePartners(RefType.FREE_DIARY, freeDiaryEntity.getId(),
                toPartnerSaveRequests(request.partnerList()));

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

        freeDiaryEntity.updateFreeDiary(request.matchStatus(), request.homeTeamName(), request.awayTeamName(),
                request.homeScore(), request.awayScore(), request.stadiumName(), request.matchAt(), request.teamName(),
                request.viewType(), request.mood(), request.weather(), request.content(), request.seatReview());

        // 도메인 서비스를 통한 연관 데이터 교체 (기존 삭제 후 새로 저장)
        fileRefDomainService.replaceFileRefs(RefType.FREE_DIARY, id, request.fileIdList());
        diaryFoodDomainService.replaceFoods(RefType.FREE_DIARY, id, request.foodNameList());
        partnerDomainService.replacePartners(RefType.FREE_DIARY, id, toPartnerSaveRequests(request.partnerList()));
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

        // 도메인 서비스를 통한 연관 데이터 삭제
        fileRefDomainService.deleteFileRefs(RefType.FREE_DIARY, id);
        diaryFoodDomainService.deleteFoods(RefType.FREE_DIARY, id);
        partnerDomainService.deletePartners(RefType.FREE_DIARY, id);

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
        var images = fileRefDomainService.findImagesByRefId(RefType.FREE_DIARY, id)
            .stream()
            .map(dto -> new FreeDiaryDomain.ImageDto(dto.id(), dto.path(), dto.saveName(), dto.ext()))
            .toList();

        // 음식 조회
        var foodList = diaryFoodDomainService.findFoodNamesByRefId(RefType.FREE_DIARY, id);

        // 파트너 조회
        var partnerList = partnerDomainService.findPartnersByRefId(RefType.FREE_DIARY, id)
            .stream()
            .map(dto -> new FreeDiaryDomain.PartnerDto(dto.name(), dto.teamId()))
            .toList();

        return new FreeDiaryDomain.DetailResponse(entity.getId(), entity.getMatchStatus(), entity.getHomeTeamName(),
                entity.getAwayTeamName(), entity.getHomeScore(), entity.getAwayScore(), entity.getStadiumName(),
                entity.getMatchAt(), entity.getTeamName(), entity.getViewType(), entity.getMoodType(),
                entity.getWeatherType(), entity.getContent(), entity.getSeatReview(), images, foodList, partnerList,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Override
    public List<FreeDiaryDomain.ListResponse> findList(YearMonth date) {
        var memberId = RequestUtils.getId();

        var startDate = date.atDay(1);
        var endDate = date.atEndOfMonth();

        var monthOfDays = IntStream.rangeClosed(1, date.lengthOfMonth()).mapToObj(date::atDay).toList();

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

        var fileMap = fileRefDomainService.findImageMapByRefIds(RefType.FREE_DIARY, diaryIds);

        // 날짜별로 일기 그룹핑
        var diaryMap = diaryList.stream().collect(Collectors.groupingBy(entity -> entity.getMatchAt().toLocalDate()));

        return monthOfDays.stream().map(day -> {
            var diaries = diaryMap.get(day);

            if (diaries == null || diaries.isEmpty()) {
                return new FreeDiaryDomain.ListResponse(null, day, null, List.of());
            }

            // 최신 일기 찾기
            var latestDiary = diaries.stream().max((d1, d2) -> {
                var time1 = d1.getUpdatedAt() != null ? d1.getUpdatedAt() : d1.getCreatedAt();
                var time2 = d2.getUpdatedAt() != null ? d2.getUpdatedAt() : d2.getCreatedAt();
                return time1.compareTo(time2);
            }).orElse(diaries.get(0));

            // 최신 일기의 이미지
            var image = fileMap.get(latestDiary.getId());
            FreeDiaryDomain.ImageDto imageDto = null;
            if (image != null) {
                imageDto = new FreeDiaryDomain.ImageDto(image.id(), image.path(), image.saveName(), image.ext());
            }

            // 해당 날짜의 모든 일기 이미지 수집
            var images = diaries.stream()
                .map(diary -> fileMap.get(diary.getId()))
                .filter(Objects::nonNull)
                .map(dto -> new FreeDiaryDomain.ImageDto(dto.id(), dto.path(), dto.saveName(), dto.ext()))
                .toList();

            return new FreeDiaryDomain.ListResponse(latestDiary.getId(), day, imageDto, images);
        }).toList();
    }

    @Override
    public List<FreeDiaryDomain.DailyListResponse> findDailyList(LocalDate date) {
        var memberId = RequestUtils.getId();

        if (memberId == null) {
            return List.of();
        }

        var startDateTime = date.atStartOfDay();
        var endDateTime = date.atTime(LocalTime.MAX);

        var diaryList = freeDiaryRepository.findByMemberIdAndMatchAtBetween(memberId, startDateTime, endDateTime);

        if (diaryList.isEmpty()) {
            return List.of();
        }

        var diaryIds = diaryList.stream().map(FreeDiaryEntity::getId).toList();

        var fileMap = fileRefDomainService.findImageMapByRefIds(RefType.FREE_DIARY, diaryIds);

        return diaryList.stream().sorted((e1, e2) -> {
            var t1 = e1.getUpdatedAt() != null ? e1.getUpdatedAt() : e1.getCreatedAt();
            var t2 = e2.getUpdatedAt() != null ? e2.getUpdatedAt() : e2.getCreatedAt();
            return t2.compareTo(t1); // 최신순
        }).map(entity -> {
            var image = fileMap.get(entity.getId());
            FreeDiaryDomain.ImageDto imageDto = null;

            if (image != null) {
                imageDto = new FreeDiaryDomain.ImageDto(image.id(), image.path(), image.saveName(), image.ext());
            }

            var homeTeam = new FreeDiaryDomain.TeamDto(entity.getHomeTeamName(), entity.getHomeScore());
            var awayTeam = new FreeDiaryDomain.TeamDto(entity.getAwayTeamName(), entity.getAwayScore());

            return new FreeDiaryDomain.DailyListResponse(entity.getId(), entity.getStadiumName(),
                    entity.getMatchAt().toLocalDate(), entity.getMatchAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                    entity.getTeamName(), homeTeam, awayTeam, entity.getContent(), entity.getMatchStatus(), imageDto,
                    entity.getCreatedAt());
        }).toList();
    }

    /**
     * FreeDiaryDomain.PartnerDto 리스트를 CommonDto.PartnerSaveRequest 리스트로 변환
     */
    private List<CommonDto.PartnerSaveRequest> toPartnerSaveRequests(List<FreeDiaryDomain.PartnerDto> partnerDtoList) {
        if (partnerDtoList == null || partnerDtoList.isEmpty()) {
            return List.of();
        }
        return partnerDtoList.stream().map(dto -> new CommonDto.PartnerSaveRequest(dto.name(), dto.teamId())).toList();
    }

}