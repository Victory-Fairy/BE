package kr.co.victoryfairy.admin.domain;

import java.util.Optional;

public interface AdminStore {

    Optional<Admin> findByAdminId(String adminId);

    Admin save(Admin admin);
}
