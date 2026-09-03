package kr.co.victoryfairy.community.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.community.domain.CommunityReport;
import kr.co.victoryfairy.community.domain.CommunityReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class JdbcCommunityReportRepository implements CommunityReportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    @Override
    public Optional<Long> save(CommunityReport report) {
        var sql = """
            INSERT INTO %s
                (%s, reporter_id, reported_member_id, reason_code, status_code, detail, snapshot)
            VALUES
                (:targetId, :reporterId, :reportedMemberId, :reason, :status, :detail, :snapshot)
            """.formatted(reportTable(report.targetType()), targetColumn(report.targetType()));
        var parameters = new MapSqlParameterSource()
            .addValue("targetId", report.targetId())
            .addValue("reporterId", report.reporterId())
            .addValue("reportedMemberId", report.reportedMemberId())
            .addValue("reason", report.reason().name())
            .addValue("status", report.status().name())
            .addValue("detail", report.detail())
            .addValue("snapshot", writeSnapshot(report.snapshot()));
        var keyHolder = new GeneratedKeyHolder();

        try {
            jdbc.update(sql, parameters, keyHolder, new String[] { "id" });
            return Optional.of(keyHolder.getKey().longValue());
        }
        catch (DuplicateKeyException exception) {
            return Optional.empty();
        }
    }

    @Override
    public List<CommunityReport> find(
            CommunityReport.TargetType targetType, CommunityReport.Status status, Long cursor, int limit) {
        var statusCondition = status == null ? "" : " AND status_code = :status";
        var sql = """
            SELECT id, %s AS target_id, reporter_id, reported_member_id, reason_code, status_code,
                   detail, snapshot, created_at
            FROM %s
            WHERE (:cursor IS NULL OR id < :cursor)%s
            ORDER BY id DESC
            LIMIT :limit
            """.formatted(targetColumn(targetType), reportTable(targetType), statusCondition);
        var parameters = new MapSqlParameterSource()
            .addValue("cursor", cursor)
            .addValue("status", status == null ? null : status.name())
            .addValue("limit", limit);
        return jdbc.query(sql, parameters, (resultSet, rowNumber) -> map(targetType, resultSet));
    }

    @Override
    public Optional<CommunityReport> findById(CommunityReport.TargetType targetType, Long reportId) {
        var sql = """
            SELECT id, %s AS target_id, reporter_id, reported_member_id, reason_code, status_code,
                   detail, snapshot, created_at
            FROM %s
            WHERE id = :reportId
            """.formatted(targetColumn(targetType), reportTable(targetType));
        var parameters = new MapSqlParameterSource("reportId", reportId);
        return jdbc.query(sql, parameters, (resultSet, rowNumber) -> map(targetType, resultSet))
            .stream()
            .findFirst();
    }

    @Override
    public boolean resolve(
            CommunityReport.TargetType targetType, Long reportId, CommunityReport.Status status) {
        var sql = """
            UPDATE %s
            SET status_code = :status
            WHERE id = :reportId AND status_code = 'PENDING'
            """.formatted(reportTable(targetType));
        var parameters = new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("status", status.name());
        return jdbc.update(sql, parameters) == 1;
    }

    @Override
    public void softDeleteTarget(CommunityReport.TargetType targetType, Long targetId) {
        var sql = """
            UPDATE %s
            SET deleted_at = CURRENT_TIMESTAMP(6)
            WHERE id = :targetId AND deleted_at IS NULL
            """.formatted(targetTable(targetType));
        jdbc.update(sql, new MapSqlParameterSource("targetId", targetId));
    }

    private CommunityReport map(CommunityReport.TargetType targetType, ResultSet resultSet) throws SQLException {
        return new CommunityReport(
                resultSet.getLong("id"), targetType, resultSet.getLong("target_id"),
                resultSet.getLong("reporter_id"), resultSet.getLong("reported_member_id"),
                CommunityReport.Reason.valueOf(resultSet.getString("reason_code")),
                CommunityReport.Status.valueOf(resultSet.getString("status_code")),
                resultSet.getString("detail"), readSnapshot(resultSet.getString("snapshot")),
                resultSet.getTimestamp("created_at").toLocalDateTime());
    }

    private String writeSnapshot(CommunityReport.Snapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize community report snapshot", exception);
        }
    }

    private CommunityReport.Snapshot readSnapshot(String snapshot) {
        try {
            return objectMapper.readValue(snapshot, CommunityReport.Snapshot.class);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to deserialize community report snapshot", exception);
        }
    }

    private String reportTable(CommunityReport.TargetType targetType) {
        return switch (targetType) {
            case POST -> "community_post_report";
            case COMMENT -> "community_comment_report";
        };
    }

    private String targetColumn(CommunityReport.TargetType targetType) {
        return switch (targetType) {
            case POST -> "post_id";
            case COMMENT -> "comment_id";
        };
    }

    private String targetTable(CommunityReport.TargetType targetType) {
        return switch (targetType) {
            case POST -> "community_post";
            case COMMENT -> "community_comment";
        };
    }

}
