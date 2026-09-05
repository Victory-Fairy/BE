package kr.co.victoryfairy.diary.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import org.junit.jupiter.api.Test;

class DiaryPersistenceMapperTest {
    @Test
    void managed_entity_apply_roundtrips_identity_relations_nullable_values_and_rated_state() {
        var member = MemberEntity.builder().id(7L).build();
        var match = GameMatchEntity.builder().id("match").build();
        var team = new TeamEntity(13L, "한화", "한화");
        var entity = DiaryEntity.builder().id(6000L).build();
        var value = new Diary(6000L, 7L, "match", 13L, "한화", DiaryEnum.ViewType.STADIUM, null,
                DiaryEnum.MoodType.HAPPY, null, true, null, null);

        entity.apply(value, member, match, team);

        var mapped = DiaryPersistenceMapper.toDomain(entity);
        assertThat(mapped).usingRecursiveComparison().ignoringFields("createdAt", "updatedAt").isEqualTo(value);
        assertThat(mapped.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(mapped.updatedAt()).isEqualTo(entity.getUpdatedAt());
    }
}
