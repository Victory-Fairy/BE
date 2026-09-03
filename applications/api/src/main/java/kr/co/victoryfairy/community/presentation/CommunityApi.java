package kr.co.victoryfairy.community.presentation;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface CommunityApi {

    record WriteRequest(
            @NotBlank @Size(max = 30) String title,
            @NotBlank @Size(max = 100) String content,
            List<Long> fileIds) {
    }

    record WriteResponse(Long postId) {
    }

}
