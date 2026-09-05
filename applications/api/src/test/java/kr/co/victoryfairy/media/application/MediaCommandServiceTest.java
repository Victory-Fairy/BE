package kr.co.victoryfairy.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.media.domain.FileDomain;
import kr.co.victoryfairy.media.infrastructure.S3FileUploader;
import kr.co.victoryfairy.media.infrastructure.persistence.repository.FileRepository;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.media.infrastructure.FileProperties;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class MediaCommandServiceTest {

    @Test
    void uploadsBeforeSavingAndDeletesWorkspaceFile(@TempDir Path storageRoot) throws Exception {
        FileRepository fileRepository = mock(FileRepository.class);
        S3FileUploader uploader = mock(S3FileUploader.class);
        S3PresignedUrlService urlService = mock(S3PresignedUrlService.class);
        when(urlService.create(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn("https://signed.example/image.jpg");
        MediaCommandService service = service(storageRoot, fileRepository, Optional.of(uploader), urlService);

        var response = service.createFile(request());

        verify(uploader).upload(org.mockito.ArgumentMatchers.eq(storageRoot), anyList());
        verify(fileRepository).saveAll(anyList());
        assertThat(fileCount(storageRoot)).isZero();
        assertThat(response.get(0).url()).isEqualTo("https://signed.example/image.jpg");
    }

    @Test
    void doesNotSaveDatabaseRowWhenUploadFails(@TempDir Path storageRoot) {
        FileRepository fileRepository = mock(FileRepository.class);
        S3FileUploader uploader = mock(S3FileUploader.class);
        doThrow(new CustomException(MessageEnum.File.FAIL_UPLOAD)).when(uploader)
            .upload(org.mockito.ArgumentMatchers.eq(storageRoot), anyList());
        MediaCommandService service = service(storageRoot, fileRepository, Optional.of(uploader),
                mock(S3PresignedUrlService.class));

        assertThatThrownBy(() -> service.createFile(request())).isInstanceOf(CustomException.class);
        verify(fileRepository, never()).saveAll(anyList());
    }

    @Test
    void keepsWorkspaceFileWhenS3IsDisabled(@TempDir Path storageRoot) throws Exception {
        FileRepository fileRepository = mock(FileRepository.class);
        MediaCommandService service = service(storageRoot, fileRepository, Optional.empty(),
                mock(S3PresignedUrlService.class));

        service.createFile(request());

        assertThat(fileCount(storageRoot)).isEqualTo(1);
    }

    @Test
    void storesCommunityImagesInDedicatedPath(@TempDir Path storageRoot) {
        var service = service(storageRoot, mock(FileRepository.class), Optional.empty(),
                mock(S3PresignedUrlService.class));

        var response = service.createFile(request(RefType.COMMUNITY));

        assertThat(response.get(0).path()).startsWith("image/community/");
    }

    private MediaCommandService service(Path storageRoot, FileRepository fileRepository,
            Optional<S3FileUploader> uploader, S3PresignedUrlService urlService) {
        FileProperties properties = new FileProperties();
        properties.setStoragePath(storageRoot.toString());
        properties.setImageResizes(new Integer[0]);
        properties.setVideoResizes(new Integer[0]);
        return new MediaCommandService(properties, fileRepository, uploader, urlService);
    }

    private FileDomain.CreateRequest request() {
        return request(RefType.PROFILE);
    }

    private FileDomain.CreateRequest request(RefType refType) {
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", "image".getBytes());
        return new FileDomain.CreateRequest(List.of(file), refType);
    }

    private long fileCount(Path storageRoot) throws Exception {
        try (var paths = Files.walk(storageRoot)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

}
