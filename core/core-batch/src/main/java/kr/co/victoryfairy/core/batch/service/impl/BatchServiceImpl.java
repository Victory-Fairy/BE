package kr.co.victoryfairy.core.batch.service.impl;

import com.microsoft.playwright.*;
import io.dodn.springboot.core.enums.EventType;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.batch.model.WriteEventDto;
import kr.co.victoryfairy.core.batch.service.BatchService;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.redis.handler.RedisOperator;
import kr.co.victoryfairy.support.properties.FileProperties;
import kr.co.victoryfairy.support.utils.SlackUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BatchServiceImpl implements BatchService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MemberRepository memberRepository;
    private final DiaryRepository diaryRepository;
    private final TeamRepository teamRepository;
    private final GameMatchRepository gameMatchRepository;
    private final GameRecordRepository gameRecordRepository;
    private final GameMatchCustomRepository gameMatchEntityCustomRepository;
    private final HitterRecordRepository hitterRecordRepository;
    private final PitcherRecordRepository pitcherRecordRepository;
    private final StadiumRepository stadiumRepository;
    private final FileRepository fileRepository;
    private final FileCustomRepository fileCustomRepository;

    private final FileProperties fileProperties;
    private final RedisHandler redisHandler;
    private final SlackUtils slackUtils;
    private final RedisOperator redisOperator;

    private final RedisTemplate<String, Object> redisTemplate;
    public BatchServiceImpl(MemberRepository memberRepository, DiaryRepository diaryRepository,
                            TeamRepository teamRepository, GameMatchRepository gameMatchRepository,
                            GameMatchCustomRepository gameMatchEntityCustomRepository, GameRecordRepository gameRecordRepository,
                            HitterRecordRepository hitterRecordRepository,
                            PitcherRecordRepository pitcherRecordRepository,
                            FileRepository fileRepository,
                            FileCustomRepository fileCustomRepository,
                            FileProperties fileProperties,
                            RedisTemplate<String, Object> redisTemplate,
                            StadiumRepository stadiumRepository,
                            RedisHandler redisHandler, SlackUtils slackUtils, RedisOperator redisOperator) {
        this.memberRepository = memberRepository;
        this.diaryRepository = diaryRepository;
        this.teamRepository = teamRepository;
        this.gameMatchRepository = gameMatchRepository;
        this.gameMatchEntityCustomRepository = gameMatchEntityCustomRepository;
        this.gameRecordRepository = gameRecordRepository;
        this.hitterRecordRepository = hitterRecordRepository;
        this.pitcherRecordRepository = pitcherRecordRepository;
        this.fileRepository = fileRepository;
        this.fileCustomRepository = fileCustomRepository;
        this.fileProperties = fileProperties;
        this.redisHandler = redisHandler;
        this.slackUtils = slackUtils;
        this.redisOperator = redisOperator;
        this.stadiumRepository = stadiumRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public void batchScore() {
        logger.info("========== Batch  Start ==========");

        var id = "";
        var formattedDate = "";
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("https://m.koreabaseball.com/Kbo/Schedule.aspx");
            //page.evaluate("getGameDateList('20250503')");

            page.waitForSelector("ul#now");
            List<ElementHandle> gameElements = page.querySelectorAll("ul#now > li.list");

            String dateText = page.querySelector("#lblGameDate").innerText();
            formattedDate = dateText.replaceAll("[^0-9]", "");

            if (gameElements.isEmpty()) return;

            String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            if (now.equals(formattedDate)) {
                for (ElementHandle game : gameElements) {
                    String classAttr = game.getAttribute("class");
                    String statusClass = classAttr.replace("list", "").trim();

                    // 경기 예정인 경우 제외
                    //if (!StringUtils.hasText(statusClass)) continue;

                    // 상태, 취소 사유 처리
                    var matchStatus = MatchEnum.MatchStatus.READY;
                    var reason = "-";
                    if ("end".equals(statusClass)) {
                        matchStatus = MatchEnum.MatchStatus.END;
                    } else if ("cancel".equals(statusClass)) {
                        matchStatus = MatchEnum.MatchStatus.CANCELED;
                        reason  = game.querySelector(".bottom ul li a").innerText();
                    } else if ("ing".equals(statusClass)) {
                        matchStatus = MatchEnum.MatchStatus.PROGRESS;
                        reason  = game.querySelector(".bottom ul li a").innerText();
                    }

                    // 더블헤더 여부 처리
                    var matchOrder = 0;
                    ElementHandle dhSpan = game.querySelector("span.dh");
                    if (dhSpan != null) {
                        matchOrder = dhSpan.innerText().equals("DH1") ? 1 : 2;
                    }

                    // 어웨이 팀명 (왼쪽 엠블럼)
                    String awaySrc = game.querySelector(".emb.txt-r img").getAttribute("src");
                    String awayTeamName = "";
                    Matcher awayMatcher = matchStatus.equals(MatchEnum.MatchStatus.CANCELED) ? Pattern.compile("emblemR_(\\w+)\\.png").matcher(awaySrc) : Pattern.compile("emblem_(\\w+)\\.png").matcher(awaySrc);
                    if (awayMatcher.find()) {
                        awayTeamName = awayMatcher.group(1);
                    }

                    // 홈 팀명 (오른쪽 엠블럼)
                    String homeSrc = game.querySelector(".emb:not(.txt-r) img").getAttribute("src");
                    String homeTeamName = "";
                    Matcher homeMatcher = matchStatus.equals(MatchEnum.MatchStatus.CANCELED) ? Pattern.compile("emblemR_(\\w+)\\.png").matcher(homeSrc) : Pattern.compile("emblem_(\\w+)\\.png").matcher(homeSrc);
                    if (homeMatcher.find()) {
                        homeTeamName = homeMatcher.group(1);
                    }

                    // 어웨이 점수
                    var awayScoreSpan = game.querySelector(".team.away .score");
                    Short awayScore = awayScoreSpan != null ? Short.parseShort(awayScoreSpan.innerText()) : null;
                    // 홈 점수
                    var homeScoreSpn = game.querySelector(".team.home .score");
                    Short homeScore = homeScoreSpn != null ? Short.parseShort(homeScoreSpn.innerText()) : null;

                    id = formattedDate + awayTeamName + homeTeamName + matchOrder;

                    var matchEntity = gameMatchRepository.findById(id).orElse(null);
                    if (matchEntity == null) continue;
                    var stadiumEntity = matchEntity.getStadiumEntity();

                    // Redis 저장 처리
                    ElementHandle status = game.querySelector("span.staus");
                    Map<String, Object> map = new HashMap<>();

                    String time = matchEntity.getMatchAt().format(DateTimeFormatter.ofPattern("HH:mm"));

                    map.put("data", formattedDate);
                    map.put("time", time);
                    map.put("awayId", matchEntity.getAwayTeamEntity().getId());
                    map.put("awayImage", awaySrc);
                    map.put("awayScore", awayScore != null ? awayScore : null);
                    map.put("homeId", matchEntity.getHomeTeamEntity().getId());
                    map.put("homeImage", homeSrc);
                    map.put("homeScore", homeScore != null ? homeScore : null);
                    map.put("status", matchStatus);
                    map.put("statusDetail", status.innerText());
                    map.put("stadium", stadiumEntity.getShortName());
                    map.put("stadiumId", stadiumEntity.getId());
                    map.put("reason", reason);

                    redisHandler.pushHash(formattedDate + "_match_list", id, map);

                    // 경기 상태 변경
                    if (matchEntity.getStatus().equals(MatchEnum.MatchStatus.READY) ||
                        matchEntity.getStatus().equals(MatchEnum.MatchStatus.PROGRESS)) {

                        String finalId = id;
                        switch (matchStatus) {
                            case PROGRESS -> {
                                matchEntity = matchEntity.toBuilder()
                                        .reason(reason)
                                        .status(matchStatus)
                                        .build();
                                gameMatchRepository.save(matchEntity);
                            }
                            case END -> {
                                matchEntity = matchEntity.toBuilder()
                                        .awayScore(awayScore)
                                        .homeScore(homeScore)
                                        .status(matchStatus)
                                        .build();
                                gameMatchRepository.save(matchEntity);

                                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        var writeEventDto = new WriteEventDto(finalId, null, null, EventType.BATCH);
                                        redisHandler.pushEvent("write_diary", writeEventDto);
                                    }
                                });

                            }
                            case CANCELED -> {
                                matchEntity = matchEntity.toBuilder()
                                        .reason(reason)
                                        .status(matchStatus)
                                        .build();
                                gameMatchRepository.save(matchEntity);

                                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        var writeEventDto = new WriteEventDto(finalId, null, null, EventType.BATCH);
                                        redisHandler.pushEvent("write_diary", writeEventDto);
                                    }
                                });
                            }
                        }
                    }
                }
            }

            page.close(); // 안전하게 닫기
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
            slackUtils.message(id + " 점수 불러오는 중 에러 발생");
        }

        var now = LocalDate.now();
        var todayMatches = gameMatchEntityCustomRepository.findByMatchAt(now);
        boolean noProgressMatch = todayMatches.stream()
                .noneMatch(match -> match.getStatus() == MatchEnum.MatchStatus.PROGRESS);

        if (noProgressMatch) {
            redisHandler.deleteHash(formattedDate + "_match_list");
        }
    }

    @Override
    @Transactional
    public void batchMatchInfo() {
        logger.info("========== Match Info Craw  Start ==========");
        // 오늘 일자 경기 목록 조회
        var now = LocalDate.now();
        var matches = gameMatchEntityCustomRepository.findByMatchAt(now);

        // status 가 end, isMatchInfoCraw 가 false or null 인 요소
        matches = matches.stream()
                .filter(entity -> (entity.getIsMatchInfoCraw() == null || !entity.getIsMatchInfoCraw()))
                .toList();

        // id 로 match detail 조회
        List<HitterRecordEntity> hitterEntities = new ArrayList<>();
        List<PitcherRecordEntity> pitcherEntities = new ArrayList<>();

        List<HitterRecordEntity> awayHitter = new ArrayList<>();
        List<PitcherRecordEntity> awayPitcher = new ArrayList<>();
        List<HitterRecordEntity> homeHitter = new ArrayList<>();
        List<PitcherRecordEntity> homePitcher = new ArrayList<>();

        for (var entity : matches) {

            if (entity.getStatus().equals(MatchEnum.MatchStatus.READY) ||
                entity.getStatus().equals(MatchEnum.MatchStatus.CANCELED) ||
                entity.getIsMatchInfoCraw()) {
                continue;
            }

            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch();
                Page page = browser.newPage();

                page.navigate("https://m.koreabaseball.com/Kbo/Live/Record.aspx?p_le_id=1&p_sr_id=" + entity.getSeries().getValue() + "&p_g_id=" + entity.getId());

                page.waitForSelector("#HitterRank table tbody tr"); // 타자
                page.waitForSelector("#PitcherRank table tbody tr"); // 투수
                awayHitter = this.scrapeHitterEntity(page, false, String.valueOf(now.getYear()), entity);
                var awayHitterMap = scrapeHitterMap(page, false, String.valueOf(now.getYear()), entity);
                page.waitForTimeout(500);
                awayPitcher = this.scrapPitcherEntity(page, false, String.valueOf(now.getYear()), entity);
                var awayPitcherMap = scrapPitcherMap(page, false, String.valueOf(now.getYear()), entity);

                page.click("#liveRecordSubTabB");
                page.waitForTimeout(1500); // 탭 전환 후 데이터 로딩 기다림

                page.waitForSelector("#HitterRank table tbody tr"); // 타자
                page.waitForSelector("#PitcherRank table tbody tr"); // 투수
                homeHitter = this.scrapeHitterEntity(page, true, String.valueOf(now.getYear()), entity);
                var homeHitterMap = scrapeHitterMap(page, true, String.valueOf(now.getYear()), entity);
                page.waitForTimeout(500);
                homePitcher = this.scrapPitcherEntity(page, true, String.valueOf(now.getYear()), entity);
                var homePitcherMap = scrapPitcherMap(page, true, String.valueOf(now.getYear()), entity);

                redisHandler.pushHash("away_hitter", entity.getId(), awayHitterMap);
                redisHandler.pushHash("away_pitcher", entity.getId(), awayPitcherMap);

                redisHandler.pushHash("home_hitter", entity.getId(), homeHitterMap);
                redisHandler.pushHash("home_pitcher", entity.getId(), homePitcherMap);

            } catch (Exception e) {
                slackUtils.message(entity.getId() + " 상세 불러오는 중 에러 발생");
                e.printStackTrace();
            }

            if (entity.getStatus().equals(MatchEnum.MatchStatus.END) || entity.getStatus().equals(MatchEnum.MatchStatus.CANCELED)) {
                // 저장 후 game_match is_match_info_craw true 처리
                entity = entity.toBuilder()
                        .isMatchInfoCraw(true)
                        .build();

                gameMatchRepository.save(entity);

                hitterEntities.addAll(awayHitter);
                hitterEntities.addAll(homeHitter);
                pitcherEntities.addAll(awayPitcher);
                pitcherEntities.addAll(homePitcher);

                hitterRecordRepository.saveAll(hitterEntities);
                pitcherRecordRepository.saveAll(pitcherEntities);

                redisHandler.deleteHash("away_hitter", entity.getId());
                redisHandler.deleteHash("away_pitcher", entity.getId());
                redisHandler.deleteHash("home_hitter", entity.getId());
                redisHandler.deleteHash("home_pitcher", entity.getId());
            }
        }


        logger.info("========== Match Info Craw  END ==========");
    }

    @Override
    @Transactional
    public void checkEvent() {
        logger.info("========== Check Event Start ==========");

        String streamKey = "write_diary";
        String groupName = "diary_group";

        // 테스트 모드 여부 (true 로 하면 테스트 consumer 사용)

        String consumerName = "event_consumer";

        var pendingSummary = redisOperator.pendingSummary(streamKey, groupName);

        pendingSummary.getPendingMessagesPerConsumer().forEach((consumer, count) -> {
            logger.info("Consumer: {}, Pending Messages: {}", consumer, count);
        });

        if (pendingSummary.getTotalPendingMessages() > 0) {
            List<PendingMessage> pendingMessages = redisOperator.getPendingDetailsPaged(streamKey, groupName, 10);

            pendingMessages.stream()
                    .filter(Objects::nonNull)
                    .forEach(pendingMessage -> {
                        List<MapRecord<String, Object, Object>> messages =
                                redisOperator.read(streamKey, groupName, consumerName, pendingMessage.getIdAsString());

                        messages.stream()
                                .filter(Objects::nonNull)
                                .forEach(recordMessage -> {
                                    Map<Object, Object> body = recordMessage.getValue();
                                    logger.info("Reprocessing record: {}", body);

                                    var eventType = EventType.valueOf(String.valueOf(body.get("type")));
                                    var matchEntity = gameMatchRepository.findById(String.valueOf(body.get("gameId")))
                                            .orElse(null);

                                    if (eventType.equals(EventType.DIARY)) {
                                        var memberEntity = memberRepository.findById(Long.parseLong((String) body.get("memberId")))
                                                .orElse(null);

                                        var diaryEntity = diaryRepository.findById(Long.parseLong((String) body.get("diaryId")))
                                                .orElse(null);

                                        if (diaryEntity.getIsRated()) {
                                            logger.info("DiaryId {} already rated, skip.", diaryEntity.getId());
                                            redisOperator.ack(streamKey, groupName, recordMessage.getId().getValue());
                                            redisHandler.eventKnowEdge(streamKey, groupName, recordMessage.getId().getValue());
                                            return;
                                        }

                                        var teamEntity = teamRepository.findById(diaryEntity.getTeamEntity().getId())
                                                .orElse(null);

                                        if (memberEntity == null || matchEntity == null || diaryEntity == null || teamEntity == null) {
                                            return;
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
                                                .build();
                                        gameRecordRepository.save(gameRecordEntity);

                                        // 이벤트 적용 여부 업데이트
                                        diaryEntity.updateRated();
                                        diaryRepository.save(diaryEntity);

                                        // 실제 처리 로직 넣기 (테스트 시에는 ack 생략 가능, 원하면 ack 넣어도 됨)
                                        redisOperator.ack(streamKey, groupName, recordMessage.getId().getValue());
                                        redisHandler.eventKnowEdge(streamKey, groupName, recordMessage.getId().getValue());
                                        logger.info("Acked messageId={}", recordMessage.getId().getValue());

                                    } else {
                                        var diaryEntities = diaryRepository.findByGameMatchEntityAndIsRatedFalse(matchEntity);

                                        if (diaryEntities.isEmpty()) {
                                            return;
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
                                                    .build();
                                            gameRecordRepository.save(gameRecordEntity);

                                            // 이벤트 적용 여부 업데이트
                                            diaryEntity.updateRated();
                                            diaryRepository.save(diaryEntity);

                                            // 실제 처리 로직 넣기 (테스트 시에는 ack 생략 가능, 원하면 ack 넣어도 됨)
                                            redisOperator.ack(streamKey, groupName, recordMessage.getId().getValue());
                                            redisHandler.eventKnowEdge(streamKey, groupName, recordMessage.getId().getValue());
                                        });
                                    }
                                });
                    });
        } else {
            logger.info("No pending messages to process.");
        }
    }

    @Override
    @Transactional
    public void checkMatchList() {
        logger.info("========== Check Event Start ==========");

        var thisDay = LocalDate.now();
        // 다음 주 월요일
        var nextWeekMonday = thisDay.plusWeeks(1);
        // 다음 주 일요일
        var nextWeekSunday = nextWeekMonday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        var teamEntities = teamRepository.findAll().stream()
                .collect(Collectors.toMap(TeamEntity::getKboNm, entity -> entity));

        var stadiumEntities = stadiumRepository.findAll().stream()
                .collect(Collectors.toMap(StadiumEntity::getRegion, entity -> entity));

        // 다음 주 하루씩 반복
        for (var date = nextWeekMonday; !date.isAfter(nextWeekSunday); date = date.plusDays(1)) {
            logger.info("date : {}", date);
            var matchEntities = gameMatchEntityCustomRepository.findByMatchAt(date);
            if (matchEntities.isEmpty()) {
                continue;
            }
            try (Playwright playwright = Playwright.create()) {
                var formatDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                Browser browser = playwright.chromium().launch();
                Page page = browser.newPage();
                page.navigate("https://m.koreabaseball.com/Kbo/Schedule.aspx");
                page.evaluate("getGameDateList('"+formatDate+"')");

                page.waitForSelector("ul#now");

                List<ElementHandle> gameElements = page.querySelectorAll("ul#now > li.list");
                logger.info("gameElements : {}", gameElements.size());

                if (gameElements.size() != matchEntities.size()) {
                    gameMatchRepository.deleteAll(matchEntities);
                    slackUtils.message(date + " 대회 일정 변경 감지");

                    for (ElementHandle game : gameElements) {
                        String classAttr = game.getAttribute("class");
                        String statusClass = classAttr.replace("list", "").trim();

                        // 상태, 취소 사유 처리
                        var matchStatus = MatchEnum.MatchStatus.READY;
                        var reason = "-";
                        if ("end".equals(statusClass)) {
                            matchStatus = MatchEnum.MatchStatus.END;
                        } else if ("cancel".equals(statusClass)) {
                            matchStatus = MatchEnum.MatchStatus.CANCELED;
                            reason  = game.querySelector(".bottom ul li a").innerText();
                        } else if ("ing".equals(statusClass)) {
                            matchStatus = MatchEnum.MatchStatus.PROGRESS;
                            reason  = game.querySelector(".bottom ul li a").innerText();
                        } else {
                            reason = null;
                        }

                        // 더블헤더 여부 처리
                        var matchOrder = 0;
                        ElementHandle dhSpan = game.querySelector("span.dh");
                        if (dhSpan != null) {
                            matchOrder = dhSpan.innerText().equals("DH1") ? 1 : 2;
                        }

                        // 어웨이 팀명 (왼쪽 엠블럼)
                        String awaySrc = game.querySelector(".emb.txt-r img").getAttribute("src");
                        String awayTeamName = "";
                        Matcher awayMatcher = matchStatus.equals(MatchEnum.MatchStatus.CANCELED) ? Pattern.compile("emblemR_(\\w+)\\.png").matcher(awaySrc) : Pattern.compile("emblem_(\\w+)\\.png").matcher(awaySrc);
                        if (awayMatcher.find()) {
                            awayTeamName = awayMatcher.group(1);
                        }

                        // 홈 팀명 (오른쪽 엠블럼)
                        String homeSrc = game.querySelector(".emb:not(.txt-r) img").getAttribute("src");
                        String homeTeamName = "";
                        Matcher homeMatcher = matchStatus.equals(MatchEnum.MatchStatus.CANCELED) ? Pattern.compile("emblemR_(\\w+)\\.png").matcher(homeSrc) : Pattern.compile("emblem_(\\w+)\\.png").matcher(homeSrc);
                        if (homeMatcher.find()) {
                            homeTeamName = homeMatcher.group(1);
                        }

                        // 어웨이 점수
                        var awayScoreSpan = game.querySelector(".team.away .score");
                        Short awayScore = awayScoreSpan != null ? Short.parseShort(awayScoreSpan.innerText()) : null;
                        // 홈 점수
                        var homeScoreSpn = game.querySelector(".team.home .score");
                        Short homeScore = homeScoreSpn != null ? Short.parseShort(homeScoreSpn.innerText()) : null;

                        var id = formatDate + awayTeamName + homeTeamName + matchOrder;

                        String selector = "a[href*='/Kbo/Live/Record.aspx']";
                        Locator recordLink = page.locator(selector);

                        MatchEnum.SeriesType seriesType = MatchEnum.SeriesType.REGULAR;
                        MatchEnum.MatchType matchType = MatchEnum.MatchType.REGULAR;
                        if (recordLink.count() > 0) {
                            String href = recordLink.first().getAttribute("href");

                            String pSrId = Arrays.stream(href.split("&"))
                                    .filter(s -> s.contains("p_sr_id="))
                                    .map(s -> s.split("=")[1])
                                    .findFirst()
                                    .orElse(null);

                            if (pSrId.equals("1")) {
                                seriesType = MatchEnum.SeriesType.EXHIBITION;
                                matchType = MatchEnum.MatchType.EXHIBITION;
                            } else if (pSrId.equals("3")) {
                                seriesType = MatchEnum.SeriesType.SEMI_PLAYOFF;
                                matchType = MatchEnum.MatchType.POST;
                            } else if (pSrId.equals("4")) {
                                seriesType = MatchEnum.SeriesType.WILDCARD;
                                matchType = MatchEnum.MatchType.POST;
                            } else if (pSrId.equals("5")) {
                                seriesType = MatchEnum.SeriesType.PLAYOFF;
                                matchType = MatchEnum.MatchType.POST;
                            } else if (pSrId.equals("6")) {
                                seriesType = MatchEnum.SeriesType.TIEBREAKER;
                                matchType = MatchEnum.MatchType.TIEBREAKER;
                            } else if (pSrId.equals("7")) {
                                seriesType = MatchEnum.SeriesType.KOREA;
                                matchType = MatchEnum.MatchType.POST;
                            }
                        }

                        var awayEntity = teamEntities.get(awayTeamName);
                        var homeEntity = teamEntities.get(homeTeamName);

                        Locator dataP = page.locator("div.data > p:not(.broadcast)");

                        StadiumEntity stadiumEntity = null;
                        LocalDateTime matchAt = null;
                        if (dataP.count() > 0) {
                            String text = dataP.first().innerText(); // "대전(신) , 18:30"
                            String[] parts = text.split("\\s*,\\s*");

                            String stadium = parts.length > 0 ? parts[0] : null;
                            String time = parts.length > 1 ? parts[1] : null;
                            LocalTime parseTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));

                            matchAt = LocalDateTime.of(date, parseTime);
                            stadiumEntity = stadiumEntities.get(stadium);
                        }

                        var matchEntity = GameMatchEntity.builder()
                                .id(id)
                                .type(matchType)
                                .series(seriesType)
                                .season(String.valueOf(thisDay.getYear()))
                                .matchAt(matchAt)
                                .awayTeamEntity(awayEntity)
                                .awayNm(awayEntity.getSponsorNm())
                                .awayScore(awayScore)
                                .homeTeamEntity(homeEntity)
                                .homeNm(homeEntity.getSponsorNm())
                                .homeScore(homeScore)
                                .stadiumEntity(stadiumEntity)
                                .reason(reason)
                                .status(matchStatus)
                                .build();
                        gameMatchRepository.save(matchEntity);
                    }
                }

            } catch (Exception e) {
                slackUtils.message(date + " 대회 확인 중 에러 발생");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void checkMissFile() {
        logger.info("========== Check Miss File Start ==========");
        var date = LocalDateTime.now().minusDays(3);

        var fileEntities = fileCustomRepository.findMissingFile(date);

        var deletedFiles = new ArrayList<FileEntity>();
        try {
            if (!fileEntities.isEmpty()) {
                for (FileEntity fileEntity : fileEntities) {
                    String directoryPath = fileEntity.getPath();
                    String saveName = fileEntity.getSaveName();
                    String ext = fileEntity.getExt();

                    File directory = new File(fileProperties.getStoragePath(), directoryPath);
                    if (directory.exists() && directory.isDirectory()) {
                        File[] filesToDelete = directory.listFiles(file ->
                                file.getName().startsWith(saveName) && file.getName().endsWith("." + ext)
                        );

                        if (filesToDelete != null) {
                            for (File file : filesToDelete) {
                                boolean deleted = file.delete();
                                if (deleted) {
                                    System.out.println("Deleted file: " + file.getAbsolutePath());
                                    deletedFiles.add(fileEntity);
                                } else {
                                    System.err.println("Failed to delete file: " + file.getAbsolutePath());
                                }
                            }
                        }
                    }
                }

                fileRepository.deleteAll(deletedFiles);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<HitterRecordEntity> scrapeHitterEntity(Page page, Boolean isHome, String year, GameMatchEntity gameMatchEntity) {
        List<ElementHandle> infoRows = page.querySelectorAll("#HitterRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#HitterRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());
        List<HitterRecordEntity> hitterEntities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            var turn = Short.parseShort(infoRow.querySelector("td").innerText().trim());

            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "", position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            var hitCount = Short.parseShort(stats.get(0).innerText());
            var score = Short.parseShort(stats.get(1).innerText());
            var hit = Short.parseShort(stats.get(2).innerText());
            var homeRun = Short.parseShort(stats.get(3).innerText());
            var hitScore = Short.parseShort(stats.get(4).innerText());
            var ballFour = Short.parseShort(stats.get(5).innerText());
            var strikeOut = Short.parseShort(stats.get(6).innerText());

            hitterEntities.add(new HitterRecordEntity(
                    turn,
                    name,
                    position,
                    hitCount,
                    score,
                    hit,
                    homeRun,
                    hitScore,
                    ballFour,
                    strikeOut,
                    gameMatchEntity,
                    year,
                    isHome
            ));
        }

        return hitterEntities;
    }

    private static List<Map<String, String>> scrapeHitterMap(Page page, Boolean isHome, String year, GameMatchEntity gameMatchEntity) {
        List<ElementHandle> infoRows = page.querySelectorAll("#HitterRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#HitterRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());
        List<Map<String, String>> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            var turn = Short.parseShort(infoRow.querySelector("td").innerText().trim());

            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "", position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            var hitCount = Short.parseShort(stats.get(0).innerText());
            var score = Short.parseShort(stats.get(1).innerText());
            var hit = Short.parseShort(stats.get(2).innerText());
            var homeRun = Short.parseShort(stats.get(3).innerText());
            var hitScore = Short.parseShort(stats.get(4).innerText());
            var ballFour = Short.parseShort(stats.get(5).innerText());
            var strikeOut = Short.parseShort(stats.get(6).innerText());

            Map<String, String> map = new HashMap<>();
            map.put("turn", String.valueOf(turn));
            map.put("name", name);
            map.put("position", position);
            map.put("hitCount", String.valueOf(hitCount));
            map.put("score", String.valueOf(score));
            map.put("hit", String.valueOf(hit));
            map.put("homeRun", String.valueOf(homeRun));
            map.put("hitScore", String.valueOf(hitScore));
            map.put("ballFour", String.valueOf(ballFour));
            map.put("strikeOut", String.valueOf(strikeOut));

            result.add(map);
        }

        return result;
    }

    private static List<PitcherRecordEntity> scrapPitcherEntity(Page page, Boolean isHome, String year, GameMatchEntity gameMatchEntity) {
        List<ElementHandle> infoRows = page.querySelectorAll("#PitcherRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#PitcherRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());

        List<PitcherRecordEntity> pitcherEntities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            var turn = Short.valueOf(infoRow.querySelector("td").innerText().trim());
            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "";
            String position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            var inning = stats.get(0).innerText();
            var pitching = Short.parseShort(stats.get(1).innerText());
            var hit = Short.parseShort(stats.get(4).innerText());
            var homeRun = Short.parseShort(stats.get(5).innerText());
            var ballFour = Short.parseShort(stats.get(6).innerText());
            var strikeOut = Short.parseShort(stats.get(7).innerText());
            var score = Short.parseShort(stats.get(8).innerText());

            pitcherEntities.add(new PitcherRecordEntity(
                    turn,
                    name,
                    position,
                    inning,
                    pitching,
                    ballFour,
                    strikeOut,
                    hit,
                    homeRun,
                    score,
                    gameMatchEntity,
                    year,
                    isHome
            ));
        }

        return pitcherEntities;
    }

    private static List<Map<String, String>> scrapPitcherMap(Page page, Boolean isHome, String year, GameMatchEntity gameMatchEntity) {
        List<ElementHandle> infoRows = page.querySelectorAll("#PitcherRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#PitcherRank .scroll-box table.tbl-new tbody tr");

        int count = Math.min(infoRows.size(), statRows.size());

        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ElementHandle infoRow = infoRows.get(i);
            ElementHandle statRow = statRows.get(i);

            var turn = Short.valueOf(infoRow.querySelector("td").innerText().trim());
            ElementHandle nameCell = infoRow.querySelector("td.name");
            String name = "";
            String position = "";
            if (nameCell != null) {
                ElementHandle p = nameCell.querySelector("p");
                ElementHandle span = nameCell.querySelector("span");
                name = (p != null) ? p.innerText().trim() : "";
                position = (span != null) ? span.innerText().trim() : "";
            }

            List<ElementHandle> stats = statRow.querySelectorAll("td");
            var inning = stats.get(0).innerText();
            var pitching = Short.parseShort(stats.get(1).innerText());
            var hit = Short.parseShort(stats.get(4).innerText());
            var homeRun = Short.parseShort(stats.get(5).innerText());
            var ballFour = Short.parseShort(stats.get(6).innerText());
            var strikeOut = Short.parseShort(stats.get(7).innerText());
            var score = Short.parseShort(stats.get(8).innerText());

            Map<String, String> map = new HashMap<>();
            map.put("turn", String.valueOf(turn));
            map.put("name", name);
            map.put("position", position);
            map.put("inning", inning);
            map.put("pitching", String.valueOf(pitching));
            map.put("ballFour", String.valueOf(ballFour));
            map.put("strikeOut", String.valueOf(strikeOut));
            map.put("hit", String.valueOf(hit));
            map.put("homeRun", String.valueOf(homeRun));
            map.put("score", String.valueOf(score));

           result.add(map);
        }

        return result;
    }
}
