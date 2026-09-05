package kr.co.victoryfairy.diary.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.co.victoryfairy.game.domain.MatchEnum;

public final class ViewingStatistics {
    private ViewingStatistics() {}

    public record ViewType(Short winAvg, Short win, Short lose, Short draw, Short cancel) {}
    public record Summary(String winTeam, String lossTeam, String stadium, Short winningStreak,
            Short homeWinAvg, Short stadiumWinAvg) {}
    public record Report(ViewType stadium, ViewType home, Summary statistics) {}
    public record Power(Short power, Short level) {}
    private record Count(int count, LocalDateTime lastAt) {}

    public static Power power(List<ViewingRecordReader.Record> records) {
        var stadium = records.stream().filter(r -> r.viewType() == DiaryEnum.ViewType.STADIUM).toList();
        var home = records.stream().filter(r -> r.viewType() == DiaryEnum.ViewType.HOME).toList();
        Short stadiumAvg = powerAverage(stadium);
        Short homeAvg = powerAverage(home);
        short power = stadiumAvg != null && homeAvg != null ? (short) Math.round((stadiumAvg + homeAvg) / 2.0)
                : stadiumAvg != null ? stadiumAvg : homeAvg != null ? homeAvg : 0;
        return new Power(power, level(power));
    }

    public static short level(short power) {
        if (power >= 80) return 5;
        if (power >= 60) return 4;
        if (power >= 40) return 3;
        if (power >= 20) return 2;
        return power > 0 ? (short) 1 : 0;
    }

    private static Short powerAverage(List<ViewingRecordReader.Record> records) {
        if (records.isEmpty()) return null;
        short wins = count(records, MatchEnum.ResultType.WIN);
        short losses = count(records, MatchEnum.ResultType.LOSS);
        return wins + losses == 0 ? null : (short) Math.round((double) wins / (wins + losses) * 100);
    }

    public static ViewType stadiumResult(List<ViewingRecordReader.Record> records) {
        var stadium = records.stream().filter(r -> r.viewType() == DiaryEnum.ViewType.STADIUM).toList();
        short wins = count(stadium, MatchEnum.ResultType.WIN);
        short losses = count(stadium, MatchEnum.ResultType.LOSS);
        return new ViewType(rate(wins, (short) (wins + losses)), wins, losses,
                count(stadium, MatchEnum.ResultType.DRAW), count(stadium, MatchEnum.ResultType.CANCEL));
    }

    private static short count(List<ViewingRecordReader.Record> records, MatchEnum.ResultType result) {
        return (short) records.stream().filter(record -> record.result() == result).count();
    }

    public static Report report(List<GameRecord> records) {
        Map<String, Count> wins = new HashMap<>(), losses = new HashMap<>(), visits = new HashMap<>();
        short currentStreak = 0, maxStreak = 0, homeGames = 0, homeWins = 0, awayGames = 0, awayWins = 0;
        for (var record : records) {
            if (record.result() == MatchEnum.ResultType.WIN) {
                merge(wins, record.opponentTeamName(), record.matchAt());
            } else if (record.result() == MatchEnum.ResultType.LOSS) {
                merge(losses, record.opponentTeamName(), record.matchAt());
            }
            if (record.viewType() != DiaryEnum.ViewType.STADIUM) continue;
            merge(visits, record.stadiumName(), record.matchAt());
            if (record.homeTeamId().equals(record.teamId())) {
                homeGames++;
                if (record.result() == MatchEnum.ResultType.WIN) homeWins++;
            } else {
                awayGames++;
                if (record.result() == MatchEnum.ResultType.WIN) awayWins++;
            }
            if (record.result() == MatchEnum.ResultType.WIN) {
                currentStreak++;
                maxStreak = (short) Math.max(maxStreak, currentStreak);
            } else currentStreak = 0;
        }
        var stadiumRecords = records.stream().filter(r -> r.viewType() == DiaryEnum.ViewType.STADIUM).toList();
        var homeRecords = records.stream().filter(r -> r.viewType() == DiaryEnum.ViewType.HOME).toList();
        return new Report(viewType(stadiumRecords), viewType(homeRecords), new Summary(maxKey(wins, "-"),
                maxKey(losses, "-"), maxKey(visits, null), maxStreak, rate(homeWins, homeGames),
                rate(awayWins, awayGames)));
    }

    private static ViewType viewType(List<GameRecord> records) {
        if (records.isEmpty()) return null;
        short wins = (short) records.stream().filter(r -> r.status() == MatchEnum.MatchStatus.END && r.result() == MatchEnum.ResultType.WIN).count();
        short losses = (short) records.stream().filter(r -> r.status() == MatchEnum.MatchStatus.END && r.result() == MatchEnum.ResultType.LOSS).count();
        short draws = (short) records.stream().filter(r -> r.status() != MatchEnum.MatchStatus.CANCELED && r.result() == MatchEnum.ResultType.DRAW).count();
        short canceled = (short) records.stream().filter(r -> r.status() == MatchEnum.MatchStatus.CANCELED).count();
        return new ViewType(rate(wins, (short) (wins + losses)), wins, losses, draws, canceled);
    }

    private static short rate(short wins, short games) {
        return games == 0 ? 0 : (short) Math.round((double) wins / games * 100);
    }

    private static void merge(Map<String, Count> values, String key, LocalDateTime at) {
        values.merge(key, new Count(1, at), (old, next) -> new Count(old.count + 1,
                at.isAfter(old.lastAt) ? at : old.lastAt));
    }

    private static String maxKey(Map<String, Count> values, String fallback) {
        return values.entrySet().stream().max(Comparator.comparingInt((Map.Entry<String, Count> e) -> e.getValue().count)
                .thenComparing(e -> e.getValue().lastAt)).map(Map.Entry::getKey).orElse(fallback);
    }
}
