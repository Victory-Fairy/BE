package kr.co.victoryfairy.core.file.service.impl;

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

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.file.domain.FileDomain;
import kr.co.victoryfairy.core.file.service.S3FileUploader;
import kr.co.victoryfairy.storage.db.core.repository.FileRefRepository;
import kr.co.victoryfairy.storage.db.core.repository.FileRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.properties.FileProperties;
import kr.co.victoryfairy.support.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileServiceImplTest {

    @Test
    void uploadsBeforeSavingAndDeletesWorkspaceFile(@TempDir Path storageRoot) throws Exception {
        FileRepository fileRepository = mock(FileRepository.class);
        S3FileUploader uploader = mock(S3FileUploader.class);
        S3PresignedUrlService urlService = mock(S3PresignedUrlService.class);
        when(urlService.create(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn("https://signed.example/image.jpg");
        FileServiceImpl service = service(storageRoot, fileRepository, Optional.of(uploader), urlService);

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
        FileServiceImpl service = service(storageRoot, fileRepository, Optional.of(uploader),
                mock(S3PresignedUrlService.class));

        assertThatThrownBy(() -> service.createFile(request())).isInstanceOf(CustomException.class);
        verify(fileRepository, never()).saveAll(anyList());
    }

    @Test
    void keepsWorkspaceFileWhenS3IsDisabled(@TempDir Path storageRoot) throws Exception {
        FileRepository fileRepository = mock(FileRepository.class);
        FileServiceImpl service = service(storageRoot, fileRepository, Optional.empty(),
                mock(S3PresignedUrlService.class));

        service.createFile(request());

        assertThat(fileCount(storageRoot)).isEqualTo(1);
    }

    private FileServiceImpl service(Path storageRoot, FileRepository fileRepository,
            Optional<S3FileUploader> uploader, S3PresignedUrlService urlService) {
        FileProperties properties = new FileProperties();
        properties.setStoragePath(storageRoot.toString());
        properties.setImageResizes(new Integer[0]);
        properties.setVideoResizes(new Integer[0]);
        return new FileServiceImpl(properties, fileRepository, mock(FileRefRepository.class), uploader, urlService);
    }

    private FileDomain.CreateRequest request() {
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", "image".getBytes());
        return new FileDomain.CreateRequest(List.of(file), RefType.PROFILE);
    }

    private long fileCount(Path storageRoot) throws Exception {
        try (var paths = Files.walk(storageRoot)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

}
