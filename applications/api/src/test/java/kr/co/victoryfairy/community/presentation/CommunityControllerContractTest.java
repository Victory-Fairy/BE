package kr.co.victoryfairy.community.presentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import kr.co.victoryfairy.community.application.CommunityCommandService;
import kr.co.victoryfairy.community.application.CommunityQueryService;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CommunityControllerContractTest {

    @Test
    void keepsPostCreationRequestAndResponseContract() throws Exception {
        var commands = mock(CommunityCommandService.class);
        var queries = mock(CommunityQueryService.class);
        when(commands.write(7L, "제목", "내용", List.of(20L))).thenReturn(99L);
        var mockMvc = MockMvcBuilders.standaloneSetup(new CommunityController(commands, queries)).build();

        mockMvc.perform(post("/api/community/posts")
            .requestAttr("accountByToken", MemberAccount.builder().id(7L).build())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"제목","content":"내용","fileIds":[20]}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.postId").value(99L));
    }

}
