package kr.co.victoryfairy.admin.infrastructure.persistence;

import java.util.Optional;
import kr.co.victoryfairy.admin.domain.Admin;
import kr.co.victoryfairy.admin.domain.AdminStore;
import kr.co.victoryfairy.admin.infrastructure.persistence.entity.AdminEntity;
import kr.co.victoryfairy.admin.infrastructure.persistence.repository.AdminRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AdminPersistenceAdapter implements AdminStore {

    private final AdminRepository repository;

    public AdminPersistenceAdapter(AdminRepository repository) {
        this.repository = repository;
    }

    public Optional<Admin> findByAdminId(String adminId) {
        return repository.findByAdminId(adminId).map(AdminPersistenceAdapter::toDomain);
    }

    public Admin save(Admin admin) {
        var source = toEntity(admin);
        var entity = admin.id() == null ? source : repository.findById(admin.id()).map(existing -> {
            existing.updateFrom(source);
            return existing;
        }).orElse(source);
        return toDomain(repository.save(entity));
    }

    private static Admin toDomain(AdminEntity entity) {
        return new Admin(entity.getId(), entity.getAdminId(), entity.getPwd(), entity.getLastConnectIp(),
                entity.getIsUse(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getLastConnectAt());
    }

    private static AdminEntity toEntity(Admin admin) {
        return AdminEntity.builder().id(admin.id()).adminId(admin.adminId()).pwd(admin.password())
            .lastConnectIp(admin.lastConnectIp()).isUse(admin.isUse()).createdAt(admin.createdAt())
            .updatedAt(admin.updatedAt()).lastConnectAt(admin.lastConnectAt()).build();
    }
}
