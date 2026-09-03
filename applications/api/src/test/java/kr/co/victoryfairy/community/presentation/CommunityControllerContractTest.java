package kr.co.victoryfairy.community.presentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import kr.co.victoryfairy.community.application.CommunityCommentCommandService;
import kr.co.victoryfairy.community.application.CommunityLikeCommandService;
import kr.co.victoryfairy.community.application.CommunityPostCommandService;
import kr.co.victoryfairy.community.application.CommunityQueryService;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CommunityControllerContractTest {

    @Test
    void keepsPostCreationRequestAndResponseContract() throws Exception {
        var services = services();
        when(services.posts.write(7L, "제목", "내용", List.of(20L))).thenReturn(99L);

        services.mockMvc.perform(post("/api/community/posts")
            .requestAttr("accountByToken", account())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"제목","content":"내용","fileIds":[20]}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.postId").value(99L));
    }

    @Test
    void exposesPostUpdateAndDelete() throws Exception {
        var services = services();

        services.mockMvc.perform(patch("/api/community/posts/99")
            .requestAttr("accountByToken", account())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"수정 제목","content":"수정 내용","fileIds":[20]}
                """))
            .andExpect(status().isOk());
        services.mockMvc.perform(delete("/api/community/posts/99")
            .requestAttr("accountByToken", account()))
            .andExpect(status().isOk());

        verify(services.posts).update(7L, 99L, "수정 제목", "수정 내용", List.of(20L));
        verify(services.posts).delete(7L, 99L);
    }

    @Test
    void exposesCommentMutations() throws Exception {
        var services = services();
        when(services.comments.write(7L, 99L, "댓글")).thenReturn(31L);

        services.mockMvc.perform(post("/api/community/posts/99/comments")
            .requestAttr("accountByToken", account())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content":"댓글"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.commentId").value(31L));
        services.mockMvc.perform(patch("/api/community/posts/99/comments/31")
            .requestAttr("accountByToken", account())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content":"수정 댓글"}
                """))
            .andExpect(status().isOk());
        services.mockMvc.perform(delete("/api/community/posts/99/comments/31")
            .requestAttr("accountByToken", account()))
            .andExpect(status().isOk());

        verify(services.comments).update(7L, 99L, 31L, "수정 댓글");
        verify(services.comments).delete(7L, 99L, 31L);
    }

    @Test
    void setsPostAndCommentLikeState() throws Exception {
        var services = services();

        services.mockMvc.perform(patch("/api/community/posts/99/likes")
            .requestAttr("accountByToken", account())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"liked":true}
                """))
            .andExpect(status().isOk());
        services.mockMvc.perform(patch("/api/community/posts/99/comments/31/likes")
            .requestAttr("accountByToken", account())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"liked":false}
                """))
            .andExpect(status().isOk());

        verify(services.likes).setPostLike(7L, 99L, true);
        verify(services.likes).setCommentLike(7L, 99L, 31L, false);
    }

    private Services services() {
        var posts = mock(CommunityPostCommandService.class);
        var comments = mock(CommunityCommentCommandService.class);
        var likes = mock(CommunityLikeCommandService.class);
        var queries = mock(CommunityQueryService.class);
        var controller = new CommunityController(posts, comments, likes, queries);
        return new Services(posts, comments, likes, MockMvcBuilders.standaloneSetup(controller).build());
    }

    private MemberAccount account() {
        return MemberAccount.builder().id(7L).build();
    }

    private record Services(
            CommunityPostCommandService posts,
            CommunityCommentCommandService comments,
            CommunityLikeCommandService likes,
            MockMvc mockMvc) {
    }

}
