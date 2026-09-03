package kr.co.victoryfairy.community.presentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.co.victoryfairy.community.application.CommunityReportCommandService;
import kr.co.victoryfairy.community.domain.CommunityReport;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CommunityReportControllerContractTest {

    @Test
    void exposesPostAndCommentReportEndpoints() throws Exception {
        var service = mock(CommunityReportCommandService.class);
        when(service.reportPost(7L, 99L, CommunityReport.Reason.SPAM, "반복 게시")).thenReturn(55L);
        when(service.reportComment(7L, 99L, 31L, CommunityReport.Reason.ABUSE, null)).thenReturn(56L);
        var mockMvc = MockMvcBuilders.standaloneSetup(new CommunityReportController(service)).build();
        var account = MemberAccount.builder().id(7L).build();

        mockMvc.perform(post("/api/community/posts/99/reports")
            .requestAttr("accountByToken", account)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reason":"SPAM","detail":"반복 게시"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportId").value(55L));
        mockMvc.perform(post("/api/community/posts/99/comments/31/reports")
            .requestAttr("accountByToken", account)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reason":"ABUSE"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reportId").value(56L));

        verify(service).reportPost(7L, 99L, CommunityReport.Reason.SPAM, "반복 게시");
        verify(service).reportComment(7L, 99L, 31L, CommunityReport.Reason.ABUSE, null);
    }

}
