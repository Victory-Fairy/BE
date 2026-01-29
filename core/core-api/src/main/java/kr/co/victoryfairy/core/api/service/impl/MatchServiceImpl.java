package kr.co.victoryfairy.core.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.service.MatchService;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.HitterRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.entity.PitcherRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final TeamRepository teamRepository;

    private final StadiumRepository stadiumRepository;

    private final GameMatchRepository gameMatchRepository;

    private final GameMatchCustomRepository gameMatchCustomRepository;

    private final PitcherRecordRepository pitcherRecordRepository;

    private final HitterRecordRepository hitterRecordRepository;

    private final MemberRepository memberRepository;

    private final MemberInfoRepository memberInfoRepository;

    private final DiaryRepository diaryRepository;

    private final RedisHandler redisHandler;

    @Override
    public MatchDomain.MatchListResponse findList(LocalDate date) {
        return findList(date, null);
    }

    @Override
    public MatchDomain.MatchListResponse findList(LocalDate date, MatchEnum.LeagueType league) {
        var memberId = RequestUtils.getId();

        var teamEntity = Optional.ofNullable(memberId)
            .flatMap(memberInfoRepository::findByMemberEntity_Id)
            .map(MemberInfoEntity::getTeamEntity)
            .orElse(null);

        var formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 당일 경기 경우 redis 에서 가져오기
        List<MatchDomain.MatchListDto> matchList = new ArrayList();

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntities = gameMatchCustomRepository.findByMatchAt(date, league);
            log.debug("findList - date: {}, league: {}, matchEntities size: {}", date, league, matchEntities.size());
            matchEntities = matchEntities
                .stream()
                .sorted(Comparator.comparing(entity -> entity.getMatchAt()))
                .toList();

            if (matchEntities.isEmpty()) {
                return new MatchDomain.MatchListResponse(date, matchList);
            }

            matchList = matchEntities.stream().map(entity -> {

                var matchAt = entity.getMatchAt();
                var awayTeamEntity = entity.getAwayTeamEntity() != null
                        ? teamRepository.findById(entity.getAwayTeamEntity().getId()).orElse(null)
                        : null;
                var homeTeamEntity = entity.getHomeTeamEntity() != null
                        ? teamRepository.findById(entity.getHomeTeamEntity().getId()).orElse(null)
                        : null;
                var stadiumEntity = entity.getStadiumEntity() != null
                        ? stadiumRepository.findById(entity.getStadiumEntity().getId()).orElse(null)
                        : null;

                var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(memberId, entity.getId()).isPresent();

                var awayScore = entity.getAwayScore();
                var homeScore = entity.getHomeScore();

                MatchEnum.ResultType awayResult = awayScore == null ? null
                        : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                MatchEnum.ResultType homeResult = homeScore == null ? null
                        : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

                var awayTeamDto = awayTeamEntity != null
                        ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(), awayScore, awayResult)
                        : new MatchDomain.TeamDto(null, entity.getAwayNm(), awayScore, awayResult);

                var homeTeamDto = homeTeamEntity != null
                        ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(), homeScore, homeResult)
                        : new MatchDomain.TeamDto(null, entity.getHomeNm(), homeScore, homeResult);

                var stadiumName = stadiumEntity != null ? stadiumEntity.getShortName() : "";

                return new MatchDomain.MatchListDto(entity.getId(), matchAt.toLocalDate(),
                        matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumName,
                        entity.getStatus(), entity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)
                                ? entity.getReason() : entity.getStatus().getDesc(),
                        awayTeamDto, homeTeamDto, isWrited);
            }).sorted(Comparator.comparing((MatchDomain.MatchListDto m) -> !isMyTeamMatch(m, teamEntity))).toList();

            return new MatchDomain.MatchListResponse(date, matchList);
        }

        for (Map.Entry<String, Map<String, Object>> entry : matchRedis.entrySet()) {
            Map<String, Object> matchData = entry.getValue();

            // league 필터링
            if (league != null) {
                String matchLeague = (String) matchData.get("league");
                if (matchLeague == null || !league.name().equals(matchLeague)) {
                    continue;
                }
            }

            String id = entry.getKey();
            String time = (String) matchData.get("time");
            String stadium = (String) matchData.get("stadium");
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
            String statusDetail = (String) matchData.get("statusDetail");
            String reason = (String) matchData.get("reason");

            Long awayId = Long.valueOf(String.valueOf(matchData.get("awayId")));
            Long homeId = Long.valueOf(String.valueOf(matchData.get("homeId")));

            Object awayScoreObj = matchData.get("awayScore");
            Object homeScoreObj = matchData.get("homeScore");

            var awayEntity = teamRepository.findById(awayId).orElse(null);
            var homeEntity = teamRepository.findById(homeId).orElse(null);
            var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(memberId, id).isPresent();

            var awayScore = awayScoreObj != null ? Short.valueOf(String.valueOf(awayScoreObj)) : null;
            var homeScore = homeScoreObj != null ? Short.valueOf(String.valueOf(homeScoreObj)) : null;

            var awayResult = status.equals(MatchEnum.MatchStatus.END)
                    ? (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                            : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS)
                    : null;

            var homeResult = status.equals(MatchEnum.MatchStatus.END)
                    ? (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                            : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS)
                    : null;

            var awayTeamDto = awayEntity != null
                    ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(), awayScore, awayResult) : null;

            var homeTeamDto = homeEntity != null
                    ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(), homeScore, homeResult) : null;

            var matchDto = new MatchDomain.MatchListDto(id, date, time, stadium, status,
                    status.equals(MatchEnum.MatchStatus.CANCELED) ? reason : statusDetail, awayTeamDto, homeTeamDto,
                    isWrited);

            matchList.add(matchDto);
        }

        matchList = matchList.stream()
            .sorted(Comparator.comparing((MatchDomain.MatchListDto m) -> !isMyTeamMatch(m, teamEntity)))
            .toList();

        return new MatchDomain.MatchListResponse(date, matchList);
    }

    @Override
    public MatchDomain.MatchInfoResponse findById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new CustomException(MessageEnum.Common.REQUEST_PARAMETER);
        }

        var matchEntity = gameMatchRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        // WBC는 Redis 캐시 사용 불가 → DB 직접 조회
        if (matchEntity.getLeague().equals(MatchEnum.LeagueType.WBC)) {
            return findByIdFromDb(matchEntity);
        }

        var formatDate = id.substring(0, 8);
        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchAt = matchEntity.getMatchAt();
            var awayTeamEntity = matchEntity.getAwayTeamEntity() != null
                    ? teamRepository.findById(matchEntity.getAwayTeamEntity().getId()).orElse(null)
                    : null;
            var homeTeamEntity = matchEntity.getHomeTeamEntity() != null
                    ? teamRepository.findById(matchEntity.getHomeTeamEntity().getId()).orElse(null)
                    : null;
            var stadiumEntity = matchEntity.getStadiumEntity() != null
                    ? stadiumRepository.findById(matchEntity.getStadiumEntity().getId()).orElse(null)
                    : null;

            var awayScore = matchEntity.getAwayScore();
            var homeScore = matchEntity.getHomeScore();

            MatchEnum.ResultType awayResult = awayScore == null ? null
                    : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                            : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
            MatchEnum.ResultType homeResult = homeScore == null ? null
                    : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                            : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

            var awayTeamDto = awayTeamEntity != null
                    ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(), awayScore, awayResult)
                    : new MatchDomain.TeamDto(null, matchEntity.getAwayNm(), awayScore, awayResult);

            var homeTeamDto = homeTeamEntity != null
                    ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(), homeScore, homeResult)
                    : new MatchDomain.TeamDto(null, matchEntity.getHomeNm(), homeScore, homeResult);

            var stadiumDto = stadiumEntity != null
                    ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(),
                            stadiumEntity.getFullName())
                    : null;

            // 취소된 경기는 취소 사유를 statusDetail로 반환
            var statusDetail = matchEntity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)
                    && matchEntity.getReason() != null ? matchEntity.getReason() : matchEntity.getStatus().getDesc();

            return new MatchDomain.MatchInfoResponse(matchEntity.getId(), matchAt.toLocalDate(),
                    matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumDto, matchEntity.getStatus(),
                    statusDetail, awayTeamDto, homeTeamDto);
        }

        var matchData = matchRedis.get(id);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(formatDate, formatter);

        String time = (String) matchData.get("time");
        MatchEnum.MatchStatus status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
        String statusDetail = (String) matchData.get("statusDetail");

        Long awayId = Long.valueOf(String.valueOf(matchData.get("awayId")));
        Long homeId = Long.valueOf(String.valueOf(matchData.get("homeId")));
        Object stadiumIdObj = matchData.get("stadiumId");
        Long stadiumId = stadiumIdObj != null ? Long.valueOf(String.valueOf(stadiumIdObj)) : null;

        Object awayScoreObj = matchData.get("awayScore");
        Object homeScoreObj = matchData.get("homeScore");

        var awayEntity = teamRepository.findById(awayId).orElse(null);
        var homeEntity = teamRepository.findById(homeId).orElse(null);
        var stadiumEntity = stadiumId != null ? stadiumRepository.findById(stadiumId).orElse(null) : null;

        var awayScore = awayScoreObj != null ? Short.valueOf(String.valueOf(awayScoreObj)) : null;
        var homeScore = homeScoreObj != null ? Short.valueOf(String.valueOf(homeScoreObj)) : null;

        var awayResult = status.equals(MatchEnum.MatchStatus.END) ? (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

        var homeResult = status.equals(MatchEnum.MatchStatus.END) ? (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

        var awayTeamDto = awayEntity != null
                ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(), awayScore, awayResult) : null;

        var homeTeamDto = homeEntity != null
                ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(), homeScore, homeResult) : null;

        var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(),
                stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

        return new MatchDomain.MatchInfoResponse(id, date, time, stadiumDto, status, statusDetail, awayTeamDto,
                homeTeamDto);
    }

    @Override
    public MatchDomain.RecordResponse findRecordById(String id) {
        var matchEntity = gameMatchRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var awayTeamEntity = matchEntity.getAwayTeamEntity() != null
                ? teamRepository.findById(matchEntity.getAwayTeamEntity().getId()).orElse(null)
                : null;

        var homeTeamEntity = matchEntity.getHomeTeamEntity() != null
                ? teamRepository.findById(matchEntity.getHomeTeamEntity().getId()).orElse(null)
                : null;

        var awayPitcherRedis = redisHandler.getHashMapList("away_pitcher");
        var homePitcherRedis = redisHandler.getHashMapList("home_pitcher");

        var awayBatterRedis = redisHandler.getHashMapList("away_hitter");
        var homeBatterRedis = redisHandler.getHashMapList("home_hitter");

        var awayPitcherData = awayPitcherRedis.get(id);
        var awayBatterData = awayBatterRedis.get(id);

        var homePitcherData = homePitcherRedis.get(id);
        var homeBatterData = homeBatterRedis.get(id);

        List<MatchDomain.PitcherRecordDto> awayPitchers = new ArrayList<>();
        List<MatchDomain.BatterRecordDto> awayBatters = new ArrayList<>();

        List<MatchDomain.PitcherRecordDto> homePitchers = new ArrayList<>();
        List<MatchDomain.BatterRecordDto> homeBatters = new ArrayList<>();

        List<PitcherRecordEntity> pitcherEntities = Collections.emptyList();
        List<HitterRecordEntity> hitterEntities = Collections.emptyList();

        // redis 에 저장된 데이터가 없으면 DB 조회
        if (((awayPitcherRedis.isEmpty() || awayPitcherData == null)
                && (awayBatterRedis.isEmpty() || awayBatterData == null))
                || ((homePitcherRedis.isEmpty() || homePitcherData == null)
                        && (homeBatterRedis.isEmpty() || homeBatterData == null))) {

            pitcherEntities = pitcherRecordRepository.findByGameMatchEntityId(id);
            hitterEntities = hitterRecordRepository.findByGameMatchEntityId(id);

            var awayPitcherEntities = pitcherEntities.stream().filter(entity -> !entity.getHome()).toList();

            if (!awayPitcherEntities.isEmpty()) {
                awayPitchers = awayPitcherEntities.stream()
                    .map(entity -> new MatchDomain.PitcherRecordDto(entity.getName(), entity.getPosition(),
                            entity.getInning(), entity.getPitching(), entity.getBallFour(), entity.getStrikeOut(),
                            entity.getHit(), entity.getHomeRun(), entity.getScore()))
                    .toList();
            }

            var awayBatterEntities = hitterEntities.stream().filter(entity -> !entity.getHome()).toList();

            if (!awayBatterEntities.isEmpty()) {
                awayBatters = awayBatterEntities.stream()
                    .map(entity -> new MatchDomain.BatterRecordDto(entity.getName(), entity.getPosition(),
                            entity.getTurn(), entity.getHitCount(), entity.getBallFour(), entity.getStrikeOut(),
                            entity.getScore(), entity.getHit(), entity.getHomeRun(), entity.getHitScore()))
                    .toList();
            }

            var homePitcherEntities = pitcherEntities.stream().filter(entity -> entity.getHome()).toList();

            if (!homePitcherEntities.isEmpty()) {
                homePitchers = homePitcherEntities.stream()
                    .map(entity -> new MatchDomain.PitcherRecordDto(entity.getName(), entity.getPosition(),
                            entity.getInning(), entity.getPitching(), entity.getBallFour(), entity.getStrikeOut(),
                            entity.getHit(), entity.getHomeRun(), entity.getScore()))
                    .toList();
            }

            var homeBatterEntities = hitterEntities.stream().filter(entity -> entity.getHome()).toList();

            if (!homeBatterEntities.isEmpty()) {
                homeBatters = homeBatterEntities.stream()
                    .map(entity -> new MatchDomain.BatterRecordDto(entity.getName(), entity.getPosition(),
                            entity.getTurn(), entity.getHitCount(), entity.getBallFour(), entity.getStrikeOut(),
                            entity.getScore(), entity.getHit(), entity.getHomeRun(), entity.getHitScore()))
                    .toList();
            }
        }
        else {
            // redis 데이터 사용
            ObjectMapper objectMapper = new ObjectMapper();
            var awayPitcherObj = awayPitcherData.stream()
                .map(data -> objectMapper.convertValue(data, MatchDomain.PitcherRecordData.class))
                .toList();

            var awayBatterObj = awayBatterData.stream()
                .map(data -> objectMapper.convertValue(data, MatchDomain.BatterRecordData.class))
                .toList();

            var homePitcherObj = homePitcherData.stream()
                .map(data -> objectMapper.convertValue(data, MatchDomain.PitcherRecordData.class))
                .toList();

            var homeBatterObj = homeBatterData.stream()
                .map(data -> objectMapper.convertValue(data, MatchDomain.BatterRecordData.class))
                .toList();

            if (!awayPitcherObj.isEmpty()) {
                awayPitchers = awayPitcherObj.stream()
                    .map(data -> new MatchDomain.PitcherRecordDto(data.name(), data.position(), data.inning(),
                            data.pitching(), data.ballFour(), data.strikeOut(), data.hit(), data.homeRun(),
                            data.score()))
                    .toList();
            }

            if (!awayBatterObj.isEmpty()) {
                awayBatters = awayBatterObj.stream()
                    .map(data -> new MatchDomain.BatterRecordDto(data.name(), data.position(), data.turn(),
                            data.hitCount(), data.ballFour(), data.strikeOut(), data.score(), data.hit(),
                            data.homeRun(), data.hitScore()))
                    .toList();
            }

            if (!homePitcherObj.isEmpty()) {
                homePitchers = homePitcherObj.stream()
                    .map(data -> new MatchDomain.PitcherRecordDto(data.name(), data.position(), data.inning(),
                            data.pitching(), data.ballFour(), data.strikeOut(), data.hit(), data.homeRun(),
                            data.score()))
                    .toList();
            }

            if (!homeBatterObj.isEmpty()) {
                homeBatters = homeBatterObj.stream()
                    .map(data -> new MatchDomain.BatterRecordDto(data.name(), data.position(), data.turn(),
                            data.hitCount(), data.ballFour(), data.strikeOut(), data.score(), data.hit(),
                            data.homeRun(), data.hitScore()))
                    .toList();
            }
        }

        var awayTeamName = awayTeamEntity != null ? awayTeamEntity.getName() : matchEntity.getAwayNm();
        var homeTeamName = homeTeamEntity != null ? homeTeamEntity.getName() : matchEntity.getHomeNm();

        var awayTeamDto = new MatchDomain.TeamRecordDto(awayTeamName, awayPitchers, awayBatters);
        var homeTeamDto = new MatchDomain.TeamRecordDto(homeTeamName, homePitchers, homeBatters);

        return new MatchDomain.RecordResponse(matchEntity.getMatchAt(), awayTeamDto, homeTeamDto);
    }

    @Override
    public List<MatchDomain.InterestTeamMatchInfoResponse> findByTeam() {
        var id = RequestUtils.getId();

        if (id == null) {
            return new ArrayList<>();
        }

        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        if (memberInfoEntity.getTeamEntity() == null) {
            throw new CustomException(MessageEnum.Data.NO_INTEREST_TEAM);
        }

        var now = LocalDate.now();
        var formatDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        var matchEntity = gameMatchCustomRepository.findByTeamIdIn(memberInfoEntity.getTeamEntity().getId(), now);

        if (matchEntity.isEmpty()) {
            return new ArrayList<>();
        }

        if (matchRedis.isEmpty()) {
            return matchEntity.stream().map(entity -> {
                var matchAt = entity.getMatchAt();
                var awayTeamEntity = entity.getAwayTeamEntity() != null
                        ? teamRepository.findById(entity.getAwayTeamEntity().getId()).orElse(null)
                        : null;
                var homeTeamEntity = entity.getHomeTeamEntity() != null
                        ? teamRepository.findById(entity.getHomeTeamEntity().getId()).orElse(null)
                        : null;
                var stadiumEntity = entity.getStadiumEntity() != null
                        ? stadiumRepository.findById(entity.getStadiumEntity().getId()).orElse(null)
                        : null;
                var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(id, entity.getId()).isPresent();

                var awayScore = entity.getAwayScore();
                var homeScore = entity.getHomeScore();

                MatchEnum.ResultType awayResult = awayScore == null ? null
                        : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                MatchEnum.ResultType homeResult = homeScore == null ? null
                        : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

                var awayTeamDto = awayTeamEntity != null
                        ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(), awayScore, awayResult)
                        : new MatchDomain.TeamDto(null, entity.getAwayNm(), awayScore, awayResult);

                var homeTeamDto = homeTeamEntity != null
                        ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(), homeScore, homeResult)
                        : new MatchDomain.TeamDto(null, entity.getHomeNm(), homeScore, homeResult);

                var stadiumDto = stadiumEntity != null
                        ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(),
                                stadiumEntity.getFullName())
                        : null;
                return new MatchDomain.InterestTeamMatchInfoResponse(entity.getId(), matchAt.toLocalDate(),
                        matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumDto, entity.getStatus(),
                        entity.getStatus().getDesc(), awayTeamDto, homeTeamDto, isWrited);
            }).toList();
        }

        return matchEntity.stream().map(entity -> {

            var matchData = matchRedis.get(entity.getId());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate date = LocalDate.parse(formatDate, formatter);

            String time = (String) matchData.get("time");
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
            String statusDetail = (String) matchData.get("statusDetail");

            Long awayId = Long.valueOf(String.valueOf(matchData.get("awayId")));
            Long homeId = Long.valueOf(String.valueOf(matchData.get("homeId")));
            Long stadiumId = Long.valueOf(String.valueOf(matchData.get("stadiumId")));

            Object awayScoreObj = matchData.get("awayScore");
            Object homeScoreObj = matchData.get("homeScore");

            var awayEntity = teamRepository.findById(awayId).orElse(null);
            var homeEntity = teamRepository.findById(homeId).orElse(null);
            var stadiumEntity = stadiumRepository.findById(stadiumId).orElse(null);
            var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(id, entity.getId()).isPresent();

            var awayScore = awayScoreObj != null ? Short.valueOf(String.valueOf(awayScoreObj)) : null;
            var homeScore = homeScoreObj != null ? Short.valueOf(String.valueOf(homeScoreObj)) : null;

            var awayResult = status.equals(MatchEnum.MatchStatus.END)
                    ? (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                            : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS)
                    : null;

            var homeResult = status.equals(MatchEnum.MatchStatus.END)
                    ? (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                            : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS)
                    : null;

            var awayTeamDto = awayEntity != null
                    ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(), awayScore, awayResult) : null;

            var homeTeamDto = homeEntity != null
                    ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(), homeScore, homeResult) : null;

            var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(),
                    stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

            return new MatchDomain.InterestTeamMatchInfoResponse(entity.getId(), date, time, stadiumDto, status,
                    statusDetail, awayTeamDto, homeTeamDto, isWrited);
        }).toList();
    }

    @Override
    public MatchDomain.TodayMatchListResponse findTodayMatch() {
        var date = LocalDate.now();
        var memberId = RequestUtils.getId();
        var formatDate = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        var teamEntity = Optional.ofNullable(memberId)
            .flatMap(memberInfoRepository::findByMemberEntity_Id)
            .map(MemberInfoEntity::getTeamEntity)
            .orElse(null);

        List<MatchDomain.MatchListDto> matchList = new ArrayList();

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntities = gameMatchCustomRepository.findByMatchAt(date)
                .stream()
                .sorted(Comparator.comparing(entity -> entity.getMatchAt()))
                .toList();

            if (matchEntities.isEmpty()) {
                return new MatchDomain.TodayMatchListResponse(matchList);
            }

            matchList = matchEntities.stream().map(entity -> {

                var matchAt = entity.getMatchAt();
                var awayTeamEntity = entity.getAwayTeamEntity() != null
                        ? teamRepository.findById(entity.getAwayTeamEntity().getId()).orElse(null)
                        : null;
                var homeTeamEntity = entity.getHomeTeamEntity() != null
                        ? teamRepository.findById(entity.getHomeTeamEntity().getId()).orElse(null)
                        : null;
                var stadiumEntity = entity.getStadiumEntity() != null
                        ? stadiumRepository.findById(entity.getStadiumEntity().getId()).orElse(null)
                        : null;

                var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(memberId, entity.getId()).isPresent();

                var awayScore = entity.getAwayScore();
                var homeScore = entity.getHomeScore();

                MatchEnum.ResultType awayResult = awayScore == null ? null
                        : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                MatchEnum.ResultType homeResult = homeScore == null ? null
                        : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

                var awayTeamDto = awayTeamEntity != null
                        ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(), awayScore, awayResult)
                        : new MatchDomain.TeamDto(null, entity.getAwayNm(), awayScore, awayResult);

                var homeTeamDto = homeTeamEntity != null
                        ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(), homeScore, homeResult)
                        : new MatchDomain.TeamDto(null, entity.getHomeNm(), homeScore, homeResult);

                var stadiumName = stadiumEntity != null ? stadiumEntity.getShortName() : "";

                return new MatchDomain.MatchListDto(entity.getId(), matchAt.toLocalDate(),
                        matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumName,
                        entity.getStatus(), entity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)
                                ? entity.getReason() : entity.getStatus().getDesc(),
                        awayTeamDto, homeTeamDto, isWrited);
            }).sorted(Comparator.comparing((MatchDomain.MatchListDto m) -> !isMyTeamMatch(m, teamEntity))).toList();

            return new MatchDomain.TodayMatchListResponse(matchList);
        }

        for (Map.Entry<String, Map<String, Object>> entry : matchRedis.entrySet()) {
            Map<String, Object> matchData = entry.getValue();

            String id = entry.getKey();
            String time = (String) matchData.get("time");
            String stadium = (String) matchData.get("stadium");
            MatchEnum.MatchStatus status = MatchEnum.MatchStatus.valueOf((String) matchData.get("status"));
            String statusDetail = (String) matchData.get("statusDetail");
            String reason = (String) matchData.get("reason");

            Long awayId = Long.valueOf(String.valueOf(matchData.get("awayId")));
            Long homeId = Long.valueOf(String.valueOf(matchData.get("homeId")));

            Object awayScoreObj = matchData.get("awayScore");
            Object homeScoreObj = matchData.get("homeScore");

            var awayEntity = teamRepository.findById(awayId).orElse(null);
            var homeEntity = teamRepository.findById(homeId).orElse(null);
            var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(memberId, id).isPresent();

            var awayScore = awayScoreObj != null ? Short.valueOf(String.valueOf(awayScoreObj)) : null;
            var homeScore = homeScoreObj != null ? Short.valueOf(String.valueOf(homeScoreObj)) : null;

            var awayResult = status.equals(MatchEnum.MatchStatus.END)
                    ? (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                            : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS)
                    : null;

            var homeResult = status.equals(MatchEnum.MatchStatus.END)
                    ? (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                            : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS)
                    : null;

            var awayTeamDto = awayEntity != null
                    ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(), awayScore, awayResult) : null;

            var homeTeamDto = homeEntity != null
                    ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(), homeScore, homeResult) : null;

            var matchDto = new MatchDomain.MatchListDto(id, date, time, stadium, status,
                    status.equals(MatchEnum.MatchStatus.CANCELED) ? reason : statusDetail, awayTeamDto, homeTeamDto,
                    isWrited);

            matchList.add(matchDto);
        }

        matchList = matchList.stream()
            .sorted(Comparator.comparing((MatchDomain.MatchListDto m) -> !isMyTeamMatch(m, teamEntity)))
            .toList();

        return new MatchDomain.TodayMatchListResponse(matchList);
    }

    private boolean isMyTeamMatch(MatchDomain.MatchListDto match, TeamEntity teamEntity) {
        if (teamEntity == null) {
            return false;
        }
        Long myTeamId = teamEntity.getId();
        boolean awayMatches = match.awayTeam() != null && myTeamId.equals(match.awayTeam().id());
        boolean homeMatches = match.homeTeam() != null && myTeamId.equals(match.homeTeam().id());
        return awayMatches || homeMatches;
    }

    private MatchDomain.MatchInfoResponse findByIdFromDb(GameMatchEntity matchEntity) {
        var matchAt = matchEntity.getMatchAt();
        var awayTeamEntity = matchEntity.getAwayTeamEntity() != null
                ? teamRepository.findById(matchEntity.getAwayTeamEntity().getId()).orElse(null)
                : null;
        var homeTeamEntity = matchEntity.getHomeTeamEntity() != null
                ? teamRepository.findById(matchEntity.getHomeTeamEntity().getId()).orElse(null)
                : null;
        var stadiumEntity = matchEntity.getStadiumEntity() != null
                ? stadiumRepository.findById(matchEntity.getStadiumEntity().getId()).orElse(null)
                : null;

        var awayScore = matchEntity.getAwayScore();
        var homeScore = matchEntity.getHomeScore();

        MatchEnum.ResultType awayResult = awayScore == null ? null
                : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                        : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
        MatchEnum.ResultType homeResult = homeScore == null ? null
                : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                        : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

        var awayTeamDto = awayTeamEntity != null
                ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(), awayScore, awayResult)
                : new MatchDomain.TeamDto(null, matchEntity.getAwayNm(), awayScore, awayResult);

        var homeTeamDto = homeTeamEntity != null
                ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(), homeScore, homeResult)
                : new MatchDomain.TeamDto(null, matchEntity.getHomeNm(), homeScore, homeResult);

        var stadiumDto = stadiumEntity != null
                ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(),
                        stadiumEntity.getFullName())
                : null;

        var statusDetail = matchEntity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)
                && matchEntity.getReason() != null ? matchEntity.getReason() : matchEntity.getStatus().getDesc();

        return new MatchDomain.MatchInfoResponse(matchEntity.getId(), matchAt.toLocalDate(),
                matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumDto, matchEntity.getStatus(),
                statusDetail, awayTeamDto, homeTeamDto);
    }

}
