package kr.co.victoryfairy.diary.infrastructure.persistence.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.QTeamEntity;
import kr.co.victoryfairy.diary.domain.DiaryModel;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryCustomRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QDiaryEntity.diaryEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QGameMatchEntity.gameMatchEntity;
import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QGameRecordEntity.gameRecordEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QStadiumEntity.stadiumEntity;

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
            .where(diaryEntity.memberId.eq(request.memberId())
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
            .where(diaryEntity.memberId.eq(request.memberId())
                .and(this.betweenMatchAt(request.startAt(), request.endExclusive())))
            .fetch();
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

}
