package kr.co.victoryfairy.diary.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.diary.domain.SeatUseStore;
import kr.co.victoryfairy.diary.presentation.DiaryDomain;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.domain.Team;
import kr.co.victoryfairy.game.domain.TeamReader;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DiaryCommandServiceTest {
    @Test
    void recordsTheResultWithoutPublishingAWriteDiaryEvent() {
        var diaries = mock(DiaryStore.class);
        var matches = mock(GameMatchRepository.class);
        var members = mock(MemberStore.class);
        var teams = mock(TeamReader.class);
        var gameRecords = mock(GameRecordDomainService.class);
        var match = new GameMatch("match", MatchEnum.LeagueType.KBO, null, null, "2026", LocalDateTime.MIN,
                1L, "away", null, 2L, "home", null, 3L, MatchEnum.MatchStatus.END, null, false, false, true,
                null, null);
        when(members.findMember(787L)).thenReturn(Optional.of(mock(Member.class)));
        when(matches.findDiaryWriteById("match")).thenReturn(Optional.of(match));
        when(teams.findById(13L)).thenReturn(Optional.of(new Team(13L, "한화", null, null, null, null,
                MatchEnum.LeagueType.KBO, null, true, null, null)));
        when(diaries.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Diary value = invocation.getArgument(0);
            return new Diary(9L, value.memberId(), value.gameMatchId(), value.teamId(), value.teamName(),
                    value.viewType(), value.weather(), value.mood(), value.content(), value.rated(), null, null);
        });
        var service = new DiaryCommandService(diaries, mock(SeatUseStore.class), matches,
                mock(GameRecordStore.class), members, teams, mock(FileReferenceService.class),
                mock(DiaryFoodDomainService.class), mock(PartnerDomainService.class), gameRecords);
        var request = new DiaryDomain.WriteRequest(13L, DiaryEnum.ViewType.HOME, "match", List.of(), null, null,
                List.of(), null, "응원 일기", List.of());

        service.writeDiary(787L, request);

        var diary = ArgumentCaptor.forClass(Diary.class);
        verify(gameRecords).record(diary.capture());
        org.assertj.core.api.Assertions.assertThat(diary.getValue().id()).isEqualTo(9L);
    }
}
