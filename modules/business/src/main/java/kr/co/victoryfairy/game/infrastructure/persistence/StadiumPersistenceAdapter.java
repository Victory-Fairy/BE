package kr.co.victoryfairy.game.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kr.co.victoryfairy.game.domain.Stadium;
import kr.co.victoryfairy.game.domain.StadiumReader;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.StadiumRepository;
import org.springframework.stereotype.Repository;

@Repository
public class StadiumPersistenceAdapter implements StadiumReader {

    private final StadiumRepository stadiums;

    public StadiumPersistenceAdapter(StadiumRepository stadiums) {
        this.stadiums = stadiums;
    }

    public Optional<Stadium> findById(Long id) {
        return stadiums.findById(id).map(GamePersistenceMapper::toDomain);
    }

    public List<Stadium> findAllById(Collection<Long> ids) {
        return stadiums.findAllById(ids).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public List<Stadium> findAll() {
        return stadiums.findAll().stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public Optional<Stadium> findByExternalId(Integer id) {
        return stadiums.findByExternalId(id).map(GamePersistenceMapper::toDomain);
    }

}
