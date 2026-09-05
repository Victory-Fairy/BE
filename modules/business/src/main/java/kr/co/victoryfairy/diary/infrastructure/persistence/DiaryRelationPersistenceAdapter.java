package kr.co.victoryfairy.diary.infrastructure.persistence;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.co.victoryfairy.diary.domain.DiaryFoodStore;
import kr.co.victoryfairy.diary.domain.PartnerStore;
import kr.co.victoryfairy.diary.domain.SeatUseStore;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryFoodEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.PartnerEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.SeatUseHistoryEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryFoodRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.PartnerRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatReviewRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.SeatRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import kr.co.victoryfairy.shared.domain.RefType;
import org.springframework.stereotype.Repository;

@Repository
public class DiaryRelationPersistenceAdapter implements DiaryFoodStore, PartnerStore, SeatUseStore {
    private final DiaryFoodRepository foods;
    private final PartnerRepository partners;
    private final SeatUseHistoryRepository seatUses;
    private final SeatReviewRepository reviews;
    private final DiaryRepository diaries;
    private final TeamRepository teams;
    private final SeatRepository seats;

    public DiaryRelationPersistenceAdapter(DiaryFoodRepository foods, PartnerRepository partners,
            SeatUseHistoryRepository seatUses, SeatReviewRepository reviews, DiaryRepository diaries,
            TeamRepository teams, SeatRepository seats) {
        this.foods = foods; this.partners = partners; this.seatUses = seatUses; this.reviews = reviews;
        this.diaries = diaries; this.teams = teams; this.seats = seats;
    }

    public void saveFoods(RefType type, Long refId, List<String> names) {
        if (names == null || names.isEmpty()) return;
        foods.saveAll(names.stream().map(name -> DiaryFoodEntity.builder().refType(type).refId(refId).foodName(name).build()).toList());
    }
    public void deleteFoods(RefType type, Long refId) { var rows = foods.findByRefTypeAndRefId(type, refId); if (!rows.isEmpty()) foods.deleteAll(rows); }
    public List<String> findNames(RefType type, Long refId) { return foods.findByRefTypeAndRefId(type, refId).stream().map(DiaryFoodEntity::getFoodName).toList(); }
    public Map<Long, List<String>> findNames(RefType type, List<Long> ids) { return ids == null || ids.isEmpty() ? Map.of() : foods.findByRefTypeAndRefIdIn(type, ids).stream().collect(groupingBy(DiaryFoodEntity::getRefId, mapping(DiaryFoodEntity::getFoodName, toList()))); }

    public void savePartners(RefType type, Long refId, List<Partner> values) {
        if (values == null || values.isEmpty()) return;
        var teamIds = values.stream().map(Partner::teamId).filter(Objects::nonNull).distinct().toList();
        var byId = teams.findAllById(teamIds).stream().collect(Collectors.toMap(TeamEntity::getId, value -> value));
        partners.saveAll(values.stream().map(value -> { var team = byId.get(value.teamId()); return PartnerEntity.builder().refType(type).refId(refId).name(value.name()).teamEntity(team).teamName(team == null ? null : team.getName()).build(); }).toList());
    }
    public List<Partner> find(RefType type, Long refId) { return partners.findByRefTypeAndRefId(type, refId).stream().map(row -> new Partner(row.getName(), row.getTeamEntity() == null ? null : row.getTeamEntity().getId())).toList(); }
    public void deletePartners(RefType type, Long refId) { var rows = partners.findByRefTypeAndRefId(type, refId); if (!rows.isEmpty()) partners.deleteAll(rows); }
    public Map<Long, List<String>> findNameMap(RefType type, List<Long> ids) { return ids == null || ids.isEmpty() ? Map.of() : partners.findByRefTypeAndRefIdIn(type, ids).stream().collect(groupingBy(PartnerEntity::getRefId, mapping(PartnerEntity::getName, toList()))); }

    public void save(Long diaryId, Long seatId, String name) { seatUses.save(SeatUseHistoryEntity.builder().diaryEntity(diaries.getReferenceById(diaryId)).seatEntity(seatId == null ? null : seats.findById(seatId).orElse(null)).seatName(name).build()); }
    public void replace(Long diaryId, Long seatId, String name) { delete(diaryId); save(diaryId, seatId, name); }
    public void delete(Long diaryId) { var row = seatUses.findByDiaryEntityId(diaryId); if (row == null) return; var existingReviews = reviews.findBySeatUseHistoryEntity(row); if (!existingReviews.isEmpty()) reviews.deleteAll(existingReviews); seatUses.delete(row); }
    public Optional<SeatUse> find(Long diaryId) { return Optional.ofNullable(seatUses.findByDiaryEntityId(diaryId)).map(row -> new SeatUse(row.getSeatEntity() == null ? null : row.getSeatEntity().getId(), row.getSeatName())); }
    public Map<Long, List<String>> findDescriptions(List<Long> diaryIds) { return seatUses.findAllByDiaryEntityIdIn(diaryIds).stream().filter(row -> row.getSeatEntity() != null).collect(groupingBy(row -> row.getDiaryEntity().getId(), mapping(row -> row.getSeatEntity().getName() + " " + row.getSeatName(), toList()))); }
}
