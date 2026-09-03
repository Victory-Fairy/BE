CREATE INDEX idx_game_match_league_match_at
    ON game_match (league, match_at);

CREATE INDEX idx_diary_member_game_match
    ON diary (member_id, game_match_id);

CREATE INDEX idx_pitcher_record_game_match
    ON pitcher_record (game_match_id);

CREATE INDEX idx_hitter_record_game_match
    ON hitter_record (game_match_id);
