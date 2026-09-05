package kr.co.victoryfairy.diary.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.diary.application.DiaryFoodDomainService;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.diary.application.PartnerDomainService;
import kr.co.victoryfairy.diary.presentation.DiaryDomain;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.SeatRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatReviewRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
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
        when(matchRepository.findDiaryWriteById(match.getId())).thenReturn(Optional.of(match));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));

        var service = new DiaryCommandService(diaryRepository, mock(SeatRepository.class),
                mock(SeatUseHistoryRepository.class), mock(SeatReviewRepository.class),
                matchRepository, mock(GameRecordRepository.class), memberRepository, teamRepository,
                mock(FileReferenceService.class), mock(DiaryFoodDomainService.class), mock(PartnerDomainService.class),
                gameRecordService);
        var request = new DiaryDomain.WriteRequest(team.getId(), DiaryEnum.ViewType.HOME, match.getId(), List.of(),
                null, null, List.of(), null, "응원 일기", List.of());

        service.writeDiary(member.getId(), request);

        var diary = ArgumentCaptor.forClass(DiaryEntity.class);
        verify(diaryRepository).save(diary.capture());
        verify(gameRecordService).record(diary.getValue());
    }

}
