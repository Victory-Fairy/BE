package kr.co.victoryfairy.game.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StadiumReader {

    Optional<Stadium> findById(Long id);

    List<Stadium> findAllById(Collection<Long> ids);

    List<Stadium> findAll();

    Optional<Stadium> findByExternalId(Integer externalId);

}
