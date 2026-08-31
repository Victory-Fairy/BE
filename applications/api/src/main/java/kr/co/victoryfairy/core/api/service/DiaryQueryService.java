package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.common.service.DiaryFoodDomainService;
import kr.co.victoryfairy.common.service.FileRefDomainService;
import kr.co.victoryfairy.common.service.PartnerDomainService;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.storage.db.core.repository.DiaryCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.DiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryService {

    private final DiaryRepository diaryRepository;

    private final DiaryCustomRepository diaryCustomRepository;

    private final SeatUseHistoryRepository seatUseHistoryRepository;

    private final FileRefDomainService fileRefDomainService;

    private final DiaryFoodDomainService diaryFoodDomainService;

    private final PartnerDomainService partnerDomainService;

    private final RedisHandler redisHandler;

    public List<DiaryDomain.ListResponse> findList(YearMonth date) {
        var id = RequestUtils.getId();

        var startDate = date.atDay(1);
        var endDate = date.atEndOfMonth();

        var monthOfDays = IntStream.rangeClosed(1, date.lengthOfMonth()).mapToObj(day -> date.atDay(day)).toList();

        if (id == null) {
            return monthOfDays.stream()
                .map(day -> new DiaryDomain.ListResponse(null, null, day, null, List.of(), null))
                .toList();
        }

        var diaryList = diaryCustomRepository.findList(new DiaryModel.ListRequest(id, startDate, endDate));

        if (diaryList.isEmpty()) {
            return monthOfDays.stream()
                .map(day -> new DiaryDomain.ListResponse(null, null, day, null, List.of(), null))
                .toList();
        }

        var diaryIds = diaryList.stream().map(DiaryModel.DiaryDto::getId).toList();
        var diaryFileMap = fileRefDomainService.findImageMapByRefIds(RefType.DIARY, diaryIds);
        var diaryMap = diaryList.stream().collect(Collectors.groupingBy(dto -> dto.getMatchAt().toLocalDate()));

        return monthOfDays.stream().map(day -> {
            var diaries = diaryMap.getOrDefault(day, List.of());
            if (diaries.isEmpty()) {
                return new DiaryDomain.ListResponse(null, null, day, null, List.of(), null);
            }

            var images = diaries.stream()
                .map(diary -> diaryFileMap.get(diary.getId()))
                .filter(Objects::nonNull)
                .map(dto -> new DiaryDomain.ImageDto(dto.id(), dto.path(), dto.saveName(), dto.ext(), dto.url()))
                .toList();

            var latestDiary = diaries.stream()
                .max((d1, d2) -> {
                    var time1 = d1.getUpdatedAt() != null ? d1.getUpdatedAt() : d1.getCreatedAt();
                    var time2 = d2.getUpdatedAt() != null ? d2.getUpdatedAt() : d2.getCreatedAt();
                    return time1.compareTo(time2);
                })
                .orElseThrow();

            var image = diaryFileMap.get(latestDiary.getId());
            var latestImage = image == null ? null
                    : new DiaryDomain.ImageDto(image.id(), image.path(), image.saveName(), image.ext(), image.url());

            return new DiaryDomain.ListResponse(latestDiary.getId(), latestDiary.getTeamId(), day, latestImage,
                    images, latestDiary.getResultType());
        }).toList();
    }

    public List<DiaryDomain.DailyListResponse> findDailyList(LocalDate date) {
        var id = RequestUtils.getId();

        if (id == null) {
            return new ArrayList<>();
        }

        var diaryEntities = diaryCustomRepository.findDailyList(new DiaryModel.DailyListRequest(id, date));

        if (diaryEntities.isEmpty()) {
            return new ArrayList<>();
        }

        var formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        var diaryIds = diaryEntities.stream().map(DiaryModel.DiaryDto::getId).toList();
        var fileMap = fileRefDomainService.findImageMapByRefIds(RefType.DIARY, diaryIds);

        return diaryEntities.stream().sorted((e1, e2) -> {
            var t1 = e1.getUpdatedAt() != null ? e1.getUpdatedAt() : e1.getCreatedAt();
            var t2 = e2.getUpdatedAt() != null ? e2.getUpdatedAt() : e2.getCreatedAt();
            return t2.compareTo(t1);
        }).map(entity -> {
            var image = fileMap.get(entity.getId());
            DiaryDomain.ImageDto imageDto = null;

            if (image != null) {
                imageDto = new DiaryDomain.ImageDto(image.id(), image.path(), image.saveName(), image.ext(),
                        image.url());
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
                }
                else if (isAwayWin) {
                    awayResult = MatchEnum.ResultType.WIN;
                    homeResult = MatchEnum.ResultType.LOSS;
                    myResult = isHome ? MatchEnum.ResultType.LOSS : MatchEnum.ResultType.WIN;
                }
                else if (isHomeWin) {
                    awayResult = MatchEnum.ResultType.LOSS;
                    homeResult = MatchEnum.ResultType.WIN;
                    myResult = isHome ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS;
                }
            }

            var awayTeamDto = new MatchDomain.TeamDto(entity.getAwayTeamId(), entity.getAwayTeamName(), awayScore,
                    awayResult);

            var homeTeamDto = new MatchDomain.TeamDto(entity.getHomeTeamId(), entity.getHomeTeamName(), homeScore,
                    homeResult);

            var status = entity.getStatus();
            if (!matchRedis.isEmpty() && entity.getGameMatchId() != null) {
                var matchData = matchRedis.get(entity.getGameMatchId());
                if (matchData != null && matchData.get("status") != null) {
                    status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
                }
            }

            var statusDetail = status.equals(MatchEnum.MatchStatus.CANCELED) && entity.getReason() != null
                    ? entity.getReason() : status.getDesc();

            return new DiaryDomain.DailyListResponse(entity.getId(), entity.getShortName(),
                    entity.getMatchAt().toLocalDate(), entity.getMatchAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                    entity.getTeamId(), awayTeamDto, homeTeamDto, entity.getContent(), myResult, status, statusDetail,
                    imageDto, entity.getCreatedAt());
        }).toList();
    }

    public DiaryDomain.DiaryDetailResponse findById(Long diaryId) {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var foodList = diaryFoodDomainService.findFoodNamesByRefId(RefType.DIARY, diaryId);

        var fileDto = fileRefDomainService.findImagesByRefId(RefType.DIARY, diaryId)
            .stream()
            .map(dto -> new DiaryDomain.ImageDto(dto.id(), dto.path(), dto.saveName(), dto.ext(), dto.url()))
            .toList();

        DiaryDomain.SeatUseHistoryDto seatUseHistoryDto = null;

        var seatUseHistoryEntity = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
        if (seatUseHistoryEntity != null) {
            var seatEntity = seatUseHistoryEntity.getSeatEntity();
            seatUseHistoryDto = new DiaryDomain.SeatUseHistoryDto(seatEntity != null ? seatEntity.getId() : null,
                    seatUseHistoryEntity.getSeatName(), List.of());
        }

        var partnerList = partnerDomainService.findPartnersByRefId(RefType.DIARY, diaryId)
            .stream()
            .map(dto -> new DiaryDomain.PartnerDto(dto.name(), dto.teamId()))
            .toList();

        var matchEntity = diaryEntity.getGameMatchEntity();
        var myTeam = diaryEntity.getTeamEntity().getId();
        var isHome = matchEntity.getHomeTeamEntity().getId().equals(myTeam);
        var awayScore = matchEntity.getAwayScore();
        var homeScore = matchEntity.getHomeScore();

        MatchEnum.ResultType myResult = null;

        if (matchEntity.getStatus() == MatchEnum.MatchStatus.END && awayScore != null && homeScore != null) {
            if (awayScore.equals(homeScore)) {
                myResult = MatchEnum.ResultType.DRAW;
            }
            else {
                boolean myTeamWin = (isHome && homeScore > awayScore) || (!isHome && awayScore > homeScore);
                myResult = myTeamWin ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS;
            }
        }

        return new DiaryDomain.DiaryDetailResponse(diaryEntity.getTeamEntity().getId(), diaryEntity.getViewType(),
                diaryEntity.getGameMatchEntity().getId(), fileDto, diaryEntity.getWeatherType(),
                diaryEntity.getMoodType(), foodList, seatUseHistoryDto, diaryEntity.getContent(), partnerList, myResult,
                diaryEntity.getCreatedAt(), diaryEntity.getUpdatedAt(), diaryEntity.getGameMatchEntity().getLeague());
    }

}
