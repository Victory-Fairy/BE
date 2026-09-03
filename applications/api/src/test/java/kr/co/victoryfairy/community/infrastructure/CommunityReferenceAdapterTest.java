package kr.co.victoryfairy.community.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import kr.co.victoryfairy.storage.db.core.entity.FileEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.repository.FileRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberInfoRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import org.junit.jupiter.api.Test;

class CommunityReferenceAdapterTest {

    @Test
    void readsCommunityAuthorFromExistingMemberAndProfileData() {
        var memberRepository = mock(MemberRepository.class);
        var memberInfoRepository = mock(MemberInfoRepository.class);
        var fileReferences = mock(FileReferenceService.class);
        var member = MemberEntity.builder().id(7L).build();
        var info = MemberInfoEntity.builder().memberEntity(member).nickNm("작성자").build();
        when(memberInfoRepository.findByMemberEntity_IdIn(List.of(7L))).thenReturn(List.of(info));
        when(fileReferences.findImageMapByRefIds(RefType.PROFILE, List.of(7L)))
            .thenReturn(Map.of(7L, new CommonDto.ImageDto(1L, "profile", "saved", "png", "profile-url")));
        var adapter = new CommunityMemberAdapter(memberRepository, memberInfoRepository, fileReferences);

        var authors = adapter.findAuthors(List.of(7L));

        assertThat(authors.get(7L).nickname()).isEqualTo("작성자");
        assertThat(authors.get(7L).profileImageUrl()).isEqualTo("profile-url");
    }

    @Test
    void readsExistingFileIdsAndUrlsInOneBoundary() {
        var fileRepository = mock(FileRepository.class);
        var presignedUrls = mock(S3PresignedUrlService.class);
        var first = FileEntity.builder().id(20L).path("community").saveName("a").ext("png").build();
        var second = FileEntity.builder().id(10L).path("community").saveName("b").ext("jpg").build();
        when(fileRepository.findAllById(List.of(20L, 10L))).thenReturn(List.of(first, second));
        when(presignedUrls.create("community", "a", "png")).thenReturn("url-20");
        when(presignedUrls.create("community", "b", "jpg")).thenReturn("url-10");
        var adapter = new CommunityFileAdapter(fileRepository, presignedUrls);

        assertThat(adapter.findExistingIds(List.of(20L, 10L))).containsExactlyInAnyOrder(20L, 10L);
        assertThat(adapter.findUrls(List.of(20L, 10L)))
            .containsEntry(20L, "url-20")
            .containsEntry(10L, "url-10");
    }

}
