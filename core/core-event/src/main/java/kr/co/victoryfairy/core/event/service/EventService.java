package kr.co.victoryfairy.core.event.service;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.event.model.EventDomain;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final Logger log = LoggerFactory.getLogger(EventService.class);

    private final MemberRepository memberRepository;
    private final DiaryRepository diaryRepository;
    private final TeamRepository teamRepository;
    private final GameMatchRepository matchRepository;
    private final GameRecordRepository gameRecordRepository;

    private final WinningRateRepository winningRateRepository;

    public EventService(MemberRepository memberRepository, DiaryRepository diaryRepository,
                        GameMatchRepository matchRepository, TeamRepository teamRepository,
                        GameRecordRepository gameRecordRepository, WinningRateRepository winningRateRepository) {
        this.memberRepository = memberRepository;
        this.diaryRepository = diaryRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.gameRecordRepository = gameRecordRepository;
        this.winningRateRepository = winningRateRepository;
    }

    @Transactional
    public boolean processDiary(EventDomain.WriteEventDto eventDto) {
        log.info(">>> Start processing diary event: {}", eventDto.diaryId());
        var memberEntity = memberRepository.findById(eventDto.memberId())
                .orElse(null);

        var matchEntity = matchRepository.findById(eventDto.gameId())
                .orElse(null);

        var diaryEntity = diaryRepository.findById(eventDto.diaryId())
                .orElse(null);

        if (diaryEntity == null || diaryEntity.getIsRated()) {
            log.info(">>> Diary is null: {}", eventDto.diaryId());
            return true;
        }

        var teamEntity = teamRepository.findById(diaryEntity.getTeamEntity().getId())
                .orElse(null);

        if (memberEntity == null || matchEntity == null || teamEntity == null) {
            return false;
        }

        var awayTeam = matchEntity.getAwayTeamEntity();
        var homeTeam = matchEntity.getHomeTeamEntity();

        var awayScore = matchEntity.getAwayScore() != null ? matchEntity.getAwayScore() : 0;
        var homeScore = matchEntity.getHomeScore() != null ? matchEntity.getHomeScore() : 0;

        var isAway = awayTeam.getId().equals(teamEntity.getId());

        var myScore = isAway ? awayScore : homeScore;
        var opponentScore = isAway ? homeScore : awayScore;

        var isWin = myScore > opponentScore;

        var matchResult = (myScore == opponentScore) ? MatchEnum.ResultType.DRAW
                : (isWin ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

        var gameRecordEntity = GameRecordEntity.builder()
                .member(memberEntity)
                .diaryEntity(diaryEntity)
                .gameMatchEntity(matchEntity)
                .teamEntity(teamEntity)
                .teamName(teamEntity.getName())
                .opponentTeamEntity(isAway ? homeTeam : awayTeam)
                .opponentTeamName(isAway ? homeTeam.getName() : awayTeam.getName())
                .stadiumEntity(matchEntity.getStadiumEntity())
                .viewType(diaryEntity.getViewType())
                .status(matchEntity.getStatus())
                .resultType(matchResult)
                .season(matchEntity.getSeason())
                .leagueType(matchEntity.getLeague())  // WBC 지원: 리그 타입 저장
                .build();
        gameRecordRepository.save(gameRecordEntity);
        gameRecordRepository.flush();

        // 이벤트 적용 여부 업데이트
        diaryEntity.updateRated();
        diaryRepository.save(diaryEntity);
        diaryRepository.flush();

        log.info(">>> Finished diary event, isRated = {}", diaryEntity.getIsRated());
        return true;
    }

    @Transactional
    public boolean processBatch(EventDomain.WriteEventDto eventDto) {
        var matchEntity = matchRepository.findById(eventDto.gameId())
                .orElse(null);

        var diaryEntities = diaryRepository.findByGameMatchEntityAndIsRatedFalse(matchEntity);

        if (diaryEntities.isEmpty()) {
            return true;
        }

        diaryEntities.forEach(diaryEntity -> {
            if (diaryEntity.getIsRated()) {
                return;
            }

            var memberEntity = memberRepository.findById(diaryEntity.getMember().getId())
                    .orElse(null);

            var teamEntity = teamRepository.findById(diaryEntity.getTeamEntity().getId())
                    .orElse(null);

            var awayTeam = matchEntity.getAwayTeamEntity();
            var homeTeam = matchEntity.getHomeTeamEntity();

            var awayScore = matchEntity.getAwayScore() != null ? matchEntity.getAwayScore() : 0;
            var homeScore = matchEntity.getHomeScore() != null ? matchEntity.getHomeScore() : 0;

            var isAway = awayTeam.getId().equals(teamEntity.getId());

            var myScore = isAway ? awayScore : homeScore;
            var opponentScore = isAway ? homeScore : awayScore;

            var isWin = myScore > opponentScore;

            var matchResult = (myScore == opponentScore) ? MatchEnum.ResultType.DRAW
                    : (isWin ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS);

            var gameRecordEntity = GameRecordEntity.builder()
                    .member(memberEntity)
                    .diaryEntity(diaryEntity)
                    .teamEntity(teamEntity)
                    .gameMatchEntity(matchEntity)
                    .teamName(teamEntity.getName())
                    .opponentTeamEntity(isAway ? homeTeam : awayTeam)
                    .opponentTeamName(isAway ? homeTeam.getName() : awayTeam.getName())
                    .stadiumEntity(matchEntity.getStadiumEntity())
                    .viewType(diaryEntity.getViewType())
                    .status(matchEntity.getStatus())
                    .resultType(matchResult)
                    .season(matchEntity.getSeason())
                    .leagueType(matchEntity.getLeague())  // WBC 지원: 리그 타입 저장
                    .build();

            gameRecordRepository.save(gameRecordEntity);

            diaryEntity.updateRated();
            diaryRepository.save(diaryEntity);

        });
        return true;
    }
}
