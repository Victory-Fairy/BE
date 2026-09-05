package kr.co.victoryfairy.admin.domain;

import java.time.LocalDateTime;

public record Admin(Long id, String adminId, String password, String lastConnectIp, Boolean isUse,
        LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime lastConnectAt) {

    public Admin login(String ip, LocalDateTime connectedAt) {
        return new Admin(id, adminId, password, ip, isUse, createdAt, updatedAt, connectedAt);
    }
}
