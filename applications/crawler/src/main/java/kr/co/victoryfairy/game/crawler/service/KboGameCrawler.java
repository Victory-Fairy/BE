package kr.co.victoryfairy.game.crawler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.TeamEnum;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KboGameCrawler {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final TeamRepository teamRepository;

    private final StadiumRepository stadiumRepository;

    private final GameMatchRepository gameMatchRepository;

    private final HitterRecordRepository hitterRecordRepository;

    private final PitcherRecordRepository pitcherRecordRepository;

    private final MatchScheduleSyncService matchScheduleSyncService;

    private final ObjectMapper objectMapper;

    public KboGameCrawler(TeamRepository teamRepository, StadiumRepository stadiumRepository,
            GameMatchRepository gameMatchRepository, HitterRecordRepository hitterRecordRepository,
            PitcherRecordRepository pitcherRecordRepository, MatchScheduleSyncService matchScheduleSyncService,
            ObjectMapper objectMapper) {
        this.teamRepository = teamRepository;
        this.stadiumRepository = stadiumRepository;
        this.gameMatchRepository = gameMatchRepository;
        this.hitterRecordRepository = hitterRecordRepository;
        this.pitcherRecordRepository = pitcherRecordRepository;
        this.matchScheduleSyncService = matchScheduleSyncService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void crawMatchList(String sYear, String sMonth) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("https://www.koreabaseball.com/Schedule/Schedule.aspx");

            // 연도 설정
            page.selectOption("#ddlYear", sYear);

            int startMonth = StringUtils.hasText(sMonth) ? Integer.parseInt(sMonth) : 3;

            var teamEntities = teamRepository.findAll()
                .stream()
                .filter(KboGameCrawler::hasKboName)
                .collect(Collectors.toMap(TeamEntity::getKboNm, entity -> entity));

            var stadiumEntities = stadiumRepository.findAll()
                .stream()
                .filter(KboGameCrawler::isKboStadium)
                .collect(Collectors.toMap(StadiumEntity::getRegion, entity -> entity));

            List<GameMatchEntity> gameEntities = new ArrayList<>();

            for (int month = startMonth; month <= 12; month++) {
                String monthStr = String.format("%02d", month);
                page.selectOption("#ddlMonth", monthStr);

                for (MatchEnum.MatchType matchType : MatchEnum.MatchType.values()) {

                    if (MatchEnum.MatchType.TIEBREAKER.equals(matchType))
                        continue;

                    page.selectOption("#ddlSeries", matchType.getValue());

                    // 테이블 로딩 대기
                    page.evaluate("getTableGridList();");
                    page.waitForSelector("#tblScheduleList tbody tr");

                    List<ElementHandle> rows = page.querySelectorAll("#tblScheduleList tbody tr");

                    if (rows.isEmpty())
                        break;

                    String lastValidDate = "";
                    for (ElementHandle row : rows) {

                        String rowText = row.innerText().trim();
                        if (rowText.contains("데이터가 없습니다")) {
                            continue;
                        }

                        String date = "";
                        ElementHandle dateCell = row.querySelector("td.day");

                        if (dateCell != null && !dateCell.innerText().isBlank()) {
                            date = dateCell.innerText();
                            lastValidDate = date; // 날짜 업데이트
                        }
                        else {
                            date = lastValidDate; // 이전 날짜 사용
                        }

                        String time = "";
                        ElementHandle timeElement = row.querySelector("td.time b");
                        if (timeElement != null) {
                            time = timeElement.innerText();
                        }

                        // LocalDateTime 변환
                        LocalDateTime matchDateTime = parseDateTime(sYear, date, time);

                        ElementHandle playElement = row.querySelector("td.play");
                        if (playElement == null)
                            continue;

                        List<ElementHandle> teamSpans = playElement.querySelectorAll("span");

                        String away = "";
                        String home = "";
                        Short awayScore = null;
                        Short homeScore = null;
                        String stadiumShortName = "";
                        String stadiumFullName = "";
                        String reason = "";

                        List<ElementHandle> tds = row.querySelectorAll("td");

                        int stadiumIndex = (tds.size() == 9) ? 7 : 6;
                        // int stadiumIndex = 8;
                        int reasonIndex = stadiumIndex + 1;

                        if (teamSpans.size() > 3) {
                            away = safeInnerText(teamSpans, 0);
                            awayScore = Short.parseShort(safeInnerText(teamSpans, 1));
                            homeScore = Short.parseShort(safeInnerText(teamSpans, 3));
                            home = safeInnerText(teamSpans, 4);
                        }
                        else {
                            away = safeInnerText(teamSpans, 0);
                            home = safeInnerText(teamSpans, 2);
                        }

                        stadiumShortName = safeInnerText(tds, stadiumIndex);
                        // stadiumFullName = parseStadium(stadiumShortName);

                        if (stadiumShortName.equals("대전")) {
                            if (sYear.equals(LocalDateTime.now().getYear())) {
                                stadiumShortName = "대전(신)";
                            }
                            else {
                                stadiumShortName = "대전(구)";
                            }
                        }

                        var stadiumEntity = stadiumEntities.get(stadiumShortName);

                        reason = safeInnerText(tds, reasonIndex);

                        ElementHandle replayElement = row.querySelector("td.relay");

                        var matchStatus = MatchEnum.MatchStatus.READY;

                        ElementHandle relayTd = row.querySelector("td.relay a");
                        var matchId = "";
                        var kboAway = TeamEnum.KboTeamNm.fromDesc(away);
                        var kboHome = TeamEnum.KboTeamNm.fromDesc(home);

                        var awayEntity = teamEntities.get(kboAway.name());
                        var homeEntity = teamEntities.get(kboHome.name());

                        if (relayTd == null) {
                            // matchStatus = matchDateTime.isAfter(LocalDateTime.now()) ?
                            // MatchEnum.MatchStatus.READY :
                            // MatchEnum.MatchStatus.CANCELED;
                            matchStatus = reason.equals("-") ? MatchEnum.MatchStatus.READY
                                    : MatchEnum.MatchStatus.CANCELED;
                            String formattedDate = matchDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                            matchId = formattedDate + kboAway + kboHome + 0;
                        }
                        else {
                            String replayRowText = relayTd.innerText().trim();
                            matchStatus = replayRowText.equals("리뷰") ? MatchEnum.MatchStatus.END
                                    : MatchEnum.MatchStatus.READY;

                            ElementHandle reviewLink = row.querySelector("td.relay a#btnReview");
                            ElementHandle previewLink = row.querySelector("td.relay a#btnPreView");

                            String href = "";

                            if (reviewLink != null) {
                                href = reviewLink.getAttribute("href");
                            }
                            else if (previewLink != null) {
                                href = previewLink.getAttribute("href");
                            }

                            if (href != null && href.contains("gameId=")) {
                                String[] parts = href.split("gameId=");
                                if (parts.length > 1) {
                                    String[] gameIdSplit = parts[1].split("&");
                                    matchId = gameIdSplit[0];
                                }
                            }
                        }

                        MatchEnum.SeriesType seriesType = switch (matchType) {
                            case EXHIBITION -> MatchEnum.SeriesType.EXHIBITION;
                            case REGULAR -> MatchEnum.SeriesType.REGULAR;
                            case TIEBREAKER -> MatchEnum.SeriesType.TIEBREAKER;
                            case POST -> null;
                        };

                        if (!StringUtils.hasText(matchId)) {
                            continue;
                        }

                        GameMatchEntity gameMatch = new GameMatchEntity(matchId, MatchEnum.LeagueType.KBO, matchType,
                                seriesType, sYear, matchDateTime, awayEntity, away, awayScore, homeEntity, home,
                                homeScore, stadiumEntity, matchStatus, reason, false, false);

                        gameEntities.add(gameMatch);

                        // 객체 바로 정리 (GC 도움)
                        playElement.dispose();
                        replayElement.dispose();
                        teamSpans.forEach(ElementHandle::dispose);
                        tds.forEach(ElementHandle::dispose);
                    }

                    rows.forEach(ElementHandle::dispose);
                }
            }

            matchScheduleSyncService.sync(gameEntities);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void crawMatchDetail(String sYear) {
        var matches = gameMatchRepository.findBySeason(sYear)
            .stream()
            .sorted(Comparator.comparing(GameMatchEntity::getMatchAt))
            .filter(KboGameCrawler::needsDetailRecovery)
            .toList();
        matches.forEach(match -> crawMatchDetailById(match.getId()));
    }

    static boolean needsDetailRecovery(GameMatchEntity match) {
        return MatchEnum.MatchStatus.END.equals(match.getStatus())
                && !Boolean.TRUE.equals(match.getIsMatchInfoCraw());
    }

    static boolean hasKboName(TeamEntity team) {
        return StringUtils.hasText(team.getKboNm());
    }

    static boolean isKboStadium(StadiumEntity stadium) {
        return stadium.getExternalId() == null;
    }

    @Transactional
    public void crawMatchDetailById(String id) {
        var match = gameMatchRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        try {
            var away = fetchOfficialRecords(match, "T", false);
            var home = fetchOfficialRecords(match, "B", true);
            List<HitterRecordEntity> hitterEntities = new ArrayList<>(away.hitters());
            hitterEntities.addAll(home.hitters());
            List<PitcherRecordEntity> pitcherEntities = new ArrayList<>(away.pitchers());
            pitcherEntities.addAll(home.pitchers());

            hitterRecordRepository.saveAll(hitterEntities);
            pitcherRecordRepository.saveAll(pitcherEntities);

            var updatedMatch = new GameMatchEntity(match.getId(), match.getLeague(), match.getType(), match.getSeries(),
                    match.getSeason(), match.getMatchAt(), match.getAwayTeamEntity(), match.getAwayNm(),
                    match.getAwayScore(), match.getHomeTeamEntity(), match.getHomeNm(), match.getHomeScore(),
                    match.getStadiumEntity(), match.getStatus(), match.getReason(), true, match.getIsSendPush());
            gameMatchRepository.save(updatedMatch);
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to crawl match detail: " + id, e);
        }
    }

    public void crawMatchListByMonth(String sYear, String sMonth) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("https://www.koreabaseball.com/Schedule/Schedule.aspx");

            // 연도 설정
            page.selectOption("#ddlYear", sYear);

            var teamEntities = teamRepository.findAll()
                .stream()
                .filter(KboGameCrawler::hasKboName)
                .collect(Collectors.toMap(TeamEntity::getKboNm, entity -> entity));

            var stadiumEntities = stadiumRepository.findAll()
                .stream()
                .filter(KboGameCrawler::isKboStadium)
                .collect(Collectors.toMap(StadiumEntity::getRegion, entity -> entity));

            List<GameMatchEntity> gameEntities = new ArrayList<>();

            String monthStr = String.format("%02d", Integer.parseInt(sMonth));
            page.selectOption("#ddlMonth", monthStr);

            for (MatchEnum.MatchType matchType : MatchEnum.MatchType.values()) {

                if (MatchEnum.MatchType.TIEBREAKER.equals(matchType))
                    continue;

                page.selectOption("#ddlSeries", matchType.getValue());

                // 테이블 로딩 대기
                page.evaluate("getTableGridList();");
                page.waitForSelector("#tblScheduleList tbody tr");

                List<ElementHandle> rows = page.querySelectorAll("#tblScheduleList tbody tr");

                if (rows.isEmpty())
                    break;

                String lastValidDate = "";
                for (ElementHandle row : rows) {

                    String rowText = row.innerText().trim();
                    if (rowText.contains("데이터가 없습니다")) {
                        continue;
                    }

                    String date = "";
                    ElementHandle dateCell = row.querySelector("td.day");

                    if (dateCell != null && !dateCell.innerText().isBlank()) {
                        date = dateCell.innerText();
                        lastValidDate = date; // 날짜 업데이트
                    }
                    else {
                        date = lastValidDate; // 이전 날짜 사용
                    }

                    String time = "";
                    ElementHandle timeElement = row.querySelector("td.time b");
                    if (timeElement != null) {
                        time = timeElement.innerText();
                    }

                    // LocalDateTime 변환
                    LocalDateTime matchDateTime = parseDateTime(sYear, date, time);

                    ElementHandle playElement = row.querySelector("td.play");
                    if (playElement == null)
                        continue;

                    List<ElementHandle> teamSpans = playElement.querySelectorAll("span");

                    String away = "";
                    String home = "";
                    Short awayScore = null;
                    Short homeScore = null;
                    String stadiumShortName = "";
                    String stadiumFullName = "";
                    String reason = "";

                    List<ElementHandle> tds = row.querySelectorAll("td");

                    int stadiumIndex = (tds.size() == 9) ? 7 : 6;
                    // int stadiumIndex = 8;
                    int reasonIndex = stadiumIndex + 1;

                    if (teamSpans.size() > 3) {
                        away = safeInnerText(teamSpans, 0);
                        awayScore = Short.parseShort(safeInnerText(teamSpans, 1));
                        homeScore = Short.parseShort(safeInnerText(teamSpans, 3));
                        home = safeInnerText(teamSpans, 4);
                    }
                    else {
                        away = safeInnerText(teamSpans, 0);
                        home = safeInnerText(teamSpans, 2);
                    }

                    stadiumShortName = safeInnerText(tds, stadiumIndex);
                    // stadiumFullName = parseStadium(stadiumShortName);

                    if (stadiumShortName.equals("대전")) {
                        if (sYear.equals(LocalDateTime.now().getYear())) {
                            stadiumShortName = "대전(신)";
                        }
                        else {
                            stadiumShortName = "대전(구)";
                        }
                    }

                    var stadiumEntity = stadiumEntities.get(stadiumShortName);

                    reason = safeInnerText(tds, reasonIndex);

                    ElementHandle replayElement = row.querySelector("td.relay");

                    var matchStatus = MatchEnum.MatchStatus.READY;

                    ElementHandle relayTd = row.querySelector("td.relay a");
                    var matchId = "";
                    var kboAway = TeamEnum.KboTeamNm.fromDesc(away);
                    var kboHome = TeamEnum.KboTeamNm.fromDesc(home);

                    var awayEntity = teamEntities.get(kboAway.name());
                    var homeEntity = teamEntities.get(kboHome.name());

                    if (relayTd == null) {
                        // matchStatus = matchDateTime.isAfter(LocalDateTime.now()) ?
                        // MatchEnum.MatchStatus.READY : MatchEnum.MatchStatus.CANCELED;
                        matchStatus = reason.equals("-") ? MatchEnum.MatchStatus.READY : MatchEnum.MatchStatus.CANCELED;
                        String formattedDate = matchDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        matchId = formattedDate + kboAway + kboHome + 0;
                    }
                    else {
                        String replayRowText = relayTd.innerText().trim();
                        matchStatus = replayRowText.equals("리뷰") ? MatchEnum.MatchStatus.END
                                : MatchEnum.MatchStatus.READY;

                        ElementHandle reviewLink = row.querySelector("td.relay a#btnReview");
                        ElementHandle previewLink = row.querySelector("td.relay a#btnPreView");

                        String href = "";

                        if (reviewLink != null) {
                            href = reviewLink.getAttribute("href");
                        }
                        else if (previewLink != null) {
                            href = previewLink.getAttribute("href");
                        }

                        if (href != null && href.contains("gameId=")) {
                            String[] parts = href.split("gameId=");
                            if (parts.length > 1) {
                                String[] gameIdSplit = parts[1].split("&");
                                matchId = gameIdSplit[0];
                            }
                        }
                    }

                    MatchEnum.SeriesType seriesType = switch (matchType) {
                        case EXHIBITION -> MatchEnum.SeriesType.EXHIBITION;
                        case REGULAR -> MatchEnum.SeriesType.REGULAR;
                        case TIEBREAKER -> MatchEnum.SeriesType.TIEBREAKER;
                        case POST -> null;
                    };

                    if (!StringUtils.hasText(matchId)) {
                        continue;
                    }

                    GameMatchEntity gameMatch = new GameMatchEntity(matchId, MatchEnum.LeagueType.KBO, matchType,
                            seriesType, sYear, matchDateTime, awayEntity, away, awayScore, homeEntity, home, homeScore,
                            stadiumEntity, matchStatus, reason, false, false);

                    gameEntities.add(gameMatch);

                    // 객체 바로 정리 (GC 도움)
                    playElement.dispose();
                    replayElement.dispose();
                    teamSpans.forEach(ElementHandle::dispose);
                    tds.forEach(ElementHandle::dispose);
                }

                rows.forEach(ElementHandle::dispose);
            }

            matchScheduleSyncService.sync(gameEntities);
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to crawl match list: " + sYear + "-" + sMonth, e);
        }
    }

    private LocalDateTime parseDateTime(String sYear, String dateStr, String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            timeStr = "00:00";
        }

        String cleanDate = dateStr.split("\\(")[0].trim(); // 예: "10.21"
        String fullDateTimeStr = sYear + "-" + cleanDate.replace(".", "-") + " " + timeStr;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return LocalDateTime.parse(fullDateTimeStr, formatter);
    }

    private String safeInnerText(List<ElementHandle> list, int index) {
        try {
            return list.get(index).innerText();
        }
        catch (Exception e) {
            return "";
        }
    }

    private String parseStadium(String stadiumStr) {
        if (!StringUtils.hasText(stadiumStr)) {
            return null;
        }

        var stadium = "";

        switch (stadiumStr) {
            case "고척" -> stadium = "고척스카이돔";
            case "광주" -> stadium = "광주기아챔피언스필드";
            case "대구" -> stadium = "대구삼성라이온즈파크";
            case "대전" -> stadium = "한화생명이글스파크";
            case "대전(신)" -> stadium = "대전한화생명볼파크";
            case "사직" -> stadium = "사직야구장";
            case "잠실" -> stadium = "서울종합운동장야구장";
            case "수원" -> stadium = "수원KT위즈파크";
            case "문학" -> stadium = "인천SSG랜더스필드";
            case "창원" -> stadium = "창원NC파크";
            default -> stadium = stadiumStr;
        }

        return stadium;
    }

    private OfficialRecords fetchOfficialRecords(GameMatchEntity match, String teamCode, boolean isHome)
            throws Exception {
        String form = "le_id=1&sr_id=" + URLEncoder.encode(match.getSeries().getValue(), StandardCharsets.UTF_8)
                + "&g_id=" + URLEncoder.encode(match.getId(), StandardCharsets.UTF_8) + "&tb_sc=" + teamCode;
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://m.koreabaseball.com/ws/Kbo.asmx/GetLiveRecord"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("KBO record API returned " + response.statusCode());
        }
        return parseOfficialRecords(objectMapper, response.body(), match, isHome);
    }

    static OfficialRecords parseOfficialRecords(ObjectMapper mapper, String response, GameMatchEntity match,
            boolean isHome) throws JsonProcessingException {
        JsonNode root = mapper.readTree(response);
        if (!"100".equals(root.path("code").asText())) {
            throw new IllegalArgumentException("KBO record API failed: " + root.path("msg").asText());
        }

        JsonNode hitterInfo = root.path("listHitter");
        JsonNode hitterRows = mapper.readTree(root.path("tableHitter").asText()).path("rows");
        JsonNode pitcherInfo = root.path("listPitcher");
        JsonNode pitcherRows = mapper.readTree(root.path("tablePitcher").asText()).path("rows");
        if (hitterInfo.isEmpty() || pitcherInfo.isEmpty() || hitterInfo.size() != hitterRows.size()
                || pitcherInfo.size() != pitcherRows.size()) {
            throw new IllegalArgumentException("KBO record API returned incomplete records");
        }

        List<HitterRecordEntity> hitters = new ArrayList<>();
        for (int i = 0; i < hitterInfo.size(); i++) {
            JsonNode info = hitterInfo.get(i);
            JsonNode row = hitterRows.get(i).path("row");
            hitters.add(new HitterRecordEntity(shortValue(info, "RANK"), info.path("NAME").asText(),
                    info.path("SPAN").asText(), shortValue(row, 0), shortValue(row, 1), shortValue(row, 2),
                    shortValue(row, 3), shortValue(row, 4), shortValue(row, 5), shortValue(row, 6), match,
                    match.getSeason(), isHome));
        }

        List<PitcherRecordEntity> pitchers = new ArrayList<>();
        for (int i = 0; i < pitcherInfo.size(); i++) {
            JsonNode info = pitcherInfo.get(i);
            JsonNode row = pitcherRows.get(i).path("row");
            pitchers.add(new PitcherRecordEntity(shortValue(info, "RANK"), info.path("NAME").asText(),
                    info.path("SPAN").asText(), textValue(row, 0), shortValue(row, 1), shortValue(row, 6),
                    shortValue(row, 7), shortValue(row, 4), shortValue(row, 5), shortValue(row, 8), match,
                    match.getSeason(), isHome));
        }
        return new OfficialRecords(hitters, pitchers);
    }

    private static short shortValue(JsonNode node, String field) {
        return Short.parseShort(node.path(field).asText().trim());
    }

    private static short shortValue(JsonNode rows, int index) {
        return Short.parseShort(textValue(rows, index));
    }

    private static String textValue(JsonNode rows, int index) {
        return rows.path(index).path("Text").asText().trim();
    }

    record OfficialRecords(List<HitterRecordEntity> hitters, List<PitcherRecordEntity> pitchers) {
    }

}
