package kr.co.victoryfairy.game.application;

import tools.jackson.databind.ObjectMapper;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.presentation.MatchDomain;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.HitterRecord;
import kr.co.victoryfairy.game.domain.PitcherRecord;
import kr.co.victoryfairy.game.domain.Stadium;
import kr.co.victoryfairy.game.domain.Team;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.GameRecordRepository;
import kr.co.victoryfairy.game.domain.GameUserReader;
import kr.co.victoryfairy.game.domain.StadiumReader;
import kr.co.victoryfairy.game.domain.TeamReader;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameQueryService {

    private final TeamReader teamRepository;

    private final StadiumReader stadiumRepository;

    private final GameMatchRepository gameMatchRepository;

    private final GameRecordRepository recordRepository;

    private final GameUserReader gameUserReader;

    private final RedisHandler redisHandler;

    public MatchDomain.MatchListResponse findList(LocalDate date) {
        return findList(date, null);
    }

    public MatchDomain.MatchListResponse findList(LocalDate date, MatchEnum.LeagueType league) {
        var memberId = CurrentRequest.getId();

        var teamEntity = Optional.ofNullable(memberId).flatMap(gameUserReader::preferredTeamId).orElse(null);

        var formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 당일 경기 경우 redis 에서 가져오기
        List<MatchDomain.MatchListDto> matchList = new ArrayList();

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntities = gameMatchRepository.findByDate(date, league);
            log.debug("findList - date: {}, league: {}, matchEntities size: {}", date, league, matchEntities.size());
            matchEntities = matchEntities.stream().sorted(Comparator.comparing(entity -> entity.getMatchAt())).toList();

            if (matchEntities.isEmpty()) {
                return new MatchDomain.MatchListResponse(date, matchList);
            }

            var teamsById = findTeamsById(matchEntities.stream()
                .flatMap(match -> Stream.of(match.getAwayTeamId(), match.getHomeTeamId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
            var stadiumsById = findStadiumsById(matchEntities.stream()
                .map(GameMatch::getStadiumId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
            var diaryIdsByMatchId = findDiaryIdsByMatchId(memberId,
                    matchEntities.stream().map(GameMatch::getId).toList());

            matchList = matchEntities.stream().map(entity -> {

                var matchAt = entity.getMatchAt();
                var awayTeamEntity = entity.getAwayTeamId() != null ? teamsById.get(entity.getAwayTeamId()) : null;
                var homeTeamEntity = entity.getHomeTeamId() != null ? teamsById.get(entity.getHomeTeamId()) : null;
                var stadiumEntity = entity.getStadiumId() != null ? stadiumsById.get(entity.getStadiumId()) : null;

                var diaryId = diaryIdsByMatchId.get(entity.getId());
                var isWrited = diaryId != null;

                var awayScore = entity.getAwayScore();
                var homeScore = entity.getHomeScore();

                MatchEnum.ResultType awayResult = awayScore == null ? null
                        : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                MatchEnum.ResultType homeResult = homeScore == null ? null
                        : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

                var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(),
                        awayTeamEntity.getName(), awayScore, awayResult)
                        : new MatchDomain.TeamDto(null, entity.getAwayNm(), awayScore, awayResult);

                var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(),
                        homeTeamEntity.getName(), homeScore, homeResult)
                        : new MatchDomain.TeamDto(null, entity.getHomeNm(), homeScore, homeResult);

                var stadiumName = stadiumEntity != null ? stadiumEntity.getShortName() : "";

                return new MatchDomain.MatchListDto(entity.getId(), matchAt.toLocalDate(),
                        matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumName, entity.getStatus(),
                        entity.getStatus().equals(MatchEnum.MatchStatus.CANCELED) ? entity.getReason()
                                : entity.getStatus().getDesc(),
                        awayTeamDto, homeTeamDto, isWrited, diaryId, entity.getLeague());
            }).sorted(Comparator.comparing((MatchDomain.MatchListDto m) -> !isMyTeamMatch(m, teamEntity))).toList();

            return new MatchDomain.MatchListResponse(date, matchList);
        }

        var matchEntries = matchRedis.entrySet()
            .stream()
            .filter(entry -> league == null || league.name().equals(entry.getValue().get("league")))
            .toList();
        var teamsById = findTeamsById(matchEntries.stream()
            .flatMap(entry -> Stream.of(Long.valueOf(String.valueOf(entry.getValue().get("awayId"))),
                    Long.valueOf(String.valueOf(entry.getValue().get("homeId")))))
            .collect(Collectors.toSet()));
        var diaryIdsByMatchId = findDiaryIdsByMatchId(memberId, matchEntries.stream().map(Map.Entry::getKey).toList());

        for (Map.Entry<String, Map<String, Object>> entry : matchEntries) {
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

            var awayEntity = teamsById.get(awayId);
            var homeEntity = teamsById.get(homeId);
            var diaryId = diaryIdsByMatchId.get(id);
            var isWrited = diaryId != null;

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

            String matchLeague = (String) matchData.get("league");
            MatchEnum.LeagueType leagueType = matchLeague != null ? MatchEnum.LeagueType.valueOf(matchLeague) : null;

            var matchDto = new MatchDomain.MatchListDto(id, date, time, stadium, status,
                    status.equals(MatchEnum.MatchStatus.CANCELED) ? reason : status.getDesc(), awayTeamDto, homeTeamDto,
                    isWrited, diaryId, leagueType);

            matchList.add(matchDto);
        }

        matchList = matchList.stream()
            .sorted(Comparator.comparing((MatchDomain.MatchListDto m) -> m.time())
                .thenComparing(m -> !isMyTeamMatch(m, teamEntity)))
            .toList();

        return new MatchDomain.MatchListResponse(date, matchList);
    }

    private Map<Long, Team> findTeamsById(Collection<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }

        return teamRepository.findAllById(teamIds).stream().collect(Collectors.toMap(Team::getId, team -> team));
    }

    private Map<Long, Stadium> findStadiumsById(Collection<Long> stadiumIds) {
        if (stadiumIds.isEmpty()) {
            return Map.of();
        }

        return stadiumRepository.findAllById(stadiumIds)
            .stream()
            .collect(Collectors.toMap(Stadium::getId, stadium -> stadium));
    }

    private Map<String, Long> findDiaryIdsByMatchId(Long memberId, Collection<String> matchIds) {
        if (memberId == null || matchIds.isEmpty()) {
            return Map.of();
        }

        return Optional.ofNullable(gameUserReader.diaryIdsByMatchId(memberId, matchIds)).orElseGet(Map::of);
    }

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
            var awayTeamEntity = matchEntity.getAwayTeamId() != null
                    ? teamRepository.findById(matchEntity.getAwayTeamId()).orElse(null) : null;
            var homeTeamEntity = matchEntity.getHomeTeamId() != null
                    ? teamRepository.findById(matchEntity.getHomeTeamId()).orElse(null) : null;
            var stadiumEntity = matchEntity.getStadiumId() != null
                    ? stadiumRepository.findById(matchEntity.getStadiumId()).orElse(null) : null;

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

            var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(),
                    stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

            // 취소된 경기는 취소 사유를 statusDetail로 반환
            var statusDetail = matchEntity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)
                    && matchEntity.getReason() != null ? matchEntity.getReason() : matchEntity.getStatus().getDesc();

            return new MatchDomain.MatchInfoResponse(matchEntity.getId(), matchAt.toLocalDate(),
                    matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumDto, matchEntity.getStatus(),
                    statusDetail, awayTeamDto, homeTeamDto, matchEntity.getLeague());
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
                homeTeamDto, matchEntity.getLeague());
    }

    public MatchDomain.RecordResponse findRecordById(String id) {
        var matchEntity = gameMatchRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var awayTeamEntity = matchEntity.getAwayTeamId() != null
                ? teamRepository.findById(matchEntity.getAwayTeamId()).orElse(null) : null;

        var homeTeamEntity = matchEntity.getHomeTeamId() != null
                ? teamRepository.findById(matchEntity.getHomeTeamId()).orElse(null) : null;

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

        List<PitcherRecord> pitcherEntities = Collections.emptyList();
        List<HitterRecord> hitterEntities = Collections.emptyList();

        // redis 에 저장된 데이터가 없으면 DB 조회
        if (((awayPitcherRedis.isEmpty() || awayPitcherData == null)
                && (awayBatterRedis.isEmpty() || awayBatterData == null))
                || ((homePitcherRedis.isEmpty() || homePitcherData == null)
                        && (homeBatterRedis.isEmpty() || homeBatterData == null))) {

            pitcherEntities = recordRepository.findPitchers(id);
            hitterEntities = recordRepository.findHitters(id);

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

    public List<MatchDomain.InterestTeamMatchInfoResponse> findByTeam() {
        var id = CurrentRequest.getId();

        if (id == null) {
            return new ArrayList<>();
        }

        var context = gameUserReader.context(id);
        if (!context.memberExists() || !context.profileExists()) {
            throw new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
        }
        if (context.preferredTeamId() == null) {
            throw new CustomException(MessageEnum.Data.NO_INTEREST_TEAM);
        }

        var now = LocalDate.now();
        var formatDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        var matchEntity = gameMatchRepository.findAllByTeam(context.preferredTeamId(), now);

        if (matchEntity.isEmpty()) {
            return new ArrayList<>();
        }

        if (matchRedis.isEmpty()) {
            return matchEntity.stream().map(entity -> {
                var matchAt = entity.getMatchAt();
                var awayTeamEntity = entity.getAwayTeamId() != null
                        ? teamRepository.findById(entity.getAwayTeamId()).orElse(null) : null;
                var homeTeamEntity = entity.getHomeTeamId() != null
                        ? teamRepository.findById(entity.getHomeTeamId()).orElse(null) : null;
                var stadiumEntity = entity.getStadiumId() != null
                        ? stadiumRepository.findById(entity.getStadiumId()).orElse(null) : null;
                var isWrited = gameUserReader.diaryIdsByMatchId(id, List.of(entity.getId()))
                    .containsKey(entity.getId());

                var awayScore = entity.getAwayScore();
                var homeScore = entity.getHomeScore();

                MatchEnum.ResultType awayResult = awayScore == null ? null
                        : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                MatchEnum.ResultType homeResult = homeScore == null ? null
                        : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

                var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(),
                        awayTeamEntity.getName(), awayScore, awayResult)
                        : new MatchDomain.TeamDto(null, entity.getAwayNm(), awayScore, awayResult);

                var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(),
                        homeTeamEntity.getName(), homeScore, homeResult)
                        : new MatchDomain.TeamDto(null, entity.getHomeNm(), homeScore, homeResult);

                var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(),
                        stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;
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
            var isWrited = gameUserReader.diaryIdsByMatchId(id, List.of(entity.getId())).containsKey(entity.getId());

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

    public MatchDomain.TodayMatchListResponse findTodayMatch() {
        var date = LocalDate.now();
        var memberId = CurrentRequest.getId();
        var formatDate = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        var teamEntity = Optional.ofNullable(memberId).flatMap(gameUserReader::preferredTeamId).orElse(null);

        List<MatchDomain.MatchListDto> matchList = new ArrayList();

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntities = gameMatchRepository.findByDate(date)
                .stream()
                .sorted(Comparator.comparing(entity -> entity.getMatchAt()))
                .toList();

            if (matchEntities.isEmpty()) {
                return new MatchDomain.TodayMatchListResponse(matchList);
            }

            matchList = matchEntities.stream().map(entity -> {

                var matchAt = entity.getMatchAt();
                var awayTeamEntity = entity.getAwayTeamId() != null
                        ? teamRepository.findById(entity.getAwayTeamId()).orElse(null) : null;
                var homeTeamEntity = entity.getHomeTeamId() != null
                        ? teamRepository.findById(entity.getHomeTeamId()).orElse(null) : null;
                var stadiumEntity = entity.getStadiumId() != null
                        ? stadiumRepository.findById(entity.getStadiumId()).orElse(null) : null;

                var diaryId = gameUserReader.diaryIdsByMatchId(memberId, List.of(entity.getId())).get(entity.getId());
                var isWrited = diaryId != null;

                var awayScore = entity.getAwayScore();
                var homeScore = entity.getHomeScore();

                MatchEnum.ResultType awayResult = awayScore == null ? null
                        : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                MatchEnum.ResultType homeResult = homeScore == null ? null
                        : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

                var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(),
                        awayTeamEntity.getName(), awayScore, awayResult)
                        : new MatchDomain.TeamDto(null, entity.getAwayNm(), awayScore, awayResult);

                var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(),
                        homeTeamEntity.getName(), homeScore, homeResult)
                        : new MatchDomain.TeamDto(null, entity.getHomeNm(), homeScore, homeResult);

                var stadiumName = stadiumEntity != null ? stadiumEntity.getShortName() : "";

                return new MatchDomain.MatchListDto(entity.getId(), matchAt.toLocalDate(),
                        matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumName, entity.getStatus(),
                        entity.getStatus().equals(MatchEnum.MatchStatus.CANCELED) ? entity.getReason()
                                : entity.getStatus().getDesc(),
                        awayTeamDto, homeTeamDto, isWrited, diaryId, entity.getLeague());
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
            var diaryId = gameUserReader.diaryIdsByMatchId(memberId, List.of(id)).get(id);
            var isWrited = diaryId != null;

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

            String matchLeague = (String) matchData.get("league");
            MatchEnum.LeagueType todayLeagueType = matchLeague != null ? MatchEnum.LeagueType.valueOf(matchLeague)
                    : null;

            var matchDto = new MatchDomain.MatchListDto(id, date, time, stadium, status,
                    status.equals(MatchEnum.MatchStatus.CANCELED) ? reason : statusDetail, awayTeamDto, homeTeamDto,
                    isWrited, diaryId, todayLeagueType);

            matchList.add(matchDto);
        }

        matchList = matchList.stream()
            .sorted(Comparator.comparing((MatchDomain.MatchListDto m) -> !isMyTeamMatch(m, teamEntity)))
            .toList();

        return new MatchDomain.TodayMatchListResponse(matchList);
    }

    private boolean isMyTeamMatch(MatchDomain.MatchListDto match, Long teamId) {
        if (teamId == null) {
            return false;
        }
        Long myTeamId = teamId;
        boolean awayMatches = match.awayTeam() != null && myTeamId.equals(match.awayTeam().id());
        boolean homeMatches = match.homeTeam() != null && myTeamId.equals(match.homeTeam().id());
        return awayMatches || homeMatches;
    }

    private MatchDomain.MatchInfoResponse findByIdFromDb(GameMatch matchEntity) {
        var matchAt = matchEntity.getMatchAt();
        var awayTeamEntity = matchEntity.getAwayTeamId() != null
                ? teamRepository.findById(matchEntity.getAwayTeamId()).orElse(null) : null;
        var homeTeamEntity = matchEntity.getHomeTeamId() != null
                ? teamRepository.findById(matchEntity.getHomeTeamId()).orElse(null) : null;
        var stadiumEntity = matchEntity.getStadiumId() != null
                ? stadiumRepository.findById(matchEntity.getStadiumId()).orElse(null) : null;

        var awayScore = matchEntity.getAwayScore();
        var homeScore = matchEntity.getHomeScore();

        MatchEnum.ResultType awayResult = awayScore == null ? null : (awayScore == homeScore ? MatchEnum.ResultType.DRAW
                : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
        MatchEnum.ResultType homeResult = homeScore == null ? null : (homeScore == awayScore ? MatchEnum.ResultType.DRAW
                : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

        var awayTeamDto = awayTeamEntity != null
                ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(), awayScore, awayResult)
                : new MatchDomain.TeamDto(null, matchEntity.getAwayNm(), awayScore, awayResult);

        var homeTeamDto = homeTeamEntity != null
                ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(), homeScore, homeResult)
                : new MatchDomain.TeamDto(null, matchEntity.getHomeNm(), homeScore, homeResult);

        var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(),
                stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

        var statusDetail = matchEntity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)
                && matchEntity.getReason() != null ? matchEntity.getReason() : matchEntity.getStatus().getDesc();

        return new MatchDomain.MatchInfoResponse(matchEntity.getId(), matchAt.toLocalDate(),
                matchAt.format(DateTimeFormatter.ofPattern("HH:mm")), stadiumDto, matchEntity.getStatus(), statusDetail,
                awayTeamDto, homeTeamDto, matchEntity.getLeague());
    }

}
