package kr.co.victoryfairy.community.application;

import java.time.LocalDateTime;

import kr.co.victoryfairy.community.domain.CommunityComment;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityCommentCommandService {

    private final CommunityRepository repository;

    @Transactional
    public Long write(Long memberId, Long postId, String content) {
        findPost(postId);
        return repository.saveComment(CommunityComment.create(postId, memberId, content)).id();
    }

    @Transactional
    public void update(Long memberId, Long postId, Long commentId, String content) {
        findPost(postId);
        var comment = findComment(postId, commentId);
        verifyOwner(comment, memberId);
        repository.saveComment(comment.update(content));
    }

    @Transactional
    public void delete(Long memberId, Long postId, Long commentId) {
        findPost(postId);
        var comment = findComment(postId, commentId);
        verifyOwner(comment, memberId);
        repository.saveComment(comment.delete(LocalDateTime.now()));
    }

    private void findPost(Long postId) {
        repository.findActivePost(postId).orElseThrow(this::noResult);
    }

    private CommunityComment findComment(Long postId, Long commentId) {
        return repository.findActiveComment(postId, commentId).orElseThrow(this::noResult);
    }

    private void verifyOwner(CommunityComment comment, Long memberId) {
        if (!comment.ownedBy(memberId)) {
            throw new CustomException(MessageEnum.Auth.FAIL_DENY);
        }
    }

    private CustomException noResult() {
        return new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
    }

}
