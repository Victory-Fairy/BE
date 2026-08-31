package kr.co.victoryfairy.core.api.media.presentation;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.media.application.MediaCommandService;
import kr.co.victoryfairy.core.api.media.domain.FileDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerContractTest {

    @Mock
    private MediaCommandService mediaCommandService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(mediaCommandService)).build();
    }

    @Test
    void keepsTheMultipartUploadContract() throws Exception {
        when(mediaCommandService.createFile(any())).thenReturn(List.of(new FileDomain.Response(1L, "profile.jpg",
                "saved", "image/profile/202608", "jpg", "https://signed.example/profile.jpg")));
        var file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", "image".getBytes());

        mockMvc.perform(multipart("/v2/file/upload").file(file).param("fileRefType", RefType.PROFILE.name())
            .contextPath("/v2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(1L))
            .andExpect(jsonPath("$.data[0].name").value("profile.jpg"))
            .andExpect(jsonPath("$.data[0].url").value("https://signed.example/profile.jpg"));
    }

}
