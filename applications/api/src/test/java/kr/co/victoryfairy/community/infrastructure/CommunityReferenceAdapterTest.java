package kr.co.victoryfairy.community.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.shared.application.model.CommonDto;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import kr.co.victoryfairy.media.domain.MediaFile;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.media.domain.MediaFileRepository;
import org.junit.jupiter.api.Test;

class CommunityReferenceAdapterTest {

    @Test
    void readsCommunityAuthorFromExistingMemberAndProfileData() {
        var members = mock(MemberStore.class);
        var fileReferences = mock(FileReferenceService.class);
        var info = new MemberProfile(8L, 7L, null, null, null, "작성자", null, null, null);
        when(members.findProfiles(List.of(7L))).thenReturn(List.of(info));
        when(fileReferences.findImageMapByRefIds(RefType.PROFILE, List.of(7L)))
            .thenReturn(Map.of(7L, new CommonDto.ImageDto(1L, "profile", "saved", "png", "profile-url")));
        var adapter = new CommunityMemberAdapter(members, fileReferences);

        var authors = adapter.findAuthors(List.of(7L));

        assertThat(authors.get(7L).nickname()).isEqualTo("작성자");
        assertThat(authors.get(7L).profileImageUrl()).isEqualTo("profile-url");
    }

    @Test
    void readsExistingFileIdsAndUrlsInOneBoundary() {
        var fileRepository = mock(MediaFileRepository.class);
        var presignedUrls = mock(S3PresignedUrlService.class);
        var first = new MediaFile(20L, null, "a", "community", "png", null, true, null, null);
        var second = new MediaFile(10L, null, "b", "community", "jpg", null, true, null, null);
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
