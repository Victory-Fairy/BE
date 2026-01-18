package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.EventType;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.service.DiaryService;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.redis.lock.DistributedLock;
import kr.co.victoryfairy.redis.lock.LockName;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.common.service.DiaryFoodDomainService;
import kr.co.victoryfairy.common.service.FileRefDomainService;
import kr.co.victoryfairy.common.service.PartnerDomainService;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;

import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryCustomRepository diaryCustomRepository;
    private final SeatRepository seatRepository;
    private final SeatUseHistoryRepository seatUseHistoryRepository;
    private final SeatReviewRepository seatReviewRepository;
    private final GameMatchRepository gameMatchRepository;
    private final GameRecordRepository gameRecordRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final FreeDiaryRepository freeDiaryRepository;

    private final FileRefDomainService fileRefDomainService;
    private final DiaryFoodDomainService diaryFoodDomainService;
    private final PartnerDomainService partnerDomainService;

    private final RedisHandler redisHandler;

    @Override
    @Transactional
    @DistributedLock(value = LockName.DIARY_WRITE, key = "#memberId + '_' + #diaryDto.gameMatchId()")
    public DiaryDomain.WriteResponse writeDiary(Long memberId, DiaryDomain.WriteRequest diaryDto){
        MemberEntity member = memberRepository.findById(Objects.requireNonNull(memberId))
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

            // 일기를 작성할 경기 조회
            GameMatchEntity gameMatchEntity = gameMatchRepository.findById(diaryDto.gameMatchId())
                    .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

            var teamEntity = teamRepository.findById(diaryDto.teamId())
                    .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

            if (diaryRepository.findByMemberAndGameMatchEntity(member, gameMatchEntity) != null) {
                throw new CustomException(HttpStatus.CONFLICT, MessageEnum.Data.FAIL_DUPLICATE);
            }

            DiaryEntity diaryEntity = DiaryEntity.builder()
                    .member(member)
                    .teamName(teamEntity.getName())
                    .teamEntity(teamEntity)
                    .viewType(diaryDto.viewType())
                    .gameMatchEntity(gameMatchEntity)
                    .weatherType(diaryDto.weather())
                    .moodType(diaryDto.mood())
                    .content(diaryDto.content())
                    .build();
            diaryRepository.save(diaryEntity);

            // 도메인 서비스를 통한 연관 데이터 저장
            fileRefDomainService.saveFileRefs(RefType.DIARY, diaryEntity.getId(), diaryDto.fileId());
            diaryFoodDomainService.saveFoods(RefType.DIARY, diaryEntity.getId(), diaryDto.foodNameList());
            partnerDomainService.savePartners(RefType.DIARY, diaryEntity.getId(), toPartnerSaveRequests(diaryDto.partnerList()));

            //
            DiaryDomain.SeatUseHistoryDto diaryDtoSeat = diaryDto.seat();
            if (diaryDtoSeat != null) {
                // 좌석 조회

                SeatEntity seatEntity = null;
                if (diaryDtoSeat.id() != null) {
                    seatEntity = seatRepository.findById(diaryDtoSeat.id()).orElse(null);
                }

                // 좌석 이용 내역 저장
                SeatUseHistoryEntity seatUseHistoryEntity = SeatUseHistoryEntity.builder()
                        .diaryEntity(diaryEntity)
                        .seatEntity(seatEntity)
                        .seatName(diaryDtoSeat.name())
                        .build();
                seatUseHistoryRepository.save(seatUseHistoryEntity);
            }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (gameMatchEntity.getStatus().equals(MatchEnum.MatchStatus.END) || gameMatchEntity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)) {
                    var writeEventDto = new DiaryDomain.WriteEventDto(
                            diaryDto.gameMatchId(), memberId, diaryEntity.getId(), EventType.DIARY
                    );
                    redisHandler.pushEvent("write_diary", writeEventDto);
                }
            }
        });

        return new DiaryDomain.WriteResponse(diaryEntity.getId());
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryDomain.UpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        MemberEntity member = memberRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(request.teamId())
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));


        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var gameRecordEntity = gameRecordRepository.findByMemberAndDiaryEntityId(member, diaryId);

        diaryEntity.updateDiary(
                teamEntity.getName(),
                teamEntity,
                request.viewType(),
                request.mood(),
                request.weather(),
                request.content()
        );
        diaryRepository.save(diaryEntity);

        // 도메인 서비스를 통한 연관 데이터 교체 (기존 삭제 후 새로 저장)
        fileRefDomainService.replaceFileRefs(RefType.DIARY, diaryId, request.fileId());
        diaryFoodDomainService.replaceFoods(RefType.DIARY, diaryId, request.foodNameList());
        partnerDomainService.replacePartners(RefType.DIARY, diaryId, toPartnerSaveRequests(request.partnerList()));

        //
        DiaryDomain.SeatUseHistoryDto diaryDtoSeat = request.seat();
        if (diaryDtoSeat != null) {
            // 기존 데이터 삭제 처리
            var bfSeatUseHistoryEntity = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
            var bfSeatReviewEntities = seatReviewRepository.findBySeatUseHistoryEntity(bfSeatUseHistoryEntity);

            if (!bfSeatReviewEntities.isEmpty()) {
                seatReviewRepository.deleteAll(bfSeatReviewEntities);
            }
            if (bfSeatUseHistoryEntity != null) {
                seatUseHistoryRepository.delete(bfSeatUseHistoryEntity);
            }

            // 좌석 조회
            SeatEntity seatEntity = null;
            if (diaryDtoSeat.id() != null) {
                seatEntity = seatRepository.findById(diaryDtoSeat.id()).orElse(null);
            }

            // 좌석 이용 내역 저장
            SeatUseHistoryEntity seatUseHistoryEntity = SeatUseHistoryEntity.builder()
                    .diaryEntity(diaryEntity)
                    .seatEntity(seatEntity)
                    .seatName(diaryDtoSeat.name())
                    .build();
            seatUseHistoryRepository.save(seatUseHistoryEntity);
        }

        // 경기 결과 수정 반영
        if (gameRecordEntity != null && !gameRecordEntity.getTeamEntity().getId().equals(teamEntity.getId())) {
            var bfMyTeamEntity = gameRecordEntity.getTeamEntity();
            var bfResult = gameRecordEntity.getResultType();
            gameRecordEntity.updateRecord(
                    teamEntity,
                    bfMyTeamEntity,
                    bfResult.equals(MatchEnum.ResultType.WIN) ? MatchEnum.ResultType.LOSS :
                            bfResult.equals(MatchEnum.ResultType.LOSS) ? MatchEnum.ResultType.WIN : bfResult
            );
            gameRecordRepository.save(gameRecordEntity);
        }
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        MemberEntity member = memberRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var gameRecordEntity = gameRecordRepository.findByMemberAndDiaryEntityId(member, diaryId);
        if (gameRecordEntity != null) {
            gameRecordRepository.delete(gameRecordEntity);
        }

        // 도메인 서비스를 통한 연관 데이터 삭제
        fileRefDomainService.deleteFileRefs(RefType.DIARY, diaryId);
        diaryFoodDomainService.deleteFoods(RefType.DIARY, diaryId);
        partnerDomainService.deletePartners(RefType.DIARY, diaryId);

        var bfSeatUseHistoryEntity = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
        if (bfSeatUseHistoryEntity != null) {
            seatUseHistoryRepository.delete(bfSeatUseHistoryEntity);
        }

        var bfSeatReviewEntities = seatReviewRepository.findBySeatUseHistoryEntity(bfSeatUseHistoryEntity);
        if (!bfSeatReviewEntities.isEmpty()) {
            seatReviewRepository.deleteAll(bfSeatReviewEntities);
        }

        diaryRepository.delete(diaryEntity);
    }

    @Override
    public List<DiaryDomain.ListResponse> findList(YearMonth date) {
        var id = RequestUtils.getId();

        var startDate = date.atDay(1);
        var endDate = date.atEndOfMonth();

        var monthOfDays = IntStream.rangeClosed(1, date.lengthOfMonth())
                .mapToObj(day -> date.atDay(day))
                .toList();

        if (id == null) {
            return monthOfDays.stream()
                    .map(day -> new DiaryDomain.ListResponse(null, null, day, null, List.of(), null))
                    .toList();
        }

        // 일반 일기 조회
        var diaryList = diaryCustomRepository.findList(new DiaryModel.ListRequest(id, startDate, endDate));

        // 자유 일기 조회
        var startDateTime = startDate.atStartOfDay();
        var endDateTime = endDate.atTime(java.time.LocalTime.MAX);
        var freeDiaryList = freeDiaryRepository.findByMemberIdAndMatchAtBetween(id, startDateTime, endDateTime);

        // 둘 다 비어있으면 빈 캘린더 반환
        if (diaryList.isEmpty() && freeDiaryList.isEmpty()) {
            return monthOfDays.stream()
                    .map(day -> new DiaryDomain.ListResponse(null, null, day, null, List.of(), null))
                    .toList();
        }

        // 일반 일기 파일 맵
        var diaryIds = diaryList.stream()
                .map(DiaryModel.DiaryDto::getId).toList();
        var diaryFileMap = fileRefDomainService.findImageMapByRefIds(RefType.DIARY, diaryIds);

        // 자유 일기 파일 맵
        var freeDiaryIds = freeDiaryList.stream()
                .map(FreeDiaryEntity::getId).toList();
        var freeDiaryFileMap = fileRefDomainService.findImageMapByRefIds(RefType.FREE_DIARY, freeDiaryIds);

        // 날짜별로 일기 그룹핑
        var diaryMap = diaryList.stream()
                .collect(Collectors.groupingBy(dto -> dto.getMatchAt().toLocalDate()));
        var freeDiaryMap = freeDiaryList.stream()
                .collect(Collectors.groupingBy(entity -> entity.getMatchAt().toLocalDate()));

        return monthOfDays.stream()
                .map(day -> {
                    var diaries = diaryMap.get(day);
                    var freeDiaries = freeDiaryMap.get(day);

                    boolean hasDiary = diaries != null && !diaries.isEmpty();
                    boolean hasFreeDiary = freeDiaries != null && !freeDiaries.isEmpty();

                    if (!hasDiary && !hasFreeDiary) {
                        return new DiaryDomain.ListResponse(null, null, day, null, List.of(), null);
                    }

                    // 모든 일기의 이미지 수집
                    List<DiaryDomain.ImageDto> allImages = new ArrayList<>();

                    // 일반 일기 이미지
                    if (hasDiary) {
                        diaries.stream()
                                .map(diary -> diaryFileMap.get(diary.getId()))
                                .filter(Objects::nonNull)
                                .map(dto -> new DiaryDomain.ImageDto(dto.id(), dto.path(), dto.saveName(), dto.ext()))
                                .forEach(allImages::add);
                    }

                    // 자유 일기 이미지
                    if (hasFreeDiary) {
                        freeDiaries.stream()
                                .map(fd -> freeDiaryFileMap.get(fd.getId()))
                                .filter(Objects::nonNull)
                                .map(dto -> new DiaryDomain.ImageDto(dto.id(), dto.path(), dto.saveName(), dto.ext()))
                                .forEach(allImages::add);
                    }

                    // 최신 일기 찾기 (일반 일기 우선, 없으면 자유 일기)
                    Long latestId = null;
                    Long latestTeamId = null;
                    MatchEnum.ResultType latestResultType = null;
                    DiaryDomain.ImageDto latestImage = null;

                    if (hasDiary) {
                        var latestDiary = diaries.stream()
                                .max((d1, d2) -> {
                                    var time1 = d1.getUpdatedAt() != null ? d1.getUpdatedAt() : d1.getCreatedAt();
                                    var time2 = d2.getUpdatedAt() != null ? d2.getUpdatedAt() : d2.getCreatedAt();
                                    return time1.compareTo(time2);
                                })
                                .orElse(diaries.get(0));
                        latestId = latestDiary.getId();
                        latestTeamId = latestDiary.getTeamId();
                        latestResultType = latestDiary.getResultType();

                        var imageDto = diaryFileMap.get(latestDiary.getId());
                        if (imageDto != null) {
                            latestImage = new DiaryDomain.ImageDto(imageDto.id(), imageDto.path(), imageDto.saveName(), imageDto.ext());
                        }
                    } else if (hasFreeDiary) {
                        var latestFreeDiary = freeDiaries.stream()
                                .max((d1, d2) -> {
                                    var time1 = d1.getUpdatedAt() != null ? d1.getUpdatedAt() : d1.getCreatedAt();
                                    var time2 = d2.getUpdatedAt() != null ? d2.getUpdatedAt() : d2.getCreatedAt();
                                    return time1.compareTo(time2);
                                })
                                .orElse(freeDiaries.get(0));
                        latestId = latestFreeDiary.getId();
                        // 자유 일기는 teamId, resultType 없음

                        var imageDto = freeDiaryFileMap.get(latestFreeDiary.getId());
                        if (imageDto != null) {
                            latestImage = new DiaryDomain.ImageDto(imageDto.id(), imageDto.path(), imageDto.saveName(), imageDto.ext());
                        }
                    }

                    return new DiaryDomain.ListResponse(latestId, latestTeamId, day, latestImage, allImages, latestResultType);
                })
                .toList();
    }

    @Override
    public List<DiaryDomain.DailyListResponse> findDailyList(LocalDate date) {
        var id = RequestUtils.getId();

        if (id == null) {
            return new ArrayList<>();
        }

        var diaryEntities = diaryCustomRepository.findDailyList(new DiaryModel.DailyListRequest(id, date));

        if (diaryEntities.isEmpty()) {
            return new ArrayList<>();
        }

        // Redis에서 해당 날짜 경기 정보 조회
        var formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        var diaryIds = diaryEntities.stream()
                .map(DiaryModel.DiaryDto :: getId).toList();

        var fileMap = fileRefDomainService.findImageMapByRefIds(RefType.DIARY, diaryIds);

        return diaryEntities.stream()
                .sorted((e1, e2) -> {
                    // updatedAt 우선, 없으면 createdAt 으로 비교
                    var t1 = e1.getUpdatedAt() != null ? e1.getUpdatedAt() : e1.getCreatedAt();
                    var t2 = e2.getUpdatedAt() != null ? e2.getUpdatedAt() : e2.getCreatedAt();
                    return t2.compareTo(t1); // 최신순 (내림차순)
                })
                .map(entity -> {
                    var image = fileMap.get(entity.getId());
                    DiaryDomain.ImageDto imageDto = null;

                    if (image != null) {
                        imageDto = new DiaryDomain.ImageDto(image.id(), image.path(), image.saveName(), image.ext());
                    }

                    var myTeam = entity.getTeamId();
                    var isHome = entity.getHomeTeamId().equals(myTeam);
                    var awayScore = entity.getAwayScore();
                    var homeScore = entity.getHomeScore();


                    MatchEnum.ResultType myResult = null;
                    MatchEnum.ResultType awayResult = null;
                    MatchEnum.ResultType homeResult = null;

                    if (awayScore != null && homeScore != null) {
                        boolean isDraw = awayScore.equals(homeScore);
                        boolean isAwayWin = awayScore > homeScore;
                        boolean isHomeWin = homeScore > awayScore;

                        if (isDraw) {
                            awayResult = homeResult = myResult = MatchEnum.ResultType.DRAW;
                        } else if (isAwayWin) {
                            awayResult = MatchEnum.ResultType.WIN;
                            homeResult = MatchEnum.ResultType.LOSS;
                            myResult = isHome ? MatchEnum.ResultType.LOSS : MatchEnum.ResultType.WIN;
                        } else if (isHomeWin) {
                            awayResult = MatchEnum.ResultType.LOSS;
                            homeResult = MatchEnum.ResultType.WIN;
                            myResult = isHome ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS;
                        }
                    }

                    var awayTeamDto = new MatchDomain.TeamDto(entity.getAwayTeamId(), entity.getAwayTeamName(), awayScore, awayResult);

                    var homeTeamDto = new MatchDomain.TeamDto(entity.getHomeTeamId(), entity.getHomeTeamName(), homeScore, homeResult);

                    // Redis에 데이터가 있으면 status 우선 적용
                    var status = entity.getStatus();
                    if (!matchRedis.isEmpty() && entity.getGameMatchId() != null) {
                        var matchData = matchRedis.get(entity.getGameMatchId());
                        if (matchData != null && matchData.get("status") != null) {
                            status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
                        }
                    }

                    // 취소된 경기는 취소 사유를 statusDetail로 반환
                    var statusDetail = status.equals(MatchEnum.MatchStatus.CANCELED) && entity.getReason() != null
                            ? entity.getReason()
                            : status.getDesc();

                    return new DiaryDomain.DailyListResponse(
                                                            entity.getId(),
                                                            entity.getShortName(),
                                                            entity.getMatchAt().toLocalDate(),
                                                            entity.getMatchAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                                                            entity.getTeamId(),
                                                            awayTeamDto,
                                                            homeTeamDto,
                                                            entity.getContent(),
                                                            myResult,
                                                            status,
                                                            statusDetail,
                                                            imageDto,
                                                            entity.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    public DiaryDomain.DiaryDetailResponse findById(Long diaryId) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var foodList = diaryFoodDomainService.findFoodNamesByRefId(RefType.DIARY, diaryId);

        var fileDto = fileRefDomainService.findImagesByRefId(RefType.DIARY, diaryId).stream()
                .map(dto -> new DiaryDomain.ImageDto(dto.id(), dto.path(), dto.saveName(), dto.ext()))
                .toList();

        DiaryDomain.SeatUseHistoryDto seatUseHistoryDto = null;

        var seatUseHistoryEntity = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
        if (seatUseHistoryEntity != null) {
            var seatEntity = seatUseHistoryEntity.getSeatEntity();
            seatUseHistoryDto = new DiaryDomain.SeatUseHistoryDto(
                    seatEntity != null ? seatEntity.getId() : null,
                    seatUseHistoryEntity.getSeatName(),
                    List.of()
            );
        }

        var partnerList = partnerDomainService.findPartnersByRefId(RefType.DIARY, diaryId).stream()
                .map(dto -> new DiaryDomain.PartnerDto(dto.name(), dto.teamId()))
                .toList();

        var matchEntity = diaryEntity.getGameMatchEntity();
        var myTeam = diaryEntity.getTeamEntity().getId();
        var isHome = matchEntity.getHomeTeamEntity().getId().equals(myTeam);
        var awayScore = matchEntity.getAwayScore();
        var homeScore = matchEntity.getHomeScore();

        MatchEnum.ResultType myResult = null;

        if (awayScore != null && homeScore != null) {
            if (awayScore.equals(homeScore)) {
                myResult = MatchEnum.ResultType.DRAW;
            } else {
                boolean myTeamWin = (isHome && homeScore > awayScore) || (!isHome && awayScore > homeScore);
                myResult = myTeamWin ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS;
            }
        }

        return new DiaryDomain.DiaryDetailResponse(
                diaryEntity.getTeamEntity().getId(),
                diaryEntity.getViewType(),
                diaryEntity.getGameMatchEntity().getId(),
                fileDto,
                diaryEntity.getWeatherType(),
                diaryEntity.getMoodType(),
                foodList,
                seatUseHistoryDto,
                diaryEntity.getContent(),
                partnerList,
                myResult,
                diaryEntity.getCreatedAt(),
                diaryEntity.getUpdatedAt()
        );
    }

    /**
     * DiaryDomain.PartnerDto 리스트를 CommonDto.PartnerSaveRequest 리스트로 변환
     */
    private List<CommonDto.PartnerSaveRequest> toPartnerSaveRequests(List<DiaryDomain.PartnerDto> partnerDtoList) {
        if (partnerDtoList == null || partnerDtoList.isEmpty()) {
            return List.of();
        }
        return partnerDtoList.stream()
                .map(dto -> new CommonDto.PartnerSaveRequest(dto.name(), dto.teamId()))
                .toList();
    }

}
