package kr.co.victoryfairy.community.application;

import java.time.LocalDateTime;
import java.util.List;

import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityPostCommandService {

    private final CommunityRepository repository;

    private final CommunityMemberReader members;

    private final CommunityFileReader files;

    @Transactional
    public Long write(Long memberId, String title, String content, List<Long> requestedFileIds) {
        if (!members.exists(memberId)) {
            throw noResult();
        }

        var fileIds = validateFiles(requestedFileIds);
        var post = repository.save(CommunityPost.create(memberId, title, content));
        if (!fileIds.isEmpty()) {
            repository.savePostFiles(post.id(), fileIds);
        }
        return post.id();
    }

    @Transactional
    public void update(Long memberId, Long postId, String title, String content, List<Long> requestedFileIds) {
        var post = findPost(postId);
        verifyOwner(post, memberId);
        var fileIds = validateFiles(requestedFileIds);

        if (!repository.updatePost(post.update(title, content))) {
            throw noResult();
        }
        repository.replacePostFiles(postId, fileIds);
    }

    @Transactional
    public void delete(Long memberId, Long postId) {
        var post = findPost(postId);
        verifyOwner(post, memberId);
        if (!repository.deletePost(post.delete(LocalDateTime.now()))) {
            throw noResult();
        }
    }

    private CommunityPost findPost(Long postId) {
        return repository.findActivePost(postId).orElseThrow(this::noResult);
    }

    private void verifyOwner(CommunityPost post, Long memberId) {
        if (!post.ownedBy(memberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, MessageEnum.Auth.FAIL_DENY);
        }
    }

    private List<Long> validateFiles(List<Long> requestedFileIds) {
        var fileIds = requestedFileIds == null ? List.<Long>of() : requestedFileIds.stream().distinct().toList();
        if (fileIds.contains(null)) {
            throw noResult();
        }
        if (!fileIds.isEmpty() && files.findExistingIds(fileIds).size() != fileIds.size()) {
            throw noResult();
        }
        return fileIds;
    }

    private CustomException noResult() {
        return new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
    }

}
