package kr.co.victoryfairy.media.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import kr.co.victoryfairy.media.domain.MediaFile;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileEntity;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileRefEntity;
import kr.co.victoryfairy.media.infrastructure.persistence.repository.FileRefRepository;
import kr.co.victoryfairy.media.infrastructure.persistence.repository.FileRepository;
import kr.co.victoryfairy.shared.domain.RefType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MediaPersistenceMapperTest {

    @Test
    void mapsNewFileWithoutInventingAnIdAndReadsGeneratedIdAndAuditFields() {
        var entity = MediaPersistenceMapper.toEntity(new MediaFile(null, "original.jpg", "saved", "image/profile",
                "jpg", 12L, true, null, null));

        assertThat(entity.getId()).isNull();

        var createdAt = LocalDateTime.of(2026, 9, 5, 1, 2);
        var updatedAt = createdAt.plusMinutes(3);
        ReflectionTestUtils.setField(entity, "id", 42L);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        ReflectionTestUtils.setField(entity, "updatedAt", updatedAt);
        ReflectionTestUtils.setField(entity, "isUse", false);

        assertThat(MediaPersistenceMapper.toDomain(entity))
            .isEqualTo(new MediaFile(42L, "original.jpg", "saved", "image/profile", "jpg", 12L, false,
                    createdAt, updatedAt));
    }

    @Test
    void savesReferenceAgainstJpaReferenceWithoutRebuildingTheFileEntity() {
        var files = mock(FileRepository.class);
        var references = mock(FileRefRepository.class);
        var existing = FileEntity.builder().id(42L).build();
        when(files.getReferenceById(42L)).thenReturn(existing);
        var adapter = new MediaPersistenceAdapter(files, references);
        var file = new MediaFile(42L, "original.jpg", "saved", "path", "jpg", 12L, true, null, null);

        adapter.save(kr.co.victoryfairy.media.domain.FileReference.active(file, 7L, RefType.PROFILE));

        var saved = ArgumentCaptor.forClass(FileRefEntity.class);
        verify(references).save(saved.capture());
        assertThat(saved.getValue().getFileEntity()).isSameAs(existing);
        verify(files).getReferenceById(42L);
    }

}
