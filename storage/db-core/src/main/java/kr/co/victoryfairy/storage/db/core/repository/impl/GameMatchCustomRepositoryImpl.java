package kr.co.victoryfairy.storage.db.core.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchCustomRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static kr.co.victoryfairy.storage.db.core.entity.QGameMatchEntity.gameMatchEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QStadiumEntity.stadiumEntity;

@Repository
public class GameMatchCustomRepositoryImpl extends QuerydslRepositorySupport implements GameMatchCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public GameMatchCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(GameMatchEntity.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<GameMatchEntity> findByMatchAt(LocalDate matchAt) {
        return jpaQueryFactory
                .select(Projections.fields(GameMatchEntity.class
                        , gameMatchEntity.id
                        , gameMatchEntity.type
                        , gameMatchEntity.series
                        , gameMatchEntity.season
                        , gameMatchEntity.matchAt
                        , gameMatchEntity.awayTeamEntity
                        , gameMatchEntity.awayNm
                        , gameMatchEntity.awayScore
                        , gameMatchEntity.homeTeamEntity
                        , gameMatchEntity.homeNm
                        , gameMatchEntity.homeScore
                        , stadiumEntity
                        , gameMatchEntity.status
                        , gameMatchEntity.reason
                        , gameMatchEntity.isMatchInfoCraw
                        , gameMatchEntity.league
                ))
                .from(gameMatchEntity)
                .leftJoin(stadiumEntity).on(gameMatchEntity.stadiumEntity.id.eq(stadiumEntity.id))
                .where(this.eqMatchAt(matchAt))
                .fetch();
    }

    @Override
    public List<GameMatchEntity> findByMatchAt(LocalDate matchAt, MatchEnum.LeagueType league) {
        return jpaQueryFactory
                .select(Projections.fields(GameMatchEntity.class
                        , gameMatchEntity.id
                        , gameMatchEntity.type
                        , gameMatchEntity.series
                        , gameMatchEntity.season
                        , gameMatchEntity.matchAt
                        , gameMatchEntity.awayTeamEntity
                        , gameMatchEntity.awayNm
                        , gameMatchEntity.awayScore
                        , gameMatchEntity.homeTeamEntity
                        , gameMatchEntity.homeNm
                        , gameMatchEntity.homeScore
                        , stadiumEntity
                        , gameMatchEntity.status
                        , gameMatchEntity.reason
                        , gameMatchEntity.isMatchInfoCraw
                        , gameMatchEntity.league
                ))
                .from(gameMatchEntity)
                .leftJoin(stadiumEntity).on(gameMatchEntity.stadiumEntity.id.eq(stadiumEntity.id))
                .where(this.eqMatchAt(matchAt), this.eqLeague(league))
                .fetch();
    }

    @Override
    public Optional<GameMatchEntity> findByTeamId(Long teamId, LocalDate matchAt) {
        return Optional.ofNullable(jpaQueryFactory
                .select(Projections.fields(GameMatchEntity.class
                        , gameMatchEntity.id
                        , gameMatchEntity.type
                        , gameMatchEntity.series
                        , gameMatchEntity.season
                        , gameMatchEntity.matchAt
                        , gameMatchEntity.awayTeamEntity
                        , gameMatchEntity.awayNm
                        , gameMatchEntity.awayScore
                        , gameMatchEntity.homeTeamEntity
                        , gameMatchEntity.homeNm
                        , gameMatchEntity.homeScore
                        , stadiumEntity
                        , gameMatchEntity.status
                        , gameMatchEntity.reason
                        , gameMatchEntity.isMatchInfoCraw
                        , gameMatchEntity.league
                ))
                .from(gameMatchEntity)
                .leftJoin(stadiumEntity).on(gameMatchEntity.stadiumEntity.id.eq(stadiumEntity.id))
                .where(this.eqTeamId(teamId).and(this.eqMatchAt(matchAt)))
                .fetchOne());
    }

    @Override
    public List<GameMatchEntity> findByTeamIdIn(Long teamId, LocalDate matchAt) {
        return jpaQueryFactory
                .select(Projections.fields(GameMatchEntity.class
                        , gameMatchEntity.id
                        , gameMatchEntity.type
                        , gameMatchEntity.series
                        , gameMatchEntity.season
                        , gameMatchEntity.matchAt
                        , gameMatchEntity.awayTeamEntity
                        , gameMatchEntity.awayNm
                        , gameMatchEntity.awayScore
                        , gameMatchEntity.homeTeamEntity
                        , gameMatchEntity.homeNm
                        , gameMatchEntity.homeScore
                        , stadiumEntity
                        , gameMatchEntity.status
                        , gameMatchEntity.reason
                        , gameMatchEntity.isMatchInfoCraw
                        , gameMatchEntity.league
                ))
                .from(gameMatchEntity)
                .leftJoin(stadiumEntity).on(gameMatchEntity.stadiumEntity.id.eq(stadiumEntity.id))
                .where(this.eqTeamId(teamId).and(this.eqMatchAt(matchAt)))
                .fetch();
    }

    @Override
    public List<GameMatchEntity> findByYearAndMonth(String year, String month) {
        return jpaQueryFactory
                .select(Projections.fields(GameMatchEntity.class
                        , gameMatchEntity.id
                        , gameMatchEntity.type
                        , gameMatchEntity.series
                        , gameMatchEntity.season
                        , gameMatchEntity.matchAt
                        , gameMatchEntity.awayTeamEntity
                        , gameMatchEntity.awayNm
                        , gameMatchEntity.awayScore
                        , gameMatchEntity.homeTeamEntity
                        , gameMatchEntity.homeNm
                        , gameMatchEntity.homeScore
                        , gameMatchEntity.status
                        , gameMatchEntity.reason
                        , gameMatchEntity.isMatchInfoCraw
                        , gameMatchEntity.league
                ))
                .from(gameMatchEntity)
                .where(this.eqMatchAt(year, month))
                .fetch();
    }

    private BooleanExpression eqMatchAt(LocalDate matchAt) {
        if (matchAt == null) {
            return null;
        }

        String matchAtStr = matchAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        StringTemplate dbDate = Expressions.stringTemplate(
                "DATE_FORMAT({0}, '%Y-%m-%d')", gameMatchEntity.matchAt);

        return dbDate.eq(matchAtStr);
    }

    private BooleanExpression eqTeamId(Long teamId) {
        return teamId == null ? null : gameMatchEntity.awayTeamEntity.id.eq(teamId).or(gameMatchEntity.homeTeamEntity.id.eq(teamId));
    }

    private BooleanExpression eqMatchAt(String year, String month) {
        if (year == null || month == null) { return null; }

        StringTemplate dbDate = Expressions.stringTemplate(
                "DATE_FORMAT({0}, '%Y-%m')", gameMatchEntity.matchAt);

        return dbDate.eq(year + "-" + month);
    }

    private BooleanExpression eqLeague(MatchEnum.LeagueType league) {
        return league != null ? gameMatchEntity.league.eq(league) : null;
    }
}
