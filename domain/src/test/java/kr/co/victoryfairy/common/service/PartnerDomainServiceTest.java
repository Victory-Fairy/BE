package kr.co.victoryfairy.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.storage.db.core.entity.PartnerEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.PartnerRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerDomainServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private PartnerDomainService service;

    @Test
    void savesPartnersWithTeamsLoadedInOneBatch() {
        var hanwha = new TeamEntity(13L, "한화", "한화");
        var samsung = new TeamEntity(4L, "삼성", "삼성");
        when(teamRepository.findAllById(List.of(13L, 4L))).thenReturn(List.of(hanwha, samsung));

        service.savePartners(RefType.DIARY, 6000L, List.of(new CommonDto.PartnerSaveRequest("건호", 13L),
                new CommonDto.PartnerSaveRequest("재진", 4L), new CommonDto.PartnerSaveRequest("수민", null)));

        @SuppressWarnings("unchecked")
        var partners = ArgumentCaptor.forClass((Class<List<PartnerEntity>>) (Class<?>) List.class);
        verify(partnerRepository).saveAll(partners.capture());
        assertThat(partners.getValue()).extracting(PartnerEntity::getName).containsExactly("건호", "재진", "수민");
        assertThat(partners.getValue())
            .extracting(partner -> partner.getTeamEntity() == null ? null : partner.getTeamEntity().getId())
            .containsExactly(13L, 4L, null);
    }

}
