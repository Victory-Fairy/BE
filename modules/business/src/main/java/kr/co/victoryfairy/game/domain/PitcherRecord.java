package kr.co.victoryfairy.game.domain;

public record PitcherRecord(Long id, Short turn, String name, String position, String inning, Short pitching,
        Short ballFour, Short strikeOut, Short hit, Short homeRun, Short score, String gameMatchId, String season,
        Boolean home) {
    public Short getTurn() {
        return turn;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public String getInning() {
        return inning;
    }

    public Short getPitching() {
        return pitching;
    }

    public Short getBallFour() {
        return ballFour;
    }

    public Short getStrikeOut() {
        return strikeOut;
    }

    public Short getHit() {
        return hit;
    }

    public Short getHomeRun() {
        return homeRun;
    }

    public Short getScore() {
        return score;
    }

    public Boolean getHome() {
        return home;
    }
}
