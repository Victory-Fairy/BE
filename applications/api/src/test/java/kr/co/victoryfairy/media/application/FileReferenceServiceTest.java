package kr.co.victoryfairy.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.storage.db.core.entity.FileEntity;
import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import kr.co.victoryfairy.storage.db.core.repository.FileRefRepository;
import kr.co.victoryfairy.storage.db.core.repository.FileRepository;
import kr.co.victoryfairy.support.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;

class FileReferenceServiceTest {

    @Test
    void returnsSignedUrlWithExistingFileFields() {
        FileRepository fileRepository = mock(FileRepository.class);
        FileRefRepository fileRefRepository = mock(FileRefRepository.class);
        S3PresignedUrlService urlService = mock(S3PresignedUrlService.class);
        FileEntity file = FileEntity.builder()
            .id(7L)
            .path("image/diary/202608")
            .saveName("sample")
            .ext("jpg")
            .build();
        FileRefEntity ref = FileRefEntity.builder().refId(3L).refType(RefType.DIARY).fileEntity(file).build();
        when(fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(RefType.DIARY, 3L))
            .thenReturn(List.of(ref));
        when(urlService.create("image/diary/202608", "sample", "jpg"))
            .thenReturn("https://signed.example/image.jpg");

        var images = new FileReferenceService(fileRepository, fileRefRepository, urlService)
            .findImagesByRefId(RefType.DIARY, 3L);

        assertThat(images).containsExactly(new kr.co.victoryfairy.common.model.CommonDto.ImageDto(7L,
                "image/diary/202608", "sample", "jpg", "https://signed.example/image.jpg"));
    }

}
