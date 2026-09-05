package kr.co.victoryfairy.common.presentation;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.web.response.CustomResponse;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Common", description = "공통")
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @GetMapping("/health")
    public CustomResponse<Boolean> healthCheck() {
        return CustomResponse.ok(true);
    }

}
