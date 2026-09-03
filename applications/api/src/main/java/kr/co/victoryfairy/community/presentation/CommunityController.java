package kr.co.victoryfairy.community.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.co.victoryfairy.community.application.CommunityCommandService;
import kr.co.victoryfairy.community.application.CommunityQueryService;
import kr.co.victoryfairy.community.application.CommunityView;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import kr.co.victoryfairy.web.response.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community", description = "커뮤니티")
@SecurityRequirement(name = "accessToken")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
public class CommunityController {

    private final CommunityCommandService commandService;

    private final CommunityQueryService queryService;

    @Operation(summary = "게시글 작성")
    @PostMapping
    public CustomResponse<CommunityApi.WriteResponse> write(@Valid @RequestBody CommunityApi.WriteRequest request) {
        var postId = commandService.write(CurrentRequest.getId(), request.title(), request.content(), request.fileIds());
        return CustomResponse.ok(new CommunityApi.WriteResponse(postId));
    }

    @Operation(summary = "커뮤니티 홈 및 게시글 검색")
    @GetMapping
    public CustomResponse<CommunityView.Cursor<CommunityView.PostPreview>> findPosts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String keyword) {
        return CustomResponse.ok(queryService.findPosts(CurrentRequest.getId(), cursor, keyword));
    }

    @Operation(summary = "게시글 상세")
    @GetMapping("/{postId}")
    public CustomResponse<CommunityView.PostDetail> findPost(@PathVariable Long postId) {
        return CustomResponse.ok(queryService.findPost(CurrentRequest.getId(), postId));
    }

    @Operation(summary = "게시글 댓글 목록")
    @GetMapping("/{postId}/comments")
    public CustomResponse<CommunityView.Cursor<CommunityView.Comment>> findComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor) {
        return CustomResponse.ok(queryService.findComments(CurrentRequest.getId(), postId, cursor));
    }

}
