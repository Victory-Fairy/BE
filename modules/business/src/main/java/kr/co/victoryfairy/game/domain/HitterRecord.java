package kr.co.victoryfairy.game.domain;

public record HitterRecord(Long id, Short turn, String name, String position, Short hitCount, Short score, Short hit,
        Short homeRun, Short hitScore, Short ballFour, Short strikeOut, String gameMatchId, String season,
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

    public Short getHitCount() {
        return hitCount;
    }

    public Short getScore() {
        return score;
    }

    public Short getHit() {
        return hit;
    }

    public Short getHomeRun() {
        return homeRun;
    }

    public Short getHitScore() {
        return hitScore;
    }

    public Short getBallFour() {
        return ballFour;
    }

    public Short getStrikeOut() {
        return strikeOut;
    }

    public Boolean getHome() {
        return home;
    }
}
