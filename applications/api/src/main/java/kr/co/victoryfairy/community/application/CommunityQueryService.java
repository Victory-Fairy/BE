package kr.co.victoryfairy.community.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.victoryfairy.community.domain.CommunityComment;
import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityPostFile;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityQueryService {

    private static final int PAGE_SIZE = 20;

    private final CommunityRepository repository;

    private final CommunityMemberReader members;

    private final CommunityFileReader files;

    public CommunityView.Cursor<CommunityView.PostPreview> findPosts(Long memberId, Long cursor, String keyword) {
        var fetched = repository.findPosts(cursor, normalize(keyword), PAGE_SIZE + 1);
        boolean hasNext = fetched.size() > PAGE_SIZE;
        var posts = fetched.stream().limit(PAGE_SIZE).toList();
        if (posts.isEmpty()) {
            return new CommunityView.Cursor<>(List.of(), null, false);
        }

        var postIds = posts.stream().map(CommunityPost::id).toList();
        var authorByMemberId = authors(posts.stream().map(CommunityPost::memberId).distinct().toList());
        var likeCounts = repository.countPostLikes(postIds);
        var commentCounts = repository.countPostComments(postIds);
        var likedPostIds = repository.findLikedPostIds(memberId, postIds);
        var thumbnailByPostId = thumbnails(repository.findPostFiles(postIds));

        var items = posts.stream()
            .map(post -> new CommunityView.PostPreview(
                post.id(), post.title(), post.content(), thumbnailByPostId.get(post.id()),
                likeCounts.getOrDefault(post.id(), 0L), commentCounts.getOrDefault(post.id(), 0L),
                likedPostIds.contains(post.id()), Objects.equals(post.memberId(), memberId), post.createdAt(),
                authorByMemberId.get(post.memberId())))
            .toList();
        return new CommunityView.Cursor<>(items, hasNext ? posts.getLast().id() : null, hasNext);
    }

    public CommunityView.PostDetail findPost(Long memberId, Long postId) {
        var post = activePost(postId);
        var postIds = List.of(postId);
        var postFiles = repository.findPostFiles(postIds);
        var fileIds = postFiles.stream().map(CommunityPostFile::fileId).toList();
        var urls = fileIds.isEmpty() ? Map.<Long, String>of() : files.findUrls(fileIds);
        var images = postFiles.stream()
            .map(file -> new CommunityView.Image(file.fileId(), urls.get(file.fileId())))
            .toList();
        return new CommunityView.PostDetail(
            post.id(), post.title(), post.content(), images,
            repository.countPostLikes(postIds).getOrDefault(postId, 0L),
            repository.countPostComments(postIds).getOrDefault(postId, 0L),
            repository.findLikedPostIds(memberId, postIds).contains(postId),
            Objects.equals(post.memberId(), memberId), post.createdAt(),
            authors(List.of(post.memberId())).get(post.memberId()), comments(memberId, postId, null));
    }

    public CommunityView.Cursor<CommunityView.Comment> findComments(Long memberId, Long postId, Long cursor) {
        activePost(postId);
        return comments(memberId, postId, cursor);
    }

    private CommunityView.Cursor<CommunityView.Comment> comments(Long memberId, Long postId, Long cursor) {
        var fetched = repository.findComments(postId, cursor, PAGE_SIZE + 1);
        boolean hasNext = fetched.size() > PAGE_SIZE;
        var comments = fetched.stream().limit(PAGE_SIZE).toList();
        var commentIds = comments.stream().map(CommunityComment::id).toList();
        var authorByMemberId = authors(comments.stream().map(CommunityComment::memberId).distinct().toList());
        var likeCounts = repository.countCommentLikes(commentIds);
        var likedIds = repository.findLikedCommentIds(memberId, commentIds);

        var items = comments.stream()
            .map(comment -> new CommunityView.Comment(
                comment.id(), comment.content(), likeCounts.getOrDefault(comment.id(), 0L),
                likedIds.contains(comment.id()), Objects.equals(comment.memberId(), memberId), false,
                comment.createdAt(), authorByMemberId.get(comment.memberId())))
            .toList();
        return new CommunityView.Cursor<>(items, hasNext ? comments.getLast().id() : null, hasNext);
    }

    private CommunityPost activePost(Long postId) {
        return repository.findActivePost(postId)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
    }

    private Map<Long, CommunityView.Author> authors(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        var result = new LinkedHashMap<Long, CommunityView.Author>();
        members.findAuthors(memberIds).forEach((memberId, author) -> result.put(memberId,
            new CommunityView.Author(author.memberId(), author.nickname(), author.profileImageUrl())));
        return result;
    }

    private Map<Long, String> thumbnails(List<CommunityPostFile> postFiles) {
        if (postFiles.isEmpty()) {
            return Map.of();
        }
        var fileIds = postFiles.stream().map(CommunityPostFile::fileId).distinct().toList();
        var urls = files.findUrls(fileIds);
        var result = new LinkedHashMap<Long, String>();
        postFiles.forEach(file -> result.putIfAbsent(file.postId(), urls.get(file.fileId())));
        return result;
    }

    private String normalize(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

}
