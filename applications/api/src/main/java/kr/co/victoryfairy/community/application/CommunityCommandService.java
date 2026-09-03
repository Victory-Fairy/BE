package kr.co.victoryfairy.community.application;

import java.util.List;

import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityCommandService {

    private final CommunityRepository repository;

    private final CommunityMemberReader members;

    private final CommunityFileReader files;

    @Transactional
    public Long write(Long memberId, String title, String content, List<Long> requestedFileIds) {
        if (!members.exists(memberId)) {
            throw noResult();
        }

        var fileIds = requestedFileIds == null ? List.<Long>of() : requestedFileIds.stream().distinct().toList();
        if (fileIds.contains(null)) {
            throw noResult();
        }
        if (!fileIds.isEmpty() && files.findExistingIds(fileIds).size() != fileIds.size()) {
            throw noResult();
        }

        var post = repository.save(CommunityPost.create(memberId, title, content));
        if (!fileIds.isEmpty()) {
            repository.savePostFiles(post.id(), fileIds);
        }
        return post.id();
    }

    private CustomException noResult() {
        return new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
    }

}
