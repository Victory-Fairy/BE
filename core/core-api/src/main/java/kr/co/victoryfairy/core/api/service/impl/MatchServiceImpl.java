package kr.co.victoryfairy.core.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.service.MatchService;
import kr.co.victoryfairy.storage.db.core.entity.HitterRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.entity.PitcherRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.handler.RedisHandler;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        var memberId = RequestUtils.getId();

        var teamEntity = Optional.ofNullable(memberId)
                .flatMap(memberRepository::findById)
                .flatMap(memberInfoRepository::findByMemberEntity)
                .map(MemberInfoEntity::getTeamEntity)
                .orElse(null);

        var formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 당일 경기 경우 redis 에서 가져오기
        List<MatchDomain.MatchListDto> matchList = new ArrayList();

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntities = gameMatchCustomRepository.findByMatchAt(date).stream()
                    .sorted(Comparator.comparing(entity -> entity.getMatchAt()))
                    .toList();

            if (matchEntities.isEmpty()) {
                return new MatchDomain.MatchListResponse(date, matchList);
            }

            matchList = matchEntities.stream()
                    .map(entity -> {

                        var matchAt = entity.getMatchAt();
                        var awayTeamEntity = teamRepository.findById(entity.getAwayTeamEntity().getId()).orElse(null);
                        var homeTeamEntity = teamRepository.findById(entity.getHomeTeamEntity().getId()).orElse(null);
                        var stadiumEntity = stadiumRepository.findById(entity.getStadiumEntity().getId()).orElse(null);

                        var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(memberId, entity.getId()).isPresent();

                        var awayScore = entity.getAwayScore();
                        var homeScore = entity.getHomeScore();

                        MatchEnum.ResultType awayResult = awayScore == null ? null :
                                (awayScore == homeScore ? MatchEnum.ResultType.DRAW :
                                (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                        MatchEnum.ResultType homeResult = homeScore == null ? null :
                                (homeScore == awayScore ? MatchEnum.ResultType.DRAW :
                                (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);


                        var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(),
                                awayScore, awayResult) : null;

                        var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(),
                                homeScore, homeResult) : null;

                        return new MatchDomain.MatchListDto(
                                        entity.getId(),
                                        matchAt.toLocalDate(),
                                        matchAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                                        stadiumEntity.getShortName(),
                                        entity.getStatus(),
                                        entity.getStatus().equals(MatchEnum.MatchStatus.CANCELED) ? entity.getReason() : entity.getStatus().getDesc(),
                                        awayTeamDto,
                                        homeTeamDto,
                                        isWrited
                        );
                    })
                    .sorted((m1, m2) -> {
                        boolean m1HasTeam = (teamEntity != null && m1.awayTeam() != null && m1.awayTeam().id() == teamEntity.getId()) ||
                                (teamEntity != null && m1.homeTeam() != null && m1.homeTeam().id() == teamEntity.getId());
                        boolean m2HasTeam = (teamEntity != null && m2.awayTeam() != null && m2.awayTeam().id() == teamEntity.getId()) ||
                                (teamEntity != null && m2.homeTeam() != null && m2.homeTeam().id() == teamEntity.getId());
                        return Boolean.compare(!m1HasTeam, !m2HasTeam);  // true가 뒤로 가도록 정렬
                    })
                    .toList();

            return new MatchDomain.MatchListResponse(date, matchList);
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

            var awayResult = status.equals(MatchEnum.MatchStatus.END) ?
                    (awayScore == homeScore ? MatchEnum.ResultType.DRAW : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

            var homeResult = status.equals(MatchEnum.MatchStatus.END) ?
                    (homeScore == awayScore ? MatchEnum.ResultType.DRAW : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

            var awayTeamDto = awayEntity != null ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(),
                    awayScore, awayResult) : null;

            var homeTeamDto = homeEntity != null ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(),
                    homeScore, homeResult) : null;

            var matchDto = new MatchDomain.MatchListDto(
                    id,
                    date,
                    time,
                    stadium,
                    status,
                    status.equals(MatchEnum.MatchStatus.CANCELED) ? reason : statusDetail,
                    awayTeamDto,
                    homeTeamDto,
                    isWrited
            );

            matchList.add(matchDto);
        }

        if (teamEntity != null) {
            matchList = matchList.stream().sorted((m1, m2) -> {
                        boolean m1HasTeam = (teamEntity != null && m1.awayTeam() != null && m1.awayTeam().id() == teamEntity.getId()) ||
                                (teamEntity != null && m1.homeTeam() != null && m1.homeTeam().id() == teamEntity.getId());
                        boolean m2HasTeam = (teamEntity != null && m2.awayTeam() != null && m2.awayTeam().id() == teamEntity.getId()) ||
                                (teamEntity != null && m2.homeTeam() != null && m2.homeTeam().id() == teamEntity.getId());
                        return Boolean.compare(!m1HasTeam, !m2HasTeam);  // true가 뒤로 가도록 정렬
                    }).toList();
        }

        return new MatchDomain.MatchListResponse(date, matchList);
    }

    @Override
    public MatchDomain.MatchInfoResponse findById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new CustomException(MessageEnum.Common.REQUEST_PARAMETER);
        }

        var formatDate = id.substring(0, 8);

        var matchRedis = redisHandler.getHashMap(formatDate + "_match_list");

        if (matchRedis.isEmpty()) {
            var matchEntity = gameMatchRepository.findById(id)
                    .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));


            var matchAt = matchEntity.getMatchAt();
            var awayTeamEntity = teamRepository.findById(matchEntity.getAwayTeamEntity().getId()).orElse(null);
            var homeTeamEntity = teamRepository.findById(matchEntity.getHomeTeamEntity().getId()).orElse(null);
            var stadiumEntity = stadiumRepository.findById(matchEntity.getStadiumEntity().getId()).orElse(null);

            var awayScore = matchEntity.getAwayScore();
            var homeScore = matchEntity.getHomeScore();

            MatchEnum.ResultType awayResult = awayScore == null ? null :
                    (awayScore == homeScore ? MatchEnum.ResultType.DRAW :
                            (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
            MatchEnum.ResultType homeResult = homeScore == null ? null :
                    (homeScore == awayScore ? MatchEnum.ResultType.DRAW :
                            (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);


            var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(),
                    awayScore, awayResult) : null;

            var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(),
                    homeScore, homeResult) : null;

            var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

            return new MatchDomain.MatchInfoResponse(
                    matchEntity.getId(),
                    matchAt.toLocalDate(),
                    matchAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                    stadiumDto,
                    matchEntity.getStatus(),
                    matchEntity.getStatus().getDesc(),
                    awayTeamDto,
                    homeTeamDto);
        }

        var matchData = matchRedis.get(id);
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

        var awayScore = awayScoreObj != null ? Short.valueOf(String.valueOf(awayScoreObj)) : null;
        var homeScore = homeScoreObj != null ? Short.valueOf(String.valueOf(homeScoreObj)) : null;

        var awayResult = status.equals(MatchEnum.MatchStatus.END) ?
                (awayScore == homeScore ? MatchEnum.ResultType.DRAW : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

        var homeResult = status.equals(MatchEnum.MatchStatus.END) ?
                (homeScore == awayScore ? MatchEnum.ResultType.DRAW : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

        var awayTeamDto = awayEntity != null ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(),
                awayScore, awayResult) : null;

        var homeTeamDto = homeEntity != null ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(),
                homeScore, homeResult) : null;

        var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

        return new MatchDomain.MatchInfoResponse(
                id,
                date,
                time,
                stadiumDto,
                status,
                statusDetail,
                awayTeamDto,
                homeTeamDto
        );
    }

    @Override
    public MatchDomain.RecordResponse findRecordById(String id) {
        var matchEntity = gameMatchRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var awayTeamEntity = teamRepository.findById(matchEntity.getAwayTeamEntity().getId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var homeTeamEntity = teamRepository.findById(matchEntity.getHomeTeamEntity().getId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

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
        if (((awayPitcherRedis.isEmpty() || awayPitcherData == null) && (awayBatterRedis.isEmpty() || awayBatterData == null)) ||
            ((homePitcherRedis.isEmpty() || homePitcherData == null) && (homeBatterRedis.isEmpty() || homeBatterData == null))) {

            pitcherEntities = pitcherRecordRepository.findByGameMatchEntityId(id);
            hitterEntities = hitterRecordRepository.findByGameMatchEntityId(id);

            var awayPitcherEntities = pitcherEntities.stream()
                    .filter(entity -> !entity.getHome())
                    .toList();

            if (!awayPitcherEntities.isEmpty()) {
                awayPitchers = awayPitcherEntities.stream()
                        .map(entity -> new MatchDomain.PitcherRecordDto(
                                entity.getName(),
                                entity.getPosition(),
                                entity.getInning(),
                                entity.getPitching(),
                                entity.getBallFour(),
                                entity.getStrikeOut(),
                                entity.getHit(),
                                entity.getHomeRun(),
                                entity.getScore()
                        ))
                        .toList();
            }

            var awayBatterEntities = hitterEntities.stream()
                    .filter(entity -> !entity.getHome())
                    .toList();

            if (!awayBatterEntities.isEmpty()) {
                awayBatters = awayBatterEntities.stream()
                        .map(entity -> new MatchDomain.BatterRecordDto(
                                entity.getName(),
                                entity.getPosition(),
                                entity.getTurn(),
                                entity.getHitCount(),
                                entity.getBallFour(),
                                entity.getStrikeOut(),
                                entity.getScore(),
                                entity.getHit(),
                                entity.getHomeRun(),
                                entity.getHitScore()
                        ))
                        .toList();
            }

            var homePitcherEntities = pitcherEntities.stream()
                    .filter(entity -> entity.getHome())
                    .toList();

            if (!homePitcherEntities.isEmpty()) {
                homePitchers = homePitcherEntities.stream()
                        .map(entity -> new MatchDomain.PitcherRecordDto(
                                entity.getName(),
                                entity.getPosition(),
                                entity.getInning(),
                                entity.getPitching(),
                                entity.getBallFour(),
                                entity.getStrikeOut(),
                                entity.getHit(),
                                entity.getHomeRun(),
                                entity.getScore()
                        ))
                        .toList();
            }

            var homeBatterEntities = hitterEntities.stream()
                    .filter(entity -> entity.getHome())
                    .toList();

            if (!homeBatterEntities.isEmpty()) {
                homeBatters = homeBatterEntities.stream()
                        .map(entity -> new MatchDomain.BatterRecordDto(
                                entity.getName(),
                                entity.getPosition(),
                                entity.getTurn(),
                                entity.getHitCount(),
                                entity.getBallFour(),
                                entity.getStrikeOut(),
                                entity.getScore(),
                                entity.getHit(),
                                entity.getHomeRun(),
                                entity.getHitScore()
                        ))
                        .toList();
            }
        } else {
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
                        .map(data -> new MatchDomain.PitcherRecordDto(
                                data.name(),
                                data.position(),
                                data.inning(),
                                data.pitching(),
                                data.ballFour(),
                                data.strikeOut(),
                                data.hit(),
                                data.homeRun(),
                                data.score()
                        ))
                        .toList();
            }

            if (!awayBatterObj.isEmpty()) {
                awayBatters = awayBatterObj.stream()
                        .map(data -> new MatchDomain.BatterRecordDto(
                                data.name(),
                                data.position(),
                                data.turn(),
                                data.hitCount(),
                                data.ballFour(),
                                data.strikeOut(),
                                data.score(),
                                data.hit(),
                                data.homeRun(),
                                data.hitScore()
                        ))
                        .toList();
            }

            if (!homePitcherObj.isEmpty()) {
                homePitchers = homePitcherObj.stream()
                        .map(data -> new MatchDomain.PitcherRecordDto(
                                data.name(),
                                data.position(),
                                data.inning(),
                                data.pitching(),
                                data.ballFour(),
                                data.strikeOut(),
                                data.hit(),
                                data.homeRun(),
                                data.score()
                        ))
                        .toList();
            }

            if (!homeBatterObj.isEmpty()) {
                homeBatters = homeBatterObj.stream()
                        .map(data -> new MatchDomain.BatterRecordDto(
                                data.name(),
                                data.position(),
                                data.turn(),
                                data.hitCount(),
                                data.ballFour(),
                                data.strikeOut(),
                                data.score(),
                                data.hit(),
                                data.homeRun(),
                                data.hitScore()
                        ))
                        .toList();
            }
        }

        var awayTeamDto = new MatchDomain.TeamRecordDto(awayTeamEntity.getName(), awayPitchers, awayBatters);
        var homeTeamDto = new MatchDomain.TeamRecordDto(homeTeamEntity.getName(), homePitchers, homeBatters);

        return new MatchDomain.RecordResponse(matchEntity.getMatchAt(), awayTeamDto, homeTeamDto);
    }

    @Override
    public List<MatchDomain.InterestTeamMatchInfoResponse> findByTeam() {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

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
            return matchEntity.stream()
                    .map(entity -> {
                            var matchAt = entity.getMatchAt();
                            var awayTeamEntity = teamRepository.findById(entity.getAwayTeamEntity().getId()).orElse(null);
                            var homeTeamEntity = teamRepository.findById(entity.getHomeTeamEntity().getId()).orElse(null);
                            var stadiumEntity = stadiumRepository.findById(entity.getStadiumEntity().getId()).orElse(null);
                            var isWrited = diaryRepository.findByMemberIdAndGameMatchEntityId(id, entity.getId()).isPresent();

                            var awayScore = entity.getAwayScore();
                            var homeScore = entity.getHomeScore();

                            MatchEnum.ResultType awayResult = awayScore == null ? null :
                                    (awayScore == homeScore ? MatchEnum.ResultType.DRAW :
                                            (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);
                            MatchEnum.ResultType homeResult = homeScore == null ? null :
                                    (homeScore == awayScore ? MatchEnum.ResultType.DRAW :
                                            (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);


                            var awayTeamDto = awayTeamEntity != null ? new MatchDomain.TeamDto(awayTeamEntity.getId(), awayTeamEntity.getName(),
                                    awayScore, awayResult) : null;

                            var homeTeamDto = homeTeamEntity != null ? new MatchDomain.TeamDto(homeTeamEntity.getId(), homeTeamEntity.getName(),
                                    homeScore, homeResult) : null;

                            var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;
                        return new MatchDomain.InterestTeamMatchInfoResponse(
                                entity.getId(),
                                matchAt.toLocalDate(),
                                matchAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                                stadiumDto,
                                entity.getStatus(),
                                entity.getStatus().getDesc(),
                                awayTeamDto,
                                homeTeamDto,
                                isWrited
                        );
                    }).toList();
        }

        return matchEntity.stream()
                .map(entity -> {

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

                    var awayResult = status.equals(MatchEnum.MatchStatus.END) ?
                            (awayScore == homeScore ? MatchEnum.ResultType.DRAW : (awayScore > homeScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

                    var homeResult = status.equals(MatchEnum.MatchStatus.END) ?
                            (homeScore == awayScore ? MatchEnum.ResultType.DRAW : (homeScore > awayScore) ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS) : null;

                    var awayTeamDto = awayEntity != null ? new MatchDomain.TeamDto(awayEntity.getId(), awayEntity.getName(),
                            awayScore, awayResult) : null;

                    var homeTeamDto = homeEntity != null ? new MatchDomain.TeamDto(homeEntity.getId(), homeEntity.getName(),
                            homeScore, homeResult) : null;

                    var stadiumDto = stadiumEntity != null ? new MatchDomain.StadiumDto(stadiumEntity.getId(), stadiumEntity.getShortName(), stadiumEntity.getFullName()) : null;

                    return new MatchDomain.InterestTeamMatchInfoResponse(
                            entity.getId(),
                            date,
                            time,
                            stadiumDto,
                            status,
                            statusDetail,
                            awayTeamDto,
                            homeTeamDto,
                            isWrited
                    );
                }).toList();
    }
}
