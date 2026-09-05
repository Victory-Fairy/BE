package kr.co.victoryfairy.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import kr.co.victoryfairy.media.domain.FileReference;
import kr.co.victoryfairy.media.domain.FileReferenceRepository;
import kr.co.victoryfairy.media.domain.MediaFile;
import kr.co.victoryfairy.media.domain.MediaFileRepository;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import kr.co.victoryfairy.shared.domain.RefType;
import org.junit.jupiter.api.Test;

class FileReferenceServiceTest {

    @Test
    void returnsSignedUrlWithExistingFileFields() {
        var files = mock(MediaFileRepository.class);
        var references = mock(FileReferenceRepository.class);
        var urls = mock(S3PresignedUrlService.class);
        var file = new MediaFile(7L, null, "sample", "image/diary/202608", "jpg", null, true, null, null);
        when(references.findActive(RefType.DIARY, 3L))
            .thenReturn(List.of(FileReference.active(file, 3L, RefType.DIARY)));
        when(urls.create("image/diary/202608", "sample", "jpg")).thenReturn("https://signed.example/image.jpg");

        var images = new FileReferenceService(files, references, urls).findImagesByRefId(RefType.DIARY, 3L);

        assertThat(images).containsExactly(new ImageDto(7L,
                "image/diary/202608", "sample", "jpg", "https://signed.example/image.jpg"));
    }

    @Test
    void ignoresMissingIdsAndSavesExistingReferencesInOneBatch() {
        var files = mock(MediaFileRepository.class);
        var references = mock(FileReferenceRepository.class);
        var file = new MediaFile(7L, null, "sample", "path", "jpg", null, true, null, null);
        when(files.findAllById(List.of(7L, 99L))).thenReturn(List.of(file));

        new FileReferenceService(files, references, mock(S3PresignedUrlService.class))
            .saveFileRefs(RefType.COMMUNITY, 3L, List.of(7L, 99L));

        verify(references).saveReferences(List.of(FileReference.active(file, 3L, RefType.COMMUNITY)));
    }

}
