package kr.co.victoryfairy.game.crawler.service;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.StadiumEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import org.junit.jupiter.api.Test;

class KboGameCrawlerTest {

    @Test
    void recoversDetailsOnlyForFinishedMatchesWithoutExistingDetails() {
        GameMatchEntity missing = GameMatchEntity.builder()
            .status(MatchEnum.MatchStatus.END)
            .isMatchInfoCraw(false)
            .build();
        GameMatchEntity completed = GameMatchEntity.builder()
            .status(MatchEnum.MatchStatus.END)
            .isMatchInfoCraw(true)
            .build();
        GameMatchEntity canceled = GameMatchEntity.builder()
            .status(MatchEnum.MatchStatus.CANCELED)
            .isMatchInfoCraw(false)
            .build();

        assertThat(KboGameCrawler.needsDetailRecovery(missing)).isTrue();
        assertThat(KboGameCrawler.needsDetailRecovery(completed)).isFalse();
        assertThat(KboGameCrawler.needsDetailRecovery(canceled)).isFalse();
    }

    @Test
    void excludesTeamsWithoutKboCodeFromScheduleLookup() {
        assertThat(KboGameCrawler.hasKboName(new TeamEntity(1L, "대한민국", null))).isFalse();
        assertThat(KboGameCrawler.hasKboName(new TeamEntity(2L, "LG", "LG"))).isTrue();
    }

    @Test
    void excludesWbcStadiumsFromKboScheduleLookup() {
        assertThat(KboGameCrawler.isKboStadium(StadiumEntity.builder().externalId(4169).build())).isFalse();
        assertThat(KboGameCrawler.isKboStadium(StadiumEntity.builder().region("잠실").build())).isTrue();
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
        GameMatchEntity match = GameMatchEntity.builder().season("2026").build();

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

}
