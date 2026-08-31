package kr.co.victoryfairy.core.api.controller;

import kr.co.victoryfairy.core.api.service.MatchService;
import kr.co.victoryfairy.core.api.service.MemberService;
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
    private MemberService memberService;

    @Mock
    private MatchService matchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MemberController(memberService, matchService)).build();
    }

    @Test
    void doesNotExposeTheRetiredFcmTokenEndpoint() throws Exception {
        mockMvc.perform(patch("/api/member/check-fcm").param("fcmToken", "retired-token"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(memberService);
    }

}
