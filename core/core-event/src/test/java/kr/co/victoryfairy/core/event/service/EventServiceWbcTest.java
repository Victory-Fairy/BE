package kr.co.victoryfairy.core.event.service;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.EventType;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.event.model.EventDomain;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("EventService WBC 통합 테스트")
class EventServiceWbcTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private DiaryRepository diaryRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private GameMatchRepository matchRepository;
    @Mock
    private GameRecordRepository gameRecordRepository;
    @Mock
    private WinningRateRepository winningRateRepository;

    @InjectMocks
    private EventService eventService;

    private MemberEntity memberEntity;
    private TeamEntity koreaTeam;
    private TeamEntity japanTeam;
    private TeamEntity samsungTeam;
    private TeamEntity lgTeam;

    @BeforeEach
    void setUp() {
        // 회원
        memberEntity = MemberEntity.builder()
                .id(1L)
                .build();

        // WBC 국가 (TeamEntity 재사용 - 생성자 사용)
        koreaTeam = createTeamEntity(101L, "대한민국", MatchEnum.LeagueType.WBC, "KOR");
        japanTeam = createTeamEntity(102L, "일본", MatchEnum.LeagueType.WBC, "JPN");

        // KBO 팀
        samsungTeam = createTeamEntity(1L, "삼성", MatchEnum.LeagueType.KBO, null);
        lgTeam = createTeamEntity(2L, "LG", MatchEnum.LeagueType.KBO, null);
    }

    private TeamEntity createTeamEntity(Long id, String name, MatchEnum.LeagueType league, String countryCode) {
        // TeamEntity는 builder가 없으므로 리플렉션 또는 테스트용 팩토리 사용
        // 여기서는 기본 생성자 + setter 대신 ID와 name을 가진 생성자 사용
        return new TeamEntity(id, name, name);
    }

    @Test
    @DisplayName("WBC 경기 일기 처리 - 승리 케이스 (leagueType=WBC 저장)")
    void processDiary_wbcWin_shouldSaveLeagueTypeWbc() {
        // given
        var gameId = "WBC2025KORJPN";
        var eventDto = new EventDomain.WriteEventDto(gameId, 1L, 1L, EventType.DIARY);

        var matchEntity = GameMatchEntity.builder()
                .id(gameId)
                .league(MatchEnum.LeagueType.WBC)
                .awayTeamEntity(koreaTeam)
                .homeTeamEntity(japanTeam)
                .awayScore((short) 5)
                .homeScore((short) 3)
                .status(MatchEnum.MatchStatus.END)
                .season("2025")
                .matchAt(LocalDateTime.of(2025, 3, 15, 19, 0))
                .build();

        var diaryEntity = DiaryEntity.builder()
                .id(1L)
                .teamEntity(koreaTeam)
                .gameMatchEntity(matchEntity)
                .viewType(DiaryEnum.ViewType.HOME)
                .isRated(false)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(memberEntity));
        given(matchRepository.findById(gameId)).willReturn(Optional.of(matchEntity));
        given(diaryRepository.findById(1L)).willReturn(Optional.of(diaryEntity));
        given(teamRepository.findById(101L)).willReturn(Optional.of(koreaTeam));

        // when
        var result = eventService.processDiary(eventDto);

        // then
        assertThat(result).isTrue();

        ArgumentCaptor<GameRecordEntity> captor = ArgumentCaptor.forClass(GameRecordEntity.class);
        verify(gameRecordRepository).save(captor.capture());

        GameRecordEntity savedRecord = captor.getValue();
        assertThat(savedRecord.getLeagueType()).isEqualTo(MatchEnum.LeagueType.WBC);
        assertThat(savedRecord.getTeamEntity()).isEqualTo(koreaTeam);
        assertThat(savedRecord.getOpponentTeamEntity()).isEqualTo(japanTeam);
        assertThat(savedRecord.getResultType()).isEqualTo(MatchEnum.ResultType.WIN);
    }

    @Test
    @DisplayName("WBC 경기 일기 처리 - 패배 케이스")
    void processDiary_wbcLoss_shouldSaveLeagueTypeWbc() {
        // given
        var gameId = "WBC2025KORJPN";
        var eventDto = new EventDomain.WriteEventDto(gameId, 1L, 1L, EventType.DIARY);

        var matchEntity = GameMatchEntity.builder()
                .id(gameId)
                .league(MatchEnum.LeagueType.WBC)
                .awayTeamEntity(koreaTeam)
                .homeTeamEntity(japanTeam)
                .awayScore((short) 2)  // 한국 2점
                .homeScore((short) 5)  // 일본 5점
                .status(MatchEnum.MatchStatus.END)
                .season("2025")
                .matchAt(LocalDateTime.of(2025, 3, 15, 19, 0))
                .build();

        var diaryEntity = DiaryEntity.builder()
                .id(1L)
                .teamEntity(koreaTeam)
                .gameMatchEntity(matchEntity)
                .viewType(DiaryEnum.ViewType.HOME)
                .isRated(false)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(memberEntity));
        given(matchRepository.findById(gameId)).willReturn(Optional.of(matchEntity));
        given(diaryRepository.findById(1L)).willReturn(Optional.of(diaryEntity));
        given(teamRepository.findById(101L)).willReturn(Optional.of(koreaTeam));

        // when
        var result = eventService.processDiary(eventDto);

        // then
        assertThat(result).isTrue();

        ArgumentCaptor<GameRecordEntity> captor = ArgumentCaptor.forClass(GameRecordEntity.class);
        verify(gameRecordRepository).save(captor.capture());

        GameRecordEntity savedRecord = captor.getValue();
        assertThat(savedRecord.getLeagueType()).isEqualTo(MatchEnum.LeagueType.WBC);
        assertThat(savedRecord.getResultType()).isEqualTo(MatchEnum.ResultType.LOSS);
    }

    @Test
    @DisplayName("KBO 경기 일기 처리 - leagueType=KBO 저장")
    void processDiary_kbo_shouldSaveLeagueTypeKbo() {
        // given
        var gameId = "20250930SSLG0";
        var eventDto = new EventDomain.WriteEventDto(gameId, 1L, 1L, EventType.DIARY);

        var matchEntity = GameMatchEntity.builder()
                .id(gameId)
                .league(MatchEnum.LeagueType.KBO)
                .awayTeamEntity(samsungTeam)
                .homeTeamEntity(lgTeam)
                .awayScore((short) 4)
                .homeScore((short) 2)
                .status(MatchEnum.MatchStatus.END)
                .season("2025")
                .matchAt(LocalDateTime.of(2025, 9, 30, 18, 30))
                .build();

        var diaryEntity = DiaryEntity.builder()
                .id(1L)
                .teamEntity(samsungTeam)
                .gameMatchEntity(matchEntity)
                .viewType(DiaryEnum.ViewType.HOME)
                .isRated(false)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(memberEntity));
        given(matchRepository.findById(gameId)).willReturn(Optional.of(matchEntity));
        given(diaryRepository.findById(1L)).willReturn(Optional.of(diaryEntity));
        given(teamRepository.findById(1L)).willReturn(Optional.of(samsungTeam));

        // when
        var result = eventService.processDiary(eventDto);

        // then
        assertThat(result).isTrue();

        ArgumentCaptor<GameRecordEntity> captor = ArgumentCaptor.forClass(GameRecordEntity.class);
        verify(gameRecordRepository).save(captor.capture());

        GameRecordEntity savedRecord = captor.getValue();
        assertThat(savedRecord.getLeagueType()).isEqualTo(MatchEnum.LeagueType.KBO);
        assertThat(savedRecord.getTeamEntity()).isEqualTo(samsungTeam);
        assertThat(savedRecord.getOpponentTeamEntity()).isEqualTo(lgTeam);
        assertThat(savedRecord.getResultType()).isEqualTo(MatchEnum.ResultType.WIN);
    }

    @Test
    @DisplayName("무승부 경기 처리")
    void processDiary_draw_shouldSaveResultTypeDraw() {
        // given
        var gameId = "WBC2025KORJPN";
        var eventDto = new EventDomain.WriteEventDto(gameId, 1L, 1L, EventType.DIARY);

        var matchEntity = GameMatchEntity.builder()
                .id(gameId)
                .league(MatchEnum.LeagueType.WBC)
                .awayTeamEntity(koreaTeam)
                .homeTeamEntity(japanTeam)
                .awayScore((short) 3)
                .homeScore((short) 3)  // 무승부
                .status(MatchEnum.MatchStatus.END)
                .season("2025")
                .matchAt(LocalDateTime.of(2025, 3, 15, 19, 0))
                .build();

        var diaryEntity = DiaryEntity.builder()
                .id(1L)
                .teamEntity(koreaTeam)
                .gameMatchEntity(matchEntity)
                .viewType(DiaryEnum.ViewType.HOME)
                .isRated(false)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(memberEntity));
        given(matchRepository.findById(gameId)).willReturn(Optional.of(matchEntity));
        given(diaryRepository.findById(1L)).willReturn(Optional.of(diaryEntity));
        given(teamRepository.findById(101L)).willReturn(Optional.of(koreaTeam));

        // when
        var result = eventService.processDiary(eventDto);

        // then
        assertThat(result).isTrue();

        ArgumentCaptor<GameRecordEntity> captor = ArgumentCaptor.forClass(GameRecordEntity.class);
        verify(gameRecordRepository).save(captor.capture());

        GameRecordEntity savedRecord = captor.getValue();
        assertThat(savedRecord.getResultType()).isEqualTo(MatchEnum.ResultType.DRAW);
    }

    @Test
    @DisplayName("이미 처리된 일기는 스킵")
    void processDiary_alreadyRated_shouldReturnTrue() {
        // given
        var gameId = "WBC2025KORJPN";
        var eventDto = new EventDomain.WriteEventDto(gameId, 1L, 1L, EventType.DIARY);

        var diaryEntity = DiaryEntity.builder()
                .id(1L)
                .isRated(true)  // 이미 처리됨
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(memberEntity));
        given(matchRepository.findById(gameId)).willReturn(Optional.empty());
        given(diaryRepository.findById(1L)).willReturn(Optional.of(diaryEntity));

        // when
        var result = eventService.processDiary(eventDto);

        // then
        assertThat(result).isTrue();  // 이미 처리된 경우 true 반환
    }
}
