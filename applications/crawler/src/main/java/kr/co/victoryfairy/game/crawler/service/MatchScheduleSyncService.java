package kr.co.victoryfairy.game.crawler.service;

import java.util.List;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchScheduleSyncService {

    private final GameMatchRepository gameMatchRepository;

    public MatchScheduleSyncService(GameMatchRepository gameMatchRepository) {
        this.gameMatchRepository = gameMatchRepository;
    }

    @Transactional
    public void sync(List<GameMatchEntity> officialMatches) {
        List<GameMatchEntity> matches = officialMatches.stream()
            .map(official -> gameMatchRepository.findById(official.getId()).map(existing -> {
                existing.syncSchedule(official);
                return existing;
            }).orElse(official))
            .toList();
        gameMatchRepository.saveAll(matches);
    }

}
