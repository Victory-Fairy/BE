package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.MyPageDomain;
import kr.co.victoryfairy.core.api.service.MyPageService;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.WithdrawalReasonEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final MemberRepository memberRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberCustomRepository memberCustomRepository;
    private final GameRecordRepository gameRecordRepository;

    private final DiaryRepository diaryRepository;
    private final DiaryFoodRepository diaryFoodRepository;
    private final PartnerRepository partnerRepository;
    private final SeatUseHistoryRepository seatUseHistoryRepository;
    private final SeatReviewRepository seatReviewRepository;

    private final WithdrawalReasonRepository withdrawalRepository;

    @Override
    public MyPageDomain.MemberInfoForMyPageResponse findMemberInfoForMyPage() {
        var id = RequestUtils.getId();

        if (id == null) {
            return new MyPageDomain.MemberInfoForMyPageResponse(
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        var member = memberCustomRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamDto = member.getTeamId() != null ? new MyPageDomain.TeamDto(member.getTeamId(), member.getTeamName(), member.getSponsorNm()) : null;
        var fileDto = member.getFileId() != null ? new MyPageDomain.ImageDto(member.getFileId(), member.getPath(), member.getSaveName(), member.getExt()) : null;

        return new MyPageDomain.MemberInfoForMyPageResponse(
                member.getId(),
                fileDto,
                member.getNickNm(),
                member.getSnsType(),
                teamDto
        );
    }

    @Override
    public MyPageDomain.VictoryPowerResponse findVictoryPower(String season) {
        var id = RequestUtils.getId();

        if (id == null) {
            return new MyPageDomain.VictoryPowerResponse(
                    null,
                    null
            );
        }

        var memberEntity = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var year = StringUtils.hasText(season) ? season : String.valueOf(LocalDate.now().getYear());

        var recordList = gameRecordRepository.findByMemberAndSeason(memberEntity, year);

        var stadiumRecord = recordList.stream()
                .filter(record -> record.getViewType() == DiaryEnum.ViewType.STADIUM)
                .toList();

        var homeRecord = recordList.stream()
                .filter(record -> record.getViewType() == DiaryEnum.ViewType.HOME)
                .toList();

        var power = this.getPower(stadiumRecord, homeRecord);

        short level = 0;
        if (power >= 0 && power < 20) {
            level = 1; // Lv 1
        } else if (power >= 20 && power < 40) {
            level = 2; // Lv 2
        } else if (power >= 40 && power < 60) {
            level = 3; // Lv 3
        } else if (power >= 60 && power < 80) {
            level = 4; // Lv 4
        } else if (power >= 80) {
            level = 5; // 만렙
        }

        return new MyPageDomain.VictoryPowerResponse(level, power);
    }

    @Override
    public MyPageDomain.ReportResponse findReport(String season) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var memberEntity = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        if (!StringUtils.hasText(season)) {
            season = String.valueOf(LocalDate.now().getYear());
        }

        var recordList = gameRecordRepository.findByMemberAndSeason(memberEntity, season).stream()
                .sorted(Comparator.comparing(entity -> entity.getGameMatchEntity().getMatchAt()))
                .toList();

        if (recordList.isEmpty()) {
            return new MyPageDomain.ReportResponse(null, null, null);
        }

        short stadiumWinAvg = 0;
        short homeWinAvg = 0;

        Map<String, MyPageDomain.VisitInfoDto> stadiumVisitCount = new HashMap<>();

        short homeGameCount = 0;
        short homeGameWinCount = 0;
        short stadiumGameCount = 0;
        short stadiumGameWinCount = 0;

        short currentStreak = 0;
        short maxStreak = 0;

        var winMap = new HashMap<String, MyPageDomain.TeamResultDto>();
        var loseMap = new HashMap<String, MyPageDomain.TeamResultDto>();

        for (var record : recordList) {
            var opponent = record.getOpponentTeamName();
            var matchAt = record.getGameMatchEntity().getMatchAt();
            var result = record.getResultType();

            if (result == MatchEnum.ResultType.WIN) {
                winMap.merge(opponent, new MyPageDomain.TeamResultDto(1, matchAt), (oldVal, newVal) ->
                        new MyPageDomain.TeamResultDto(
                                oldVal.count() + 1,
                                matchAt.isAfter(oldVal.lastPlayedAt()) ? matchAt : oldVal.lastPlayedAt()
                        )
                );
            } else if (result == MatchEnum.ResultType.LOSS) {
                loseMap.merge(opponent, new MyPageDomain.TeamResultDto(1, matchAt), (oldVal, newVal) ->
                        new MyPageDomain.TeamResultDto(
                                oldVal.count() + 1,
                                matchAt.isAfter(oldVal.lastPlayedAt()) ? matchAt : oldVal.lastPlayedAt()
                        )
                );
            }
        }

        var stadiumRecord = recordList.stream()
                .filter(record -> record.getViewType() == DiaryEnum.ViewType.STADIUM)
                .toList();

        MyPageDomain.ViewTypeDto stadiumViewDto = null;
        if (!stadiumRecord.isEmpty()) {
            var winCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getStatus().equals(MatchEnum.MatchStatus.END) && record.getResultType().equals(MatchEnum.ResultType.WIN))
                    .count();

            var loseCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getStatus().equals(MatchEnum.MatchStatus.END) && record.getResultType().equals(MatchEnum.ResultType.LOSS))
                    .count();

            var drawCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getStatus() != MatchEnum.MatchStatus.CANCELED && record.getResultType().equals(MatchEnum.ResultType.DRAW))
                    .count();

            var cancelCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getStatus() == MatchEnum.MatchStatus.CANCELED)
                    .count();

            var validGameCount = winCount + loseCount;

            if (validGameCount > 0) {
                double avg = (double) winCount / validGameCount * 100;
                stadiumWinAvg = (short) Math.round(avg);  // 소수점 첫째자리 반올림
            }

            stadiumViewDto = new MyPageDomain.ViewTypeDto(
                    stadiumWinAvg,
                    winCount,
                    loseCount,
                    drawCount,
                    cancelCount
            );

            // 직관 데이터
            for (var record : stadiumRecord) {
                var stadiumEntity = record.getStadiumEntity();
                var result = record.getResultType();
                var matchEntity = record.getGameMatchEntity();

                // 최대 방문 구장 처리
                stadiumVisitCount.merge(stadiumEntity.getFullName(), new MyPageDomain.VisitInfoDto(1, matchEntity.getMatchAt()), (oldVal, newVal) -> {
                    return new MyPageDomain.VisitInfoDto(
                            oldVal.count() + 1,
                            matchEntity.getMatchAt().isAfter(oldVal.lastVisited()) ? matchEntity.getMatchAt() : oldVal.lastVisited()
                    );
                });

                var myTeam = record.getTeamEntity();

                // 원정, 홈 여부
                var isHome = matchEntity.getHomeTeamEntity().getId().equals(myTeam.getId());

                if (isHome) {
                    homeGameCount++;
                    if (result == MatchEnum.ResultType.WIN) {
                        homeGameWinCount++;
                    }
                } else {
                    stadiumGameCount++;
                    if (result == MatchEnum.ResultType.WIN) {
                        stadiumGameWinCount++;
                    }
                }

                if (result == MatchEnum.ResultType.WIN) {
                    currentStreak++;
                    maxStreak = (short) Math.max(maxStreak, currentStreak);
                } else if (result == MatchEnum.ResultType.LOSS) {
                    currentStreak = 0; // 연승 끊김
                } else {
                    currentStreak = 0; // 연승 끊김
                }
            }
        }

        MyPageDomain.ViewTypeDto homeViewDto = null;
        var homeRecord = recordList.stream()
                .filter(record -> record.getViewType().equals(DiaryEnum.ViewType.HOME))
                .toList();

        if (!homeRecord.isEmpty()) {
            var winCount = (short) homeRecord.stream()
                    .filter(record -> record.getStatus().equals(MatchEnum.MatchStatus.END) && record.getResultType().equals(MatchEnum.ResultType.WIN))
                    .count();

            var loseCount = (short) homeRecord.stream()
                    .filter(record -> record.getStatus().equals(MatchEnum.MatchStatus.END) && record.getResultType().equals(MatchEnum.ResultType.LOSS))
                    .count();

            var drawCount = (short) homeRecord.stream()
                    .filter(record -> record.getStatus() != MatchEnum.MatchStatus.CANCELED && record.getResultType() == MatchEnum.ResultType.DRAW)
                    .count();

            var cancelCount = (short) homeRecord.stream()
                    .filter(record -> record.getStatus() == MatchEnum.MatchStatus.CANCELED)
                    .count();

            var validGameCount = winCount + loseCount;

            if (validGameCount > 0) {
                double avg = (double) winCount / validGameCount * 100;
                homeWinAvg = (short) Math.round(avg);  // 소수점 첫째자리 반올림
            }

            homeViewDto = new MyPageDomain.ViewTypeDto(
                    homeWinAvg,
                    winCount,
                    loseCount,
                    drawCount,
                    cancelCount
            );
        }

        // 최대 승리 팀
        var maxWinTeam = winMap.entrySet().stream()
                .max((e1, e2) -> {
                    int cmp = Integer.compare(e1.getValue().count(), e2.getValue().count());
                    if (cmp != 0) return cmp;
                    return e1.getValue().lastPlayedAt().compareTo(e2.getValue().lastPlayedAt());
                })
                .map(Map.Entry::getKey)
                .orElse("-");

        // 최대 패배 팀
        var maxLoseTeam = loseMap.entrySet().stream()
                .max((e1, e2) -> {
                    int cmp = Integer.compare(e1.getValue().count(), e2.getValue().count());
                    if (cmp != 0) return cmp;
                    return e1.getValue().lastPlayedAt().compareTo(e2.getValue().lastPlayedAt());
                })
                .map(Map.Entry::getKey)
                .orElse("-");

        // 최대 방문 구장
        // 방문 수가 같으면 마지막 방문일이 늦은 게 우선
        var maxVisitedStadium = stadiumVisitCount.entrySet().stream()
                .max(Comparator.comparingInt((Map.Entry<String, MyPageDomain.VisitInfoDto> e) ->
                        e.getValue().count()).thenComparing(e -> e.getValue().lastVisited()))
                .map(Map.Entry::getKey)
                .orElse(null);

        // 홈 승률
        short homeWinRate = (short) (homeGameCount == 0 ? 0 : Math.round(((double) homeGameWinCount / homeGameCount) * 100));
        // 직관 승률
        short stadiumWinRate = (short) (stadiumGameCount == 0 ? 0 : Math.round(((double) stadiumGameWinCount / stadiumGameCount) * 100));


        var visitStatisticsDto = new MyPageDomain.ViewStatisticsDto(
                maxWinTeam,
                maxLoseTeam,
                maxVisitedStadium,
                maxStreak,
                homeWinRate,
                stadiumWinRate
        );
        return new MyPageDomain.ReportResponse(stadiumViewDto, homeViewDto, visitStatisticsDto);
    }

    @Override
    @Transactional
    public void deleteMember(MyPageDomain.DeleteAccountRequest request) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var memberEntity = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var diaryEntities = diaryRepository.findByMemberId(memberEntity.getId());
        var recordEntities = gameRecordRepository.findByMemberId(id);

        var diaryIds = diaryEntities.stream()
                .map(entity -> entity.getId())
                .toList();

        var diaryFoodEntities = diaryFoodRepository.findAllByDiaryEntityIdIn(diaryIds);
        var partnerEntities = partnerRepository.findAllByDiaryEntityIdIn(diaryIds);
        var seatUserEntities = seatUseHistoryRepository.findAllByDiaryEntityIdIn(diaryIds);
        var seatReviewEntities = seatReviewRepository.findAllBySeatUseHistoryEntityIdIn(seatUserEntities.stream().map(entity -> entity.getId()).toList());

        // 회원 정보 삭제
        memberInfoRepository.delete(memberInfoEntity);
        // 직관 음식 삭제
        diaryFoodRepository.deleteAll(diaryFoodEntities);
        // 직관 파트너 삭제
        partnerRepository.deleteAll(partnerEntities);
        // 응원 기록 삭제
        gameRecordRepository.deleteAll(recordEntities);
        // 멤버 삭제
        memberRepository.delete(memberEntity);
        // 좌석 후기 삭제
        seatReviewRepository.deleteAll(seatReviewEntities);
        // 좌석 이용 내역 삭제
        seatUseHistoryRepository.deleteAll(seatUserEntities);
        // 일기 삭제
        diaryRepository.deleteAll(diaryEntities);

        var entity = WithdrawalReasonEntity.builder()
                .reason(request.reason())
                .build();
        withdrawalRepository.save(entity);
    }

    private Short getPower(List<GameRecordEntity> stadiumRecord, List<GameRecordEntity> homeRecord) {
        Short stadiumWinAvg = null;
        Short homeWinAvg = null;

        // 직관 기록이 있는 경우만 계산
        if (!stadiumRecord.isEmpty()) {
            var winCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.WIN)
                    .count();

            var loseCount = (short) stadiumRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.LOSS)
                    .count();

            var validGameCount = winCount + loseCount;

            if (validGameCount > 0) {
                double avg = (double) winCount / validGameCount * 100;
                stadiumWinAvg = (short) Math.round(avg);  // 소수점 첫째자리 반올림
            }
        }

        // 집관 기록이 있는 경우만 계산
        if (!homeRecord.isEmpty()) {
            var winCount = (short) homeRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.WIN)
                    .count();

            var loseCount = (short) homeRecord.stream()
                    .filter(record -> record.getResultType() == MatchEnum.ResultType.LOSS)
                    .count();

            var validGameCount = winCount + loseCount;

            if (validGameCount > 0) {
                double avg = (double) winCount / validGameCount * 100;
                homeWinAvg = (short) Math.round(avg);  // 소수점 첫째자리 반올림
            }
        }

        // 조건 분기 처리
        if (stadiumWinAvg != null && homeWinAvg != null) {
            return (short) Math.round((stadiumWinAvg + homeWinAvg) / 2.0);
        } else if (stadiumWinAvg != null) {
            return stadiumWinAvg;
        } else if (homeWinAvg != null) {
            return homeWinAvg;
        } else {
            return 0; // 직관/집관 모두 없음
        }
    }
}
