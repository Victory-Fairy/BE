package kr.co.victoryfairy.community.presentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import kr.co.victoryfairy.community.application.CommunityReportAdminService;
import kr.co.victoryfairy.community.application.CommunityView;
import kr.co.victoryfairy.community.domain.CommunityReport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CommunityAdminControllerContractTest {

    @Test
    void listsAndResolvesReports() throws Exception {
        var service = mock(CommunityReportAdminService.class);
        when(service.findReports(CommunityReport.TargetType.POST, CommunityReport.Status.PENDING, null))
            .thenReturn(new CommunityView.Cursor<>(List.of(), null, false));
        var mockMvc = MockMvcBuilders.standaloneSetup(new CommunityAdminController(service)).build();

        mockMvc.perform(get("/admin/community/reports")
            .param("targetType", "POST")
            .param("statusCode", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());
        mockMvc.perform(patch("/admin/community/reports/POST/55")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"ACCEPTED"}
                """))
            .andExpect(status().isOk());

        verify(service).resolve(CommunityReport.TargetType.POST, 55L, CommunityReport.Status.ACCEPTED);
    }

}
