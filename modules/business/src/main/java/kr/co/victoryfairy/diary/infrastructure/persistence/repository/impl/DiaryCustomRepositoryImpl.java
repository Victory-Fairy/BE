package kr.co.victoryfairy.diary.infrastructure.persistence.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.QTeamEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.model.DiaryModel;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryCustomRepository;
import kr.co.victoryfairy.shared.infrastructure.persistence.PageUtils;
import kr.co.victoryfairy.shared.infrastructure.persistence.model.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QDiaryEntity.diaryEntity;
import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QDiaryFoodEntity.diaryFoodEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QGameMatchEntity.gameMatchEntity;
import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QGameRecordEntity.gameRecordEntity;
import static kr.co.victoryfairy.member.infrastructure.persistence.entity.QMemberEntity.memberEntity;
import static kr.co.victoryfairy.member.infrastructure.persistence.entity.QMemberInfoEntity.memberInfoEntity;
import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QPartnerEntity.partnerEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QSeatEntity.seatEntity;
import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QSeatUseHistoryEntity.seatUseHistoryEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QStadiumEntity.stadiumEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QTeamEntity.teamEntity;

@Repository
public class DiaryCustomRepositoryImpl extends QuerydslRepositorySupport implements DiaryCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public DiaryCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(DiaryEntity.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request) {
        return jpaQueryFactory
            .select(Projections.fields(DiaryModel.DiaryDto.class, diaryEntity.id,
                    diaryEntity.teamEntity.id.as("teamId"), gameMatchEntity.matchAt, gameRecordEntity.resultType,
                    diaryEntity.createdAt, diaryEntity.updatedAt))
            .from(diaryEntity)
            .leftJoin(gameMatchEntity)
            .on(gameMatchEntity.id.eq(diaryEntity.gameMatchEntity.id))
            .leftJoin(gameRecordEntity)
            .on(gameRecordEntity.diaryEntity.id.eq(diaryEntity.id))
            .where(diaryEntity.member.id.eq(request.memberId())
                .and(this.betweenMatchAt(request.startDate(), request.endDate())))
            .fetch();
    }

    @Override
    public List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request) {
        var awayEntity = new QTeamEntity("awayTeamEntity");
        var homeEntity = new QTeamEntity("homeTeamEntity");

        return jpaQueryFactory
            .select(Projections.fields(DiaryModel.DiaryDto.class, diaryEntity.id,
                    diaryEntity.teamEntity.id.as("teamId"), diaryEntity.content, gameMatchEntity.id.as("gameMatchId"),
                    gameMatchEntity.matchAt, awayEntity.id.as("awayTeamId"), awayEntity.name.as("awayTeamName"),
                    gameMatchEntity.awayScore, homeEntity.id.as("homeTeamId"), homeEntity.name.as("homeTeamName"),
                    gameMatchEntity.homeScore, gameRecordEntity.resultType, gameMatchEntity.status,
                    gameMatchEntity.reason, stadiumEntity.shortName, stadiumEntity.fullName, diaryEntity.createdAt,
                    diaryEntity.updatedAt))
            .from(diaryEntity)
            .leftJoin(gameMatchEntity)
            .on(gameMatchEntity.id.eq(diaryEntity.gameMatchEntity.id))
            .leftJoin(awayEntity)
            .on(awayEntity.id.eq(gameMatchEntity.awayTeamEntity.id))
            .leftJoin(homeEntity)
            .on(homeEntity.id.eq(gameMatchEntity.homeTeamEntity.id))
            .leftJoin(stadiumEntity)
            .on(gameMatchEntity.stadiumEntity.id.eq(stadiumEntity.id))
            .leftJoin(gameRecordEntity)
            .on(gameRecordEntity.diaryEntity.id.eq(diaryEntity.id))
            .where(diaryEntity.member.id.eq(request.memberId())
                .and(this.betweenMatchAt(request.startAt(), request.endExclusive())))
            .fetch();
    }

    @Override
    public PageResult<DiaryModel.DiaryListResponse> findAll(DiaryModel.DiaryListRequest request) {
        var pageRequest = PageRequest.of(request.page() - 1, request.size());

        var query = jpaQueryFactory
            .select(Projections.fields(DiaryModel.DiaryListResponse.class, diaryEntity.id, teamEntity.id.as("teamId"),
                    teamEntity.name.as("teamName"), diaryEntity.content, memberEntity.id.as("memberId"),
                    memberInfoEntity.nickNm, gameMatchEntity.matchAt, gameMatchEntity.status, diaryEntity.moodType,
                    diaryEntity.viewType, diaryEntity.weatherType))
            .from(diaryEntity)
            .innerJoin(memberEntity)
            .on(diaryEntity.member.id.eq(memberEntity.id))
            .innerJoin(memberInfoEntity)
            .on(memberEntity.id.eq(memberInfoEntity.memberEntity.id))
            .leftJoin(teamEntity)
            .on(diaryEntity.teamEntity.id.eq(teamEntity.id))
            .leftJoin(gameMatchEntity)
            .on(gameMatchEntity.id.eq(diaryEntity.gameMatchEntity.id))
            // .leftJoin(diaryFoodEntity).on(diaryEntity.id.eq(diaryFoodEntity.diaryEntity.id))
            // .leftJoin(partnerEntity).on(diaryEntity.id.eq(partnerEntity.diaryEntity.id))
            // .leftJoin(seatUseHistoryEntity).on(diaryEntity.id.eq(seatUseHistoryEntity.diaryEntity.id))
            // .leftJoin(seatEntity).on(seatUseHistoryEntity.seatEntity.id.eq(seatEntity.id))
            .orderBy(diaryEntity.id.desc())
            .where(this.eqMatchAt(request.date()), this.eqStatus(request.status()));

        return PageUtils.getPageResult(query, pageRequest);
    }

    private BooleanExpression betweenMatchAt(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return gameMatchEntity.matchAt.between(start, end);
    }

    private BooleanExpression betweenMatchAt(LocalDateTime startAt, LocalDateTime endExclusive) {
        if (startAt == null || endExclusive == null) {
            return null;
        }

        return gameMatchEntity.matchAt.goe(startAt).and(gameMatchEntity.matchAt.lt(endExclusive));
    }

    private BooleanExpression eqMatchAt(LocalDate matchAt) {
        return matchAt == null ? null
                : betweenMatchAt(matchAt.atStartOfDay(), matchAt.plusDays(1).atStartOfDay());
    }

    private BooleanExpression eqStatus(MatchEnum.MatchStatus status) {
        return status != null ? gameMatchEntity.status.eq(status) : null;
    }

    private BooleanExpression eqResultType(MatchEnum.MatchType type) {
        return null;
    }

}
