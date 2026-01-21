package kr.co.victoryfairy.core.api.controller;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("develop")
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchController E2E 테스트")
class MatchControllerE2ETest {

	private MockMvc mockMvc;

	@Mock
	private MatchService matchService;

	@InjectMocks
	private MatchController matchController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(matchController)
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.setControllerAdvice(new kr.co.victoryfairy.support.handler.ExceptionAdvice())
			.build();
	}

	@Nested
	@DisplayName("GET /match/list - 경기 목록 조회")
	class FindListTest {

		@Test
		@DisplayName("날짜만 파라미터로 전달하면 모든 리그 경기를 반환한다")
		void findList_withDateOnly_shouldReturnAllLeagues() throws Exception {
			// given
			String date = "20250930";
			LocalDate localDate = LocalDate.of(2025, 9, 30);

			var response = new MatchDomain.MatchListResponse(localDate, Collections.emptyList());
			given(matchService.findList(eq(localDate), eq(null))).willReturn(response);

			// when & then
			mockMvc.perform(get("/match/list").param("date", date).contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").exists());

			verify(matchService).findList(localDate, null);
		}

		@Test
		@DisplayName("league=KBO 파라미터로 전달하면 KBO 경기만 조회한다")
		void findList_withLeagueKBO_shouldCallServiceWithKBO() throws Exception {
			// given
			String date = "20250930";
			LocalDate localDate = LocalDate.of(2025, 9, 30);

			var response = new MatchDomain.MatchListResponse(localDate, Collections.emptyList());
			given(matchService.findList(eq(localDate), eq(MatchEnum.LeagueType.KBO))).willReturn(response);

			// when & then
			mockMvc.perform(get("/match/list").param("date", date)
				.param("league", "KBO")
				.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(matchService).findList(localDate, MatchEnum.LeagueType.KBO);
		}

		@Test
		@DisplayName("league=WBC 파라미터로 전달하면 WBC 경기만 조회한다")
		void findList_withLeagueWBC_shouldCallServiceWithWBC() throws Exception {
			// given
			String date = "20230308";
			LocalDate localDate = LocalDate.of(2023, 3, 8);

			var response = new MatchDomain.MatchListResponse(localDate, Collections.emptyList());
			given(matchService.findList(eq(localDate), eq(MatchEnum.LeagueType.WBC))).willReturn(response);

			// when & then
			mockMvc.perform(get("/match/list").param("date", date)
				.param("league", "WBC")
				.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(matchService).findList(localDate, MatchEnum.LeagueType.WBC);
		}

		@Test
		@DisplayName("league=MLB 파라미터로 전달하면 MLB 경기만 조회한다")
		void findList_withLeagueMLB_shouldCallServiceWithMLB() throws Exception {
			// given
			String date = "20250930";
			LocalDate localDate = LocalDate.of(2025, 9, 30);

			var response = new MatchDomain.MatchListResponse(localDate, Collections.emptyList());
			given(matchService.findList(eq(localDate), eq(MatchEnum.LeagueType.MLB))).willReturn(response);

			// when & then
			mockMvc.perform(get("/match/list").param("date", date)
				.param("league", "MLB")
				.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			verify(matchService).findList(localDate, MatchEnum.LeagueType.MLB);
		}

		@Test
		@DisplayName("경기 목록이 있으면 정상적으로 반환한다")
		void findList_withMatches_shouldReturnMatchList() throws Exception {
			// given
			String date = "20250930";
			LocalDate localDate = LocalDate.of(2025, 9, 30);

			var awayTeam = new MatchDomain.TeamDto(1L, "삼성", (short) 3, MatchEnum.ResultType.WIN);
			var homeTeam = new MatchDomain.TeamDto(2L, "LG", (short) 2, MatchEnum.ResultType.LOSS);

			var match = new MatchDomain.MatchListDto("20250930SSLG0", localDate, "18:30", "잠실", MatchEnum.MatchStatus.END,
					"종료", awayTeam, homeTeam, false);

			var response = new MatchDomain.MatchListResponse(localDate, List.of(match));
			given(matchService.findList(eq(localDate), eq(null))).willReturn(response);

			// when & then
			mockMvc.perform(get("/match/list").param("date", date).contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.matchList").isArray())
				.andExpect(jsonPath("$.data.matchList[0].id").value("20250930SSLG0"))
				.andExpect(jsonPath("$.data.matchList[0].awayTeam.name").value("삼성"))
				.andExpect(jsonPath("$.data.matchList[0].homeTeam.name").value("LG"));
		}

	}

	@Nested
	@DisplayName("GET /match/today - 오늘 경기 목록 조회")
	class FindTodayMatchTest {

		@Test
		@DisplayName("오늘 경기 목록을 정상적으로 조회한다")
		void findTodayMatch_shouldReturnTodayMatches() throws Exception {
			// given
			var response = new MatchDomain.TodayMatchListResponse(Collections.emptyList());
			given(matchService.findTodayMatch()).willReturn(response);

			// when & then
			mockMvc.perform(get("/match/today").contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").exists());

			verify(matchService).findTodayMatch();
		}

	}

	@Nested
	@DisplayName("GET /match/{id} - 경기 상세 조회")
	class FindByIdTest {

		@Test
		@DisplayName("유효한 ID로 경기 상세를 조회한다")
		void findById_withValidId_shouldReturnMatchInfo() throws Exception {
			// given
			String matchId = "20250930SSLG0";
			LocalDate date = LocalDate.of(2025, 9, 30);

			var awayTeam = new MatchDomain.TeamDto(1L, "삼성", (short) 3, MatchEnum.ResultType.WIN);
			var homeTeam = new MatchDomain.TeamDto(2L, "LG", (short) 2, MatchEnum.ResultType.LOSS);
			var stadium = new MatchDomain.StadiumDto(1L, "잠실", "잠실야구장");

			var response = new MatchDomain.MatchInfoResponse(matchId, date, "18:30", stadium, MatchEnum.MatchStatus.END,
					"종료", awayTeam, homeTeam);
			given(matchService.findById(matchId)).willReturn(response);

			// when & then
			mockMvc.perform(get("/match/{id}", matchId).contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.id").value(matchId))
				.andExpect(jsonPath("$.data.stadium.shortName").value("잠실"));

			verify(matchService).findById(matchId);
		}

	}

}
