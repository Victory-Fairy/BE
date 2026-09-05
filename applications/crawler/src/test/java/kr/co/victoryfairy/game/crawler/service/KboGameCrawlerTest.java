package kr.co.victoryfairy.game.crawler.service;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.Stadium;
import kr.co.victoryfairy.game.domain.Team;
import org.junit.jupiter.api.Test;

class KboGameCrawlerTest {

    @Test
    void recoversDetailsOnlyForFinishedMatchesWithoutExistingDetails() {
        GameMatch missing = match(MatchEnum.MatchStatus.END, false);
        GameMatch completed = match(MatchEnum.MatchStatus.END, true);
        GameMatch canceled = match(MatchEnum.MatchStatus.CANCELED, false);

        assertThat(KboGameCrawler.needsDetailRecovery(missing)).isTrue();
        assertThat(KboGameCrawler.needsDetailRecovery(completed)).isFalse();
        assertThat(KboGameCrawler.needsDetailRecovery(canceled)).isFalse();
    }

    @Test
    void excludesTeamsWithoutKboCodeFromScheduleLookup() {
        assertThat(
                KboGameCrawler.hasKboName(new Team(1L, "대한민국", null, null, null, null, null, null, true, null, null)))
            .isFalse();
        assertThat(KboGameCrawler.hasKboName(new Team(2L, "LG", "LG", null, null, null, null, null, true, null, null)))
            .isTrue();
    }

    @Test
    void excludesWbcStadiumsFromKboScheduleLookup() {
        assertThat(KboGameCrawler.isKboStadium(new Stadium(null, null, null, null, 4169, true, null, null))).isFalse();
        assertThat(KboGameCrawler.isKboStadium(new Stadium(null, null, null, "잠실", null, true, null, null))).isTrue();
    }

    @Test
    void parsesOfficialKboRecordResponse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var response = mapper.createObjectNode();
        response.put("code", "100");
        response.putArray("listHitter").addObject().put("RANK", "1").put("NAME", "김도영").put("SPAN", "3루수");
        response.putArray("listPitcher").addObject().put("RANK", "1").put("NAME", "네일").put("SPAN", "선발");
        response.put("tableHitter",
                "{\"rows\":[{\"row\":[{\"Text\":\"4\"},{\"Text\":\"1\"},{\"Text\":\"2\"},{\"Text\":\"0\"},{\"Text\":\"1\"},{\"Text\":\"0\"},{\"Text\":\"1\"}]}]}");
        response.put("tablePitcher",
                "{\"rows\":[{\"row\":[{\"Text\":\"6\"},{\"Text\":\"92\"},{\"Text\":\"30\"},{\"Text\":\"62\"},{\"Text\":\"5\"},{\"Text\":\"0\"},{\"Text\":\"2\"},{\"Text\":\"7\"},{\"Text\":\"1\"}]}]}");
        GameMatch match = new GameMatch("game", MatchEnum.LeagueType.KBO, null, MatchEnum.SeriesType.REGULAR, "2026",
                null, null, null, null, null, null, null, null, null, null, false, false, true, null, null);

        var records = KboGameCrawler.parseOfficialRecords(mapper, response.toString(), match, false);

        assertThat(records.hitters()).singleElement().satisfies(hitter -> {
            assertThat(hitter.getName()).isEqualTo("김도영");
            assertThat(hitter.getHit()).isEqualTo((short) 2);
        });
        assertThat(records.pitchers()).singleElement().satisfies(pitcher -> {
            assertThat(pitcher.getName()).isEqualTo("네일");
            assertThat(pitcher.getStrikeOut()).isEqualTo((short) 7);
        });
    }

    private GameMatch match(MatchEnum.MatchStatus status, boolean crawled) {
        return new GameMatch("game", MatchEnum.LeagueType.KBO, null, null, "2026", null, null, null, null, null, null,
                null, null, status, null, crawled, false, true, null, null);
    }

}
