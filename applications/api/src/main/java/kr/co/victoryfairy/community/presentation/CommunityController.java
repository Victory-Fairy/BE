package kr.co.victoryfairy.community.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.co.victoryfairy.community.application.CommunityCommentCommandService;
import kr.co.victoryfairy.community.application.CommunityLikeCommandService;
import kr.co.victoryfairy.community.application.CommunityPostCommandService;
import kr.co.victoryfairy.community.application.CommunityQueryService;
import kr.co.victoryfairy.community.application.CommunityView;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import kr.co.victoryfairy.web.response.CustomResponse;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    private final CommunityPostCommandService postCommands;

    private final CommunityCommentCommandService commentCommands;

    private final CommunityLikeCommandService likeCommands;

    private final CommunityQueryService queryService;

    @Operation(summary = "게시글 작성")
    @PostMapping
    public CustomResponse<CommunityApi.WriteResponse> write(@Valid @RequestBody CommunityApi.WriteRequest request) {
        var postId = postCommands.write(CurrentRequest.getId(), request.title(), request.content(), request.fileIds());
        return CustomResponse.ok(new CommunityApi.WriteResponse(postId));
    }

    @Operation(summary = "게시글 수정")
    @PatchMapping("/{postId}")
    public CustomResponse<MessageEnum> updatePost(@PathVariable Long postId,
            @Valid @RequestBody CommunityApi.WriteRequest request) {
        postCommands.update(CurrentRequest.getId(), postId, request.title(), request.content(), request.fileIds());
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public CustomResponse<MessageEnum> deletePost(@PathVariable Long postId) {
        postCommands.delete(CurrentRequest.getId(), postId);
        return CustomResponse.ok(MessageEnum.Common.DELETE);
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

    @Operation(summary = "댓글 작성")
    @PostMapping("/{postId}/comments")
    public CustomResponse<CommunityApi.WriteCommentResponse> writeComment(@PathVariable Long postId,
            @Valid @RequestBody CommunityApi.CommentRequest request) {
        var commentId = commentCommands.write(CurrentRequest.getId(), postId, request.content());
        return CustomResponse.ok(new CommunityApi.WriteCommentResponse(commentId));
    }

    @Operation(summary = "댓글 수정")
    @PatchMapping("/{postId}/comments/{commentId}")
    public CustomResponse<MessageEnum> updateComment(@PathVariable Long postId, @PathVariable Long commentId,
            @Valid @RequestBody CommunityApi.CommentRequest request) {
        commentCommands.update(CurrentRequest.getId(), postId, commentId, request.content());
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{postId}/comments/{commentId}")
    public CustomResponse<MessageEnum> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {
        commentCommands.delete(CurrentRequest.getId(), postId, commentId);
        return CustomResponse.ok(MessageEnum.Common.DELETE);
    }

    @Operation(summary = "게시글 좋아요 설정")
    @PatchMapping("/{postId}/likes")
    public CustomResponse<MessageEnum> setPostLike(@PathVariable Long postId,
            @Valid @RequestBody CommunityApi.LikeRequest request) {
        likeCommands.setPostLike(CurrentRequest.getId(), postId, request.liked());
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @Operation(summary = "댓글 좋아요 설정")
    @PatchMapping("/{postId}/comments/{commentId}/likes")
    public CustomResponse<MessageEnum> setCommentLike(@PathVariable Long postId, @PathVariable Long commentId,
            @Valid @RequestBody CommunityApi.LikeRequest request) {
        likeCommands.setCommentLike(CurrentRequest.getId(), postId, commentId, request.liked());
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

}
