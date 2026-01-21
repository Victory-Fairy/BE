package kr.co.victoryfairy.core.api.controller;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.CommonDomain;
import kr.co.victoryfairy.core.api.service.CommonService;
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
@DisplayName("CommonController E2E 테스트")
class CommonControllerE2ETest {

	private MockMvc mockMvc;

	@Mock
	private CommonService commonService;

	@InjectMocks
	private CommonController commonController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(commonController)
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.setControllerAdvice(new kr.co.victoryfairy.support.handler.ExceptionAdvice())
			.build();
	}

	@Nested
	@DisplayName("GET /common/team - 팀 목록 조회")
	class FindAllTeamTest {

		@Test
		@DisplayName("league 파라미터 없이 호출하면 전체 팀 목록을 반환한다")
		void findAll_withoutLeague_shouldReturnAllTeams() throws Exception {
			// given
			var kboTeam = new CommonDomain.TeamListResponse(1L, "삼성", "삼성", MatchEnum.LeagueType.KBO, null);
			var wbcTeam = new CommonDomain.TeamListResponse(11L, "대한민국", "WBC", MatchEnum.LeagueType.WBC, "KOR");
			var response = List.of(kboTeam, wbcTeam);

			given(commonService.findAll(eq(null))).willReturn(response);

			// when & then
			mockMvc.perform(get("/common/team")
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(2));

			verify(commonService).findAll(null);
		}

		@Test
		@DisplayName("league=KBO 파라미터로 호출하면 KBO 팀만 반환한다")
		void findAll_withLeagueKBO_shouldReturnKBOTeamsOnly() throws Exception {
			// given
			var team1 = new CommonDomain.TeamListResponse(1L, "삼성", "삼성", MatchEnum.LeagueType.KBO, null);
			var team2 = new CommonDomain.TeamListResponse(2L, "LG", "LG", MatchEnum.LeagueType.KBO, null);
			var response = List.of(team1, team2);

			given(commonService.findAll(eq(MatchEnum.LeagueType.KBO))).willReturn(response);

			// when & then
			mockMvc.perform(get("/common/team")
					.param("league", "KBO")
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].league").value("KBO"))
				.andExpect(jsonPath("$.data[1].league").value("KBO"));

			verify(commonService).findAll(MatchEnum.LeagueType.KBO);
		}

		@Test
		@DisplayName("league=WBC 파라미터로 호출하면 WBC 국가만 반환한다")
		void findAll_withLeagueWBC_shouldReturnWBCCountriesOnly() throws Exception {
			// given
			var team1 = new CommonDomain.TeamListResponse(11L, "대한민국", "WBC", MatchEnum.LeagueType.WBC, "KOR");
			var team2 = new CommonDomain.TeamListResponse(12L, "일본", "WBC", MatchEnum.LeagueType.WBC, "JPN");
			var response = List.of(team1, team2);

			given(commonService.findAll(eq(MatchEnum.LeagueType.WBC))).willReturn(response);

			// when & then
			mockMvc.perform(get("/common/team")
					.param("league", "WBC")
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].league").value("WBC"))
				.andExpect(jsonPath("$.data[0].countryCode").value("KOR"))
				.andExpect(jsonPath("$.data[1].league").value("WBC"))
				.andExpect(jsonPath("$.data[1].countryCode").value("JPN"));

			verify(commonService).findAll(MatchEnum.LeagueType.WBC);
		}

		@Test
		@DisplayName("league=MLB 파라미터로 호출하면 MLB 팀만 반환한다")
		void findAll_withLeagueMLB_shouldReturnMLBTeamsOnly() throws Exception {
			// given
			var response = Collections.<CommonDomain.TeamListResponse>emptyList();

			given(commonService.findAll(eq(MatchEnum.LeagueType.MLB))).willReturn(response);

			// when & then
			mockMvc.perform(get("/common/team")
					.param("league", "MLB")
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(0));

			verify(commonService).findAll(MatchEnum.LeagueType.MLB);
		}

		@Test
		@DisplayName("팀 목록이 비어있으면 빈 배열을 반환한다")
		void findAll_withNoTeams_shouldReturnEmptyArray() throws Exception {
			// given
			var response = Collections.<CommonDomain.TeamListResponse>emptyList();

			given(commonService.findAll(eq(null))).willReturn(response);

			// when & then
			mockMvc.perform(get("/common/team")
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(0));

			verify(commonService).findAll(null);
		}

	}

	@Nested
	@DisplayName("GET /common/health - 헬스체크")
	class HealthCheckTest {

		@Test
		@DisplayName("헬스체크 정상 응답")
		void healthCheck_shouldReturnTrue() throws Exception {
			// when & then
			mockMvc.perform(get("/common/health")
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").value(true));
		}

	}

}
