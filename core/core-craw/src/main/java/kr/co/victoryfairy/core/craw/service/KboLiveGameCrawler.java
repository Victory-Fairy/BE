package kr.co.victoryfairy.core.craw.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.dodn.springboot.core.enums.MatchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KboLiveGameCrawler {

    private static final Logger log = LoggerFactory.getLogger(KboLiveGameCrawler.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern TEAM_CODE = Pattern.compile("emblemR?_(\\w+)\\.png");
    private static final String SCHEDULE_URL = "https://m.koreabaseball.com/Kbo/Schedule.aspx";
    private static final String RECORD_URL = "https://m.koreabaseball.com/Kbo/Live/Record.aspx";
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 Version/15.0 Mobile/15E148 Safari/604.1";

    public List<Snapshot> crawl(LocalDate date, Collection<Target> targets) {
        var targetById = new HashMap<String, Target>();
        targets.forEach(target -> targetById.put(target.id(), target));

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(MOBILE_USER_AGENT)
                .setViewportSize(375, 812));
            return crawlSchedule(context, date, targetById);
        }
    }

    private List<Snapshot> crawlSchedule(BrowserContext context, LocalDate date, Map<String, Target> targetById) {
        String formattedDate = date.format(DATE);
        Page page = context.newPage();
        try {
            page.navigate(SCHEDULE_URL);
            page.evaluate("getGameDateList('" + formattedDate + "')");
            page.waitForSelector("ul#now");

            List<Snapshot> snapshots = new ArrayList<>();
            for (ElementHandle game : page.querySelectorAll("ul#now > li.list")) {
                String id = matchId(formattedDate, game);
                Target target = targetById.get(id);
                if (target == null) {
                    continue;
                }
                try {
                    snapshots.add(snapshot(context, target, game));
                }
                catch (Exception e) {
                    log.error("Failed to crawl live KBO match: {}", id, e);
                }
            }
            return snapshots;
        }
        finally {
            page.close();
        }
    }

    private Snapshot snapshot(BrowserContext context, Target target, ElementHandle game) {
        MatchEnum.MatchStatus status = toStatus(statusClass(game));
        Records records = Records.empty();
        if (status == MatchEnum.MatchStatus.PROGRESS) {
            try {
                records = crawlRecords(context, target);
            }
            catch (Exception e) {
                log.warn("Live records unavailable; keeping score update for {}", target.id(), e);
            }
        }
        return new Snapshot(target.id(), status, text(game.querySelector("span.staus")),
                status == MatchEnum.MatchStatus.CANCELED ? text(game.querySelector(".bottom ul li a")) : "-",
                score(game, ".team.away .score"), score(game, ".team.home .score"), records);
    }

    private Records crawlRecords(BrowserContext context, Target target) {
        Page page = context.newPage();
        try {
            page.navigate(RECORD_URL + "?p_le_id=1&p_sr_id=" + target.series() + "&p_g_id=" + target.id());
            waitForRecords(page);
            var awayHitters = hitterRecords(page);
            var awayPitchers = pitcherRecords(page);

            page.click("#liveRecordSubTabB");
            page.waitForTimeout(1000);
            waitForRecords(page);
            return new Records(awayHitters, awayPitchers, hitterRecords(page), pitcherRecords(page));
        }
        finally {
            page.close();
        }
    }

    private static void waitForRecords(Page page) {
        page.waitForSelector("#HitterRank table tbody tr");
        page.waitForSelector("#PitcherRank table tbody tr");
    }

    static MatchEnum.MatchStatus toStatus(String cssClass) {
        return switch (cssClass) {
            case "ing" -> MatchEnum.MatchStatus.PROGRESS;
            case "end" -> MatchEnum.MatchStatus.END;
            case "cancel" -> MatchEnum.MatchStatus.CANCELED;
            default -> MatchEnum.MatchStatus.READY;
        };
    }

    private static String statusClass(ElementHandle game) {
        String className = game.getAttribute("class");
        return className == null ? "" : className.replace("list", "").trim();
    }

    private static String matchId(String date, ElementHandle game) {
        ElementHandle doubleHeader = game.querySelector("span.dh");
        int order = doubleHeader == null ? 0 : "DH1".equals(doubleHeader.innerText()) ? 1 : 2;
        return date + teamCode(game, ".emb.txt-r img") + teamCode(game, ".emb:not(.txt-r) img") + order;
    }

    private static String teamCode(ElementHandle game, String selector) {
        ElementHandle image = game.querySelector(selector);
        if (image == null) {
            return "";
        }
        var matcher = TEAM_CODE.matcher(image.getAttribute("src"));
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Short score(ElementHandle game, String selector) {
        String value = text(game.querySelector(selector));
        return value.isBlank() ? null : Short.valueOf(value);
    }

    private static String text(ElementHandle element) {
        return element == null ? "" : element.innerText().trim();
    }

    private static List<Map<String, String>> hitterRecords(Page page) {
        List<ElementHandle> infoRows = page.querySelectorAll("#HitterRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#HitterRank .scroll-box table.tbl-new tbody tr");
        List<Map<String, String>> records = new ArrayList<>(Math.min(infoRows.size(), statRows.size()));
        for (int i = 0; i < Math.min(infoRows.size(), statRows.size()); i++) {
            var info = infoRows.get(i);
            var stats = statRows.get(i).querySelectorAll("td");
            Map<String, String> record = player(info);
            record.put("turn", text(info.querySelector("td")));
            record.put("hitCount", text(stats.get(0)));
            record.put("score", text(stats.get(1)));
            record.put("hit", text(stats.get(2)));
            record.put("homeRun", text(stats.get(3)));
            record.put("hitScore", text(stats.get(4)));
            record.put("ballFour", text(stats.get(5)));
            record.put("strikeOut", text(stats.get(6)));
            records.add(record);
        }
        return records;
    }

    private static List<Map<String, String>> pitcherRecords(Page page) {
        List<ElementHandle> infoRows = page.querySelectorAll("#PitcherRank table.tbl-new.fixed tbody tr");
        List<ElementHandle> statRows = page.querySelectorAll("#PitcherRank .scroll-box table.tbl-new tbody tr");
        List<Map<String, String>> records = new ArrayList<>(Math.min(infoRows.size(), statRows.size()));
        for (int i = 0; i < Math.min(infoRows.size(), statRows.size()); i++) {
            var info = infoRows.get(i);
            var stats = statRows.get(i).querySelectorAll("td");
            Map<String, String> record = player(info);
            record.put("turn", text(info.querySelector("td")));
            record.put("inning", text(stats.get(0)));
            record.put("pitching", text(stats.get(1)));
            record.put("hit", text(stats.get(4)));
            record.put("homeRun", text(stats.get(5)));
            record.put("ballFour", text(stats.get(6)));
            record.put("strikeOut", text(stats.get(7)));
            record.put("score", text(stats.get(8)));
            records.add(record);
        }
        return records;
    }

    private static Map<String, String> player(ElementHandle info) {
        Map<String, String> record = new HashMap<>();
        ElementHandle name = info.querySelector("td.name");
        record.put("name", name == null ? "" : text(name.querySelector("p")));
        record.put("position", name == null ? "" : text(name.querySelector("span")));
        return record;
    }

    public record Target(String id, String series) {
    }

    public record Snapshot(String id, MatchEnum.MatchStatus status, String statusDetail, String reason,
            Short awayScore, Short homeScore, Records records) {
    }

    public record Records(List<Map<String, String>> awayHitters, List<Map<String, String>> awayPitchers,
            List<Map<String, String>> homeHitters, List<Map<String, String>> homePitchers) {

        static Records empty() {
            return new Records(List.of(), List.of(), List.of(), List.of());
        }

        boolean hasData() {
            return !awayHitters.isEmpty() || !awayPitchers.isEmpty() || !homeHitters.isEmpty()
                    || !homePitchers.isEmpty();
        }

    }

}
