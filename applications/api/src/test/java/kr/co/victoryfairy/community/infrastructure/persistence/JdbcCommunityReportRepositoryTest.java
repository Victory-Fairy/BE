package kr.co.victoryfairy.community.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;

import kr.co.victoryfairy.community.domain.CommunityReport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;
import tools.jackson.databind.ObjectMapper;

class JdbcCommunityReportRepositoryTest {

    @Test
    void savesPostReportAndReturnsGeneratedId() throws Exception {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenReturn("{\"postId\":99,\"title\":\"제목\",\"content\":\"내용\"}");
        doAnswer(invocation -> {
            var keyHolder = invocation.getArgument(2, KeyHolder.class);
            keyHolder.getKeyList().add(Map.of("id", 55L));
            return 1;
        }).when(jdbc).update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class));
        var repository = new JdbcCommunityReportRepository(jdbc, mapper);

        var saved = repository.save(report());

        assertThat(saved).contains(55L);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class));
        assertThat(sql.getValue()).contains("community_post_report");
    }

    @Test
    void returnsEmptyWhenSameMemberReportsTargetAgain() throws Exception {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenReturn("{}");
        doThrow(new DuplicateKeyException("duplicate")).when(jdbc)
            .update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class));
        var repository = new JdbcCommunityReportRepository(jdbc, mapper);

        assertThat(repository.save(report())).isEmpty();
    }

    @Test
    void resolvesAndDeletesOnlySelectedTargetType() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        var repository = new JdbcCommunityReportRepository(jdbc, mock(ObjectMapper.class));

        assertThat(repository.resolve(
                CommunityReport.TargetType.COMMENT, 55L, CommunityReport.Status.ACCEPTED)).isTrue();
        repository.softDeleteTarget(CommunityReport.TargetType.COMMENT, 31L);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, org.mockito.Mockito.times(2)).update(sql.capture(), any(SqlParameterSource.class));
        assertThat(sql.getAllValues().get(0)).contains("community_comment_report");
        assertThat(sql.getAllValues().get(1)).contains("community_comment");
    }

    private CommunityReport report() {
        return new CommunityReport(null, CommunityReport.TargetType.POST, 99L, 7L, 8L,
                CommunityReport.Reason.SPAM, CommunityReport.Status.PENDING, "반복 게시",
                new CommunityReport.Snapshot(99L, "제목", "내용"), LocalDateTime.of(2026, 9, 3, 12, 2));
    }

}
