package kr.co.victoryfairy.diary.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.domain.PartnerStore;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.PartnerEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.PartnerRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerDomainServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock private kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryFoodRepository foods;
    @Mock private kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository seatUses;
    @Mock private kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatReviewRepository reviews;
    @Mock private kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository diaries;
    @Mock private kr.co.victoryfairy.game.infrastructure.persistence.repository.SeatRepository seats;

    @Test
    void savesPartnersWithTeamsLoadedInOneBatch() {
        var hanwha = new TeamEntity(13L, "한화", "한화");
        var samsung = new TeamEntity(4L, "삼성", "삼성");
        when(teamRepository.findAllById(List.of(13L, 4L))).thenReturn(List.of(hanwha, samsung));

        var store = new DiaryRelationPersistenceAdapter(foods, partnerRepository, seatUses, reviews, diaries,
                teamRepository, seats);
        store.savePartners(RefType.DIARY, 6000L, List.of(new PartnerStore.Partner("건호", 13L),
                new PartnerStore.Partner("재진", 4L), new PartnerStore.Partner("수민", null)));

        @SuppressWarnings("unchecked")
        var partners = ArgumentCaptor.forClass((Class<List<PartnerEntity>>) (Class<?>) List.class);
        verify(partnerRepository).saveAll(partners.capture());
        assertThat(partners.getValue()).extracting(PartnerEntity::getName).containsExactly("건호", "재진", "수민");
        assertThat(partners.getValue())
            .extracting(partner -> partner.getTeamEntity() == null ? null : partner.getTeamEntity().getId())
            .containsExactly(13L, 4L, null);
    }

}
