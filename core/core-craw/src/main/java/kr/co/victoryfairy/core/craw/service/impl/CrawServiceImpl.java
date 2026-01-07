package kr.co.victoryfairy.core.craw.service.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.TeamEnum;
import kr.co.victoryfairy.core.craw.service.CrawService;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrawServiceImpl implements CrawService {

    private final TeamRepository teamRepository;

    private final StadiumRepository stadiumRepository;

    private final GameMatchRepository gameMatchRepository;

    private final HitterRecordRepository hitterRecordRepository;

    private final PitcherRecordRepository pitcherRecordRepository;

    private final GameMatchCustomRepository gameMatchCustomRepository;

    public CrawServiceImpl(TeamRepository teamRepository, StadiumRepository stadiumRepository,
            GameMatchRepository gameMatchRepository, HitterRecordRepository hitterRecordRepository,
            PitcherRecordRepository pitcherRecordRepository, GameMatchCustomRepository gameMatchCustomRepository) {
        this.teamRepository = teamRepository;
        this.stadiumRepository = stadiumRepository;
        this.gameMatchRepository = gameMatchRepository;
        this.hitterRecordRepository = hitterRecordRepository;
        this.pitcherRecordRepository = pitcherRecordRepository;
        this.gameMatchCustomRepository = gameMatchCustomRepository;
    }

    @Override
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
                .collect(Collectors.toMap(TeamEntity::getKboNm, entity -> entity));

            var stadiumEntities = stadiumRepository.findAll()
                .stream()
                .collect(Collectors.toMap(StadiumEntity::getRegion, entity -> entity));

            List<GameMatchEntity> gameEntities = new ArrayList<>();

            List<GameMatchEntity> beforeMatches;
            if (StringUtils.hasText(sMonth)) {
                beforeMatches = gameMatchCustomRepository.findByYearAndMonth(sYear, sMonth);
            }
            else {
                beforeMatches = gameMatchRepository.findBySeason(sYear);
            }

            gameMatchRepository.deleteAll(beforeMatches);

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

                        GameMatchEntity gameMatch = new GameMatchEntity(matchId, matchType, seriesType, sYear,
                                matchDateTime, awayEntity, away, awayScore, homeEntity, home, homeScore, stadiumEntity,
                                matchStatus, reason, false, false);

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

            gameMatchRepository.saveAll(gameEntities);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public void crawMatchDetail(String sYear) {
        var matches = gameMatchRepository.findBySeason(sYear)
            .stream()
            .sorted(Comparator.comparing(GameMatchEntity::getMatchAt))
            .filter(match -> MatchEnum.MatchStatus.END.equals(match.getStatus()))
            .toList();

        List<HitterRecordEntity> hitterEntities = new ArrayList<>();
        List<PitcherRecordEntity> pitcherEntities = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            matches.forEach(match -> {
                page.navigate("https://m.koreabaseball.com/Kbo/Live/Record.aspx?p_le_id=1&p_sr_id="
                        + match.getSeries().getValue() + "&p_g_id=" + match.getId());

                page.waitForSelector("#HitterRank table tbody tr"); // 타자
                page.waitForSelector("#PitcherRank table tbody tr"); // 투수
                var awayHitter = this.scrapeHitterTable(page, false, sYear, match);
                page.waitForTimeout(500);
                var awayPitcher = this.scrapPitcherTable(page, false, sYear, match);

                // 탭 클릭해서 원정팀으로 전환
                page.click("#liveRecordSubTabB");
                page.waitForTimeout(1000); // 탭 전환 후 데이터 로딩 기다림

                page.waitForSelector("#HitterRank table tbody tr"); // 타자
                page.waitForSelector("#PitcherRank table tbody tr"); // 투수
                var homeHitter = this.scrapeHitterTable(page, true, sYear, match);
                page.waitForTimeout(500);
                var homePitcher = this.scrapPitcherTable(page, true, sYear, match);

                hitterEntities.addAll(awayHitter);
                hitterEntities.addAll(homeHitter);
                pitcherEntities.addAll(awayPitcher);
                pitcherEntities.addAll(homePitcher);

                var updatedMatch = new GameMatchEntity(match.getId(), match.getType(), match.getSeries(),
                        match.getSeason(), match.getMatchAt(), match.getAwayTeamEntity(), match.getAwayNm(),
                        match.getAwayScore(), match.getHomeTeamEntity(), match.getHomeNm(), match.getHomeScore(),
                        match.getStadiumEntity(), match.getStatus(), match.getReason(), true, match.getIsSendPush());
                gameMatchRepository.save(updatedMatch);
            });

            browser.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        hitterRecordRepository.saveAll(hitterEntities);
        pitcherRecordRepository.saveAll(pitcherEntities);
    }

    @Override
    public void crawMatchDetailById(String id) {
        var match = gameMatchRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        List<HitterRecordEntity> hitterEntities = new ArrayList<>();
        List<PitcherRecordEntity> pitcherEntities = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("https://m.koreabaseball.com/Kbo/Live/Record.aspx?p_le_id=1&p_sr_id="
                    + match.getSeries().getValue() + "&p_g_id=" + match.getId());

            page.waitForSelector("#HitterRank table tbody tr"); // 타자
            page.waitForSelector("#PitcherRank table tbody tr"); // 투수
            var awayHitter = this.scrapeHitterTable(page, false, match.getSeason(), match);
            page.waitForTimeout(500);
            var awayPitcher = this.scrapPitcherTable(page, false, match.getSeason(), match);

            // 탭 클릭해서 원정팀으로 전환
            page.click("#liveRecordSubTabB");
            page.waitForTimeout(1000); // 탭 전환 후 데이터 로딩 기다림

            page.waitForSelector("#HitterRank table tbody tr"); // 타자
            page.waitForSelector("#PitcherRank table tbody tr"); // 투수
            var homeHitter = this.scrapeHitterTable(page, true, match.getSeason(), match);
            page.waitForTimeout(500);
            var homePitcher = this.scrapPitcherTable(page, true, match.getSeason(), match);

            hitterEntities.addAll(awayHitter);
            hitterEntities.addAll(homeHitter);
            pitcherEntities.addAll(awayPitcher);
            pitcherEntities.addAll(homePitcher);

            var updatedMatch = new GameMatchEntity(match.getId(), match.getType(), match.getSeries(), match.getSeason(),
                    match.getMatchAt(), match.getAwayTeamEntity(), match.getAwayNm(), match.getAwayScore(),
                    match.getHomeTeamEntity(), match.getHomeNm(), match.getHomeScore(), match.getStadiumEntity(),
                    match.getStatus(), match.getReason(), true, match.getIsSendPush());
            gameMatchRepository.save(updatedMatch);

            browser.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        hitterRecordRepository.saveAll(hitterEntities);
        pitcherRecordRepository.saveAll(pitcherEntities);
    }

    @Override
    public void crawMatchListByMonth(String sYear, String sMonth) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("https://www.koreabaseball.com/Schedule/Schedule.aspx");

            // 연도 설정
            page.selectOption("#ddlYear", sYear);

            var teamEntities = teamRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TeamEntity::getKboNm, entity -> entity));

            var stadiumEntities = stadiumRepository.findAll()
                .stream()
                .collect(Collectors.toMap(StadiumEntity::getRegion, entity -> entity));

            List<GameMatchEntity> gameEntities = new ArrayList<>();

            var beforeMatches = gameMatchCustomRepository.findByYearAndMonth(sYear, sMonth);

            gameMatchRepository.deleteAll(beforeMatches);

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

                    GameMatchEntity gameMatch = new GameMatchEntity(matchId, matchType, seriesType, sYear,
                            matchDateTime, awayEntity, away, awayScore, homeEntity, home, homeScore, stadiumEntity,
                            matchStatus, reason, false, false);

                    gameEntities.add(gameMatch);

                    // 객체 바로 정리 (GC 도움)
                    playElement.dispose();
                    replayElement.dispose();
                    teamSpans.forEach(ElementHandle::dispose);
                    tds.forEach(ElementHandle::dispose);
                }

                rows.forEach(ElementHandle::dispose);
            }

            gameMatchRepository.saveAll(gameEntities);
        }
        catch (Exception e) {
            e.printStackTrace();
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

    private static List<HitterRecordEntity> scrapeHitterTable(Page page, Boolean isHome, String year,
            GameMatchEntity gameMatchEntity) {
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

            hitterEntities.add(new HitterRecordEntity(turn, name, position, hitCount, score, hit, homeRun, hitScore,
                    ballFour, strikeOut, gameMatchEntity, year, isHome));
        }

        return hitterEntities;
    }

    private static List<PitcherRecordEntity> scrapPitcherTable(Page page, Boolean isHome, String year,
            GameMatchEntity gameMatchEntity) {
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

            pitcherEntities.add(new PitcherRecordEntity(turn, name, position, inning, pitching, ballFour, strikeOut,
                    hit, homeRun, score, gameMatchEntity, year, isHome));
        }

        return pitcherEntities;
    }

}
