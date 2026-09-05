package kr.co.victoryfairy.game.crawler.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.game.crawler.service.KboLiveGameCrawler.Snapshot;
import kr.co.victoryfairy.game.crawler.service.KboLiveGameCrawler.Target;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchCustomRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LiveGameSyncService {

    private static final Logger log = LoggerFactory.getLogger(LiveGameSyncService.class);

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final String MATCH_CACHE_SUFFIX = "_match_list";

    private static final String AWAY_HITTER = "away_hitter";

    private static final String AWAY_PITCHER = "away_pitcher";

    private static final String HOME_HITTER = "home_hitter";

    private static final String HOME_PITCHER = "home_pitcher";

    private final GameMatchCustomRepository gameMatchCustomRepository;

    private final GameMatchRepository gameMatchRepository;

    private final RedisHandler redisHandler;

    private final KboGameCrawler gameCrawler;

    private final KboLiveGameCrawler crawler;

    private final GameRecordDomainService gameRecordDomainService;

    public LiveGameSyncService(GameMatchCustomRepository gameMatchCustomRepository,
            GameMatchRepository gameMatchRepository, RedisHandler redisHandler,
            KboGameCrawler gameCrawler, KboLiveGameCrawler crawler,
            GameRecordDomainService gameRecordDomainService) {
        this.gameMatchCustomRepository = gameMatchCustomRepository;
        this.gameMatchRepository = gameMatchRepository;
        this.redisHandler = redisHandler;
        this.gameCrawler = gameCrawler;
        this.crawler = crawler;
        this.gameRecordDomainService = gameRecordDomainService;
    }

    public int sync() {
        LocalDateTime now = LocalDateTime.now(KOREA);
        return sync(now.toLocalDate(), now);
    }

    public int sync(LocalDate gameDate) {
        return sync(gameDate, LocalDateTime.now(KOREA));
    }

    private int sync(LocalDate gameDate, LocalDateTime now) {
        var matches = gameMatchCustomRepository.findByMatchAt(gameDate, MatchEnum.LeagueType.KBO)
            .stream()
            .filter(match -> shouldPoll(now, match.getMatchAt(), match.getStatus(), match.getIsMatchInfoCraw()))
            .collect(Collectors.toMap(GameMatchEntity::getId, Function.identity()));
        if (matches.isEmpty()) {
            log.info("No live KBO matches to sync");
            return 0;
        }

        var targets = matches.values()
            .stream()
            .map(match -> new Target(match.getId(), match.getSeries().getValue()))
            .toList();
        var snapshots = crawler.crawl(gameDate, targets);
        for (Snapshot snapshot : snapshots) {
            try {
                apply(matches.get(snapshot.id()), snapshot);
            }
            catch (Exception e) {
                log.error("Failed to store live KBO match: {}", snapshot.id(), e);
            }
        }
        return snapshots.size();
    }

    public Optional<LocalDateTime> nextExecutionAt(LocalDate gameDate, LocalDateTime now, LocalDateTime notBefore) {
        return gameMatchCustomRepository.findByMatchAt(gameDate, MatchEnum.LeagueType.KBO)
            .stream()
            .filter(match -> match.getStatus() != MatchEnum.MatchStatus.CANCELED)
            .filter(match -> match.getStatus() != MatchEnum.MatchStatus.END
                    || !Boolean.TRUE.equals(match.getIsMatchInfoCraw()))
            .map(match -> {
                var pollingStartsAt = match.getMatchAt().minusMinutes(10);
                return pollingStartsAt.isAfter(now) ? pollingStartsAt : notBefore;
            })
            .min(LocalDateTime::compareTo);
    }

    static boolean shouldPoll(LocalDateTime now, LocalDateTime matchAt, MatchEnum.MatchStatus status,
            Boolean detailCrawled) {
        if (status == MatchEnum.MatchStatus.CANCELED) {
            return false;
        }
        if (status == MatchEnum.MatchStatus.END) {
            return !Boolean.TRUE.equals(detailCrawled);
        }
        return !now.isBefore(matchAt.minusMinutes(10));
    }

    private void apply(GameMatchEntity match, Snapshot snapshot) {
        redisHandler.pushHash(match.getMatchAt().toLocalDate().format(DATE) + MATCH_CACHE_SUFFIX, match.getId(),
                matchCache(match, snapshot));
        GameMatchEntity savedMatch = saveStatus(match, snapshot);

        if (snapshot.status() == MatchEnum.MatchStatus.END || snapshot.status() == MatchEnum.MatchStatus.CANCELED) {
            gameRecordDomainService.recover(savedMatch);
        }

        if (snapshot.status() == MatchEnum.MatchStatus.PROGRESS) {
            if (snapshot.records().hasData()) {
                cacheLiveRecords(snapshot);
            }
        }
        else if (snapshot.status() == MatchEnum.MatchStatus.END && !Boolean.TRUE.equals(match.getIsMatchInfoCraw())) {
            gameCrawler.crawMatchDetailById(match.getId());
            clearLiveRecords(match.getId());
        }
    }

    private GameMatchEntity saveStatus(GameMatchEntity match, Snapshot snapshot) {
        if (snapshot.status() == MatchEnum.MatchStatus.READY) {
            return match;
        }
        return gameMatchRepository.save(match.toBuilder()
            .status(snapshot.status())
            .reason(snapshot.reason())
            .awayScore(snapshot.awayScore())
            .homeScore(snapshot.homeScore())
            .build());
    }

    private void cacheLiveRecords(Snapshot snapshot) {
        var records = snapshot.records();
        redisHandler.pushHash(AWAY_HITTER, snapshot.id(), records.awayHitters());
        redisHandler.pushHash(AWAY_PITCHER, snapshot.id(), records.awayPitchers());
        redisHandler.pushHash(HOME_HITTER, snapshot.id(), records.homeHitters());
        redisHandler.pushHash(HOME_PITCHER, snapshot.id(), records.homePitchers());
    }

    private static Map<String, Object> matchCache(GameMatchEntity match, Snapshot snapshot) {
        Map<String, Object> cache = new HashMap<>();
        cache.put("date", match.getMatchAt().toLocalDate().format(DATE));
        cache.put("time", match.getMatchAt().format(TIME));
        cache.put("awayId", match.getAwayTeamEntity().getId());
        cache.put("awayScore", snapshot.awayScore());
        cache.put("homeId", match.getHomeTeamEntity().getId());
        cache.put("homeScore", snapshot.homeScore());
        cache.put("status", snapshot.status());
        cache.put("statusDetail", snapshot.statusDetail());
        cache.put("stadium", match.getStadiumEntity().getShortName());
        cache.put("stadiumId", match.getStadiumEntity().getId());
        cache.put("reason", snapshot.reason());
        cache.put("league", match.getLeague());
        return cache;
    }

    private void clearLiveRecords(String id) {
        redisHandler.deleteHash(AWAY_HITTER, id);
        redisHandler.deleteHash(AWAY_PITCHER, id);
        redisHandler.deleteHash(HOME_HITTER, id);
        redisHandler.deleteHash(HOME_PITCHER, id);
    }

}
