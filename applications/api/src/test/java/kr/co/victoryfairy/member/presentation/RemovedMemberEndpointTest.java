package kr.co.victoryfairy.member.presentation;

import kr.co.victoryfairy.game.application.GameQueryService;
import kr.co.victoryfairy.member.application.MemberAuthService;
import kr.co.victoryfairy.member.application.MemberCommandService;
import kr.co.victoryfairy.member.application.MemberQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RemovedMemberEndpointTest {

    @Mock
    private MemberAuthService authService;

    @Mock
    private MemberCommandService commandService;

    @Mock
    private MemberQueryService queryService;

    @Mock
    private GameQueryService matchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new MemberController(authService, commandService, queryService, matchService))
            .build();
    }

    @Test
    void doesNotExposeTheRetiredFcmTokenEndpoint() throws Exception {
        mockMvc.perform(patch("/api/member/check-fcm").param("fcmToken", "retired-token"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(authService, commandService, queryService);
    }

}
