package kr.co.victoryfairy.support.repository;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 Refresh Token 저장소
 * <p>
 * Refresh Token을 Redis에 저장하여 다음 기능을 지원합니다:
 * <ul>
 *   <li>강제 로그아웃 (토큰 즉시 무효화)</li>
 *   <li>다중 기기 로그인 관리</li>
 *   <li>토큰 탈취 시 즉시 차단</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
	private static final String ADMIN_REFRESH_TOKEN_PREFIX = "admin_refresh_token:";

	/**
	 * Refresh Token 저장
	 * @param memberId 회원 ID
	 * @param refreshToken Refresh Token 값
	 * @param expireDays 만료 일수
	 */
	public void save(Long memberId, String refreshToken, int expireDays) {
		String key = generateKey(memberId);
		redisTemplate.opsForValue().set(key, refreshToken, expireDays, TimeUnit.DAYS);
		log.debug("Refresh Token 저장 - memberId: {}, expireDays: {}", memberId, expireDays);
	}

	/**
	 * Refresh Token 조회
	 * @param memberId 회원 ID
	 * @return 저장된 Refresh Token (없으면 null)
	 */
	public String findByMemberId(Long memberId) {
		String key = generateKey(memberId);
		Object token = redisTemplate.opsForValue().get(key);
		return token != null ? token.toString() : null;
	}

	/**
	 * Refresh Token 유효성 검증
	 * @param memberId 회원 ID
	 * @param refreshToken 검증할 Refresh Token
	 * @return 유효 여부
	 */
	public boolean validate(Long memberId, String refreshToken) {
		String storedToken = findByMemberId(memberId);
		if (storedToken == null) {
			log.debug("Refresh Token 없음 - memberId: {}", memberId);
			return false;
		}
		boolean isValid = storedToken.equals(refreshToken);
		if (!isValid) {
			log.warn("Refresh Token 불일치 - memberId: {}", memberId);
		}
		return isValid;
	}

	/**
	 * Refresh Token 삭제 (로그아웃)
	 * @param memberId 회원 ID
	 */
	public void delete(Long memberId) {
		String key = generateKey(memberId);
		Boolean deleted = redisTemplate.delete(key);
		log.debug("Refresh Token 삭제 - memberId: {}, deleted: {}", memberId, deleted);
	}

	/**
	 * Refresh Token 존재 여부 확인
	 * @param memberId 회원 ID
	 * @return 존재 여부
	 */
	public boolean exists(Long memberId) {
		String key = generateKey(memberId);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	/**
	 * Refresh Token 갱신 (Rotation)
	 * @param memberId 회원 ID
	 * @param newRefreshToken 새로운 Refresh Token
	 * @param expireDays 만료 일수
	 */
	public void rotate(Long memberId, String newRefreshToken, int expireDays) {
		save(memberId, newRefreshToken, expireDays);
		log.debug("Refresh Token 갱신 - memberId: {}", memberId);
	}

	private String generateKey(Long memberId) {
		return REFRESH_TOKEN_PREFIX + memberId;
	}

	private String generateAdminKey(Long adminId) {
		return ADMIN_REFRESH_TOKEN_PREFIX + adminId;
	}

	// ==================== Admin용 메서드 ====================

	/**
	 * Admin Refresh Token 저장
	 */
	public void saveAdmin(Long adminId, String refreshToken, int expireDays) {
		String key = generateAdminKey(adminId);
		redisTemplate.opsForValue().set(key, refreshToken, expireDays, TimeUnit.DAYS);
		log.debug("Admin Refresh Token 저장 - adminId: {}, expireDays: {}", adminId, expireDays);
	}

	/**
	 * Admin Refresh Token 조회
	 */
	public String findByAdminId(Long adminId) {
		String key = generateAdminKey(adminId);
		Object token = redisTemplate.opsForValue().get(key);
		return token != null ? token.toString() : null;
	}

	/**
	 * Admin Refresh Token 유효성 검증
	 */
	public boolean validateAdmin(Long adminId, String refreshToken) {
		String storedToken = findByAdminId(adminId);
		if (storedToken == null) {
			log.debug("Admin Refresh Token 없음 - adminId: {}", adminId);
			return false;
		}
		boolean isValid = storedToken.equals(refreshToken);
		if (!isValid) {
			log.warn("Admin Refresh Token 불일치 - adminId: {}", adminId);
		}
		return isValid;
	}

	/**
	 * Admin Refresh Token 삭제 (로그아웃)
	 */
	public void deleteAdmin(Long adminId) {
		String key = generateAdminKey(adminId);
		Boolean deleted = redisTemplate.delete(key);
		log.debug("Admin Refresh Token 삭제 - adminId: {}, deleted: {}", adminId, deleted);
	}

	/**
	 * Admin Refresh Token 갱신 (Rotation)
	 */
	public void rotateAdmin(Long adminId, String newRefreshToken, int expireDays) {
		saveAdmin(adminId, newRefreshToken, expireDays);
		log.debug("Admin Refresh Token 갱신 - adminId: {}", adminId);
	}

}