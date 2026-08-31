package kr.co.victoryfairy.core.api.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.common.service.DiaryFoodDomainService;
import kr.co.victoryfairy.common.service.FileRefDomainService;
import kr.co.victoryfairy.common.service.GameRecordDomainService;
import kr.co.victoryfairy.common.service.PartnerDomainService;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.DiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameRecordRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatReviewRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DiaryCommandServiceTest {

    @Test
    void recordsTheResultWithoutPublishingAWriteDiaryEvent() {
        var diaryRepository = mock(DiaryRepository.class);
        var matchRepository = mock(GameMatchRepository.class);
        var memberRepository = mock(MemberRepository.class);
        var teamRepository = mock(TeamRepository.class);
        var gameRecordService = mock(GameRecordDomainService.class);
        var member = MemberEntity.builder().id(787L).build();
        var team = new TeamEntity(13L, "한화", "한화");
        var match = GameMatchEntity.builder().id("20260827HHSK0").status(MatchEnum.MatchStatus.END).build();
        when(memberRepository.findById(787L)).thenReturn(Optional.of(member));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));

        var service = new DiaryCommandService(diaryRepository, mock(SeatRepository.class),
                mock(SeatUseHistoryRepository.class), mock(SeatReviewRepository.class),
                matchRepository, mock(GameRecordRepository.class), memberRepository, teamRepository,
                mock(FileRefDomainService.class), mock(DiaryFoodDomainService.class), mock(PartnerDomainService.class),
                gameRecordService);
        var request = new DiaryDomain.WriteRequest(team.getId(), DiaryEnum.ViewType.HOME, match.getId(), List.of(),
                null, null, List.of(), null, "응원 일기", List.of());

        service.writeDiary(member.getId(), request);

        var diary = ArgumentCaptor.forClass(DiaryEntity.class);
        verify(diaryRepository).save(diary.capture());
        verify(gameRecordService).record(diary.getValue());
    }

}
