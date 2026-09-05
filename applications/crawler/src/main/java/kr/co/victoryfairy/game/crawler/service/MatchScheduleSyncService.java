package kr.co.victoryfairy.game.crawler.service;

import java.util.List;

import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchScheduleSyncService {

    private final GameMatchRepository gameMatchRepository;

    public MatchScheduleSyncService(GameMatchRepository gameMatchRepository) {
        this.gameMatchRepository = gameMatchRepository;
    }

    @Transactional
    public void sync(List<GameMatch> officialMatches) {
        gameMatchRepository.saveAll(officialMatches);
    }

}
