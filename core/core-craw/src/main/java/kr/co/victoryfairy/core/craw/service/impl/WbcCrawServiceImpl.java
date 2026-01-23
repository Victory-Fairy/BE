package kr.co.victoryfairy.core.craw.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.WbcEnum;
import kr.co.victoryfairy.core.craw.service.CrawService;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.StadiumEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchRepository;
import kr.co.victoryfairy.storage.db.core.repository.StadiumRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service("wbcCrawService")
public class WbcCrawServiceImpl implements CrawService {

    private static final String MLB_STATS_API = "https://statsapi.mlb.com/api/v1/schedule";

    private static final int WBC_SPORT_ID = 51;

    private final TeamRepository teamRepository;

    private final GameMatchRepository gameMatchRepository;

    private final StadiumRepository stadiumRepository;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    public WbcCrawServiceImpl(TeamRepository teamRepository, GameMatchRepository gameMatchRepository,
            StadiumRepository stadiumRepository) {
        this.teamRepository = teamRepository;
        this.gameMatchRepository = gameMatchRepository;
        this.stadiumRepository = stadiumRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public void crawMatchList(String sYear, String sMonth) {
        // 연도 유효성 검사
        validateYear(sYear);

        // season 파라미터만으로 조회 (날짜 자동)
        String url = String.format("%s?sportId=%d&season=%s", MLB_STATS_API, WBC_SPORT_ID, sYear);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // 해당 연도에 WBC 경기가 없는 경우
            int totalGames = root.has("totalGames") ? root.get("totalGames").asInt() : 0;
            if (totalGames == 0) {
                throw new IllegalArgumentException(sYear + "년에는 WBC 경기가 없습니다.");
            }

            // 기존 데이터 삭제 (경기가 있는 경우에만)
            gameMatchRepository.deleteByLeagueAndSeason(MatchEnum.LeagueType.WBC, sYear);

            JsonNode dates = root.get("dates");
            List<GameMatchEntity> gameEntities = new ArrayList<>();

            if (dates != null && dates.isArray()) {
                for (JsonNode dateNode : dates) {
                    JsonNode games = dateNode.get("games");
                    if (games != null && games.isArray()) {
                        for (JsonNode game : games) {
                            GameMatchEntity entity = parseGame(game, sYear);
                            if (entity != null) {
                                gameEntities.add(entity);
                            }
                        }
                    }
                }
            }

            if (!gameEntities.isEmpty()) {
                gameMatchRepository.saveAll(gameEntities);
            }
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("WBC 경기 정보 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private void validateYear(String sYear) {
        if (sYear == null || sYear.isBlank()) {
            throw new IllegalArgumentException("연도는 필수입니다.");
        }
        try {
            int year = Integer.parseInt(sYear);
            if (year < 2006 || year > 2100) {
                throw new IllegalArgumentException("유효하지 않은 연도입니다. (2006년 이후)");
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("연도 형식이 올바르지 않습니다.");
        }
    }

    private GameMatchEntity parseGame(JsonNode game, String season) {
        try {
            // 본선 경기만 필터링 (Pool, Quarterfinal, Semifinal, Championship)
            String description = getTextOrNull(game, "description");
            if (description == null || !isOfficialGame(description)) {
                return null;
            }

            // gamePk (경기 ID)
            int gamePk = game.get("gamePk").asInt();
            String gameId = String.valueOf(gamePk);

            // gameDate (UTC)
            String gameDateStr = game.get("gameDate").asText();
            ZonedDateTime gameDateTime = ZonedDateTime.parse(gameDateStr, DateTimeFormatter.ISO_DATE_TIME);

            // status
            JsonNode status = game.get("status");
            String detailedState = status != null ? status.get("detailedState").asText() : "Scheduled";
            MatchEnum.MatchStatus matchStatus = mapStatus(detailedState);

            // teams
            JsonNode teams = game.get("teams");
            JsonNode awayTeamNode = teams.get("away");
            JsonNode homeTeamNode = teams.get("home");

            String awayTeamName = awayTeamNode.get("team").get("name").asText();
            String homeTeamName = homeTeamNode.get("team").get("name").asText();

            Short awayScore = awayTeamNode.has("score") ? (short) awayTeamNode.get("score").asInt() : null;
            Short homeScore = homeTeamNode.has("score") ? (short) homeTeamNode.get("score").asInt() : null;

            // 팀 매핑 (MLB 팀명 → Country → TeamEntity, 한글명)
            WbcEnum.Country awayCountry = WbcEnum.Country.fromName(awayTeamName);
            WbcEnum.Country homeCountry = WbcEnum.Country.fromName(homeTeamName);

            TeamEntity awayTeam = findTeamByCountry(awayCountry);
            TeamEntity homeTeam = findTeamByCountry(homeCountry);

            String awayNmKor = awayCountry != null ? awayCountry.getKoreanName() : awayTeamName;
            String homeNmKor = homeCountry != null ? homeCountry.getKoreanName() : homeTeamName;

            // 경기장 매핑
            StadiumEntity stadium = null;
            JsonNode venue = game.get("venue");
            if (venue != null && venue.has("id")) {
                int venueId = venue.get("id").asInt();
                stadium = stadiumRepository.findByExternalId(venueId).orElse(null);
            }

            return GameMatchEntity.builder()
                .id(gameId)
                .league(MatchEnum.LeagueType.WBC)
                .homeTeamEntity(homeTeam)
                .awayTeamEntity(awayTeam)
                .homeNm(homeNmKor)
                .awayNm(awayNmKor)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .stadiumEntity(stadium)
                .status(matchStatus)
                .season(season)
                .matchAt(gameDateTime.toLocalDateTime())
                .build();
        }
        catch (Exception e) {
            return null;
        }
    }

    private boolean isOfficialGame(String description) {
        return description.contains("Pool") || description.contains("Quarterfinal") || description.contains("Semifinal")
                || description.contains("Championship");
    }

    private MatchEnum.MatchStatus mapStatus(String detailedState) {
        if (detailedState.contains("Final") || detailedState.contains("Completed")
                || detailedState.equals("Game Over")) {
            return MatchEnum.MatchStatus.END;
        }
        if (detailedState.equals("In Progress") || detailedState.equals("Warmup")) {
            return MatchEnum.MatchStatus.PROGRESS;
        }
        if (detailedState.equals("Postponed") || detailedState.equals("Cancelled")
                || detailedState.equals("Suspended")) {
            return MatchEnum.MatchStatus.CANCELED;
        }
        return MatchEnum.MatchStatus.READY;
    }

    private TeamEntity findTeamByCountry(WbcEnum.Country country) {
        if (country != null) {
            return teamRepository.findByCountryCode(country.getCode()).orElse(null);
        }
        return null;
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    @Override
    public void crawMatchDetail(String sYear) {
        // WBC는 상세 기록 크롤링 미지원
    }

    @Override
    public void crawMatchDetailById(String id) {
        // WBC는 상세 기록 크롤링 미지원
    }

    @Override
    public void crawMatchListByMonth(String sYear, String sMonth) {
        crawMatchList(sYear, sMonth);
    }

}
