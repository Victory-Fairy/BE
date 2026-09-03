package kr.co.victoryfairy.community.application;

import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityLikeCommandService {

    private final CommunityRepository repository;

    @Transactional
    public void setPostLike(Long memberId, Long postId, boolean liked) {
        findPost(postId);
        repository.setPostLike(postId, memberId, liked);
    }

    @Transactional
    public void setCommentLike(Long memberId, Long postId, Long commentId, boolean liked) {
        findPost(postId);
        repository.findActiveComment(postId, commentId).orElseThrow(this::noResult);
        repository.setCommentLike(commentId, memberId, liked);
    }

    private void findPost(Long postId) {
        repository.findActivePost(postId).orElseThrow(this::noResult);
    }

    private CustomException noResult() {
        return new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
    }

}
