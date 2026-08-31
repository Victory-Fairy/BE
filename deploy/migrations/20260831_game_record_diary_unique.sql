-- This query must return no rows before applying the constraint.
SELECT diary_id, COUNT(*) AS record_count
FROM game_record
WHERE diary_id IS NOT NULL
GROUP BY diary_id
HAVING COUNT(*) > 1;

ALTER TABLE game_record
    ADD CONSTRAINT uk_game_record_diary_id UNIQUE (diary_id);
