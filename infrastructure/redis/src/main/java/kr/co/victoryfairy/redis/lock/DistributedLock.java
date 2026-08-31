package kr.co.victoryfairy.redis.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis 분산 락 어노테이션
 * <p>
 * 메서드에 선언하면 AOP를 통해 자동으로 분산 락 획득/해제를 처리합니다.
 * <p>
 * 사용 예시: <pre>
 * {@code
 * &#64;DistributedLock(value = LockName.DIARY_WRITE, key = "#userId")
 * public void writeDiary(Long userId, DiaryRequest request) {
 *     // 비즈니스 로직
 * }
 * }
 * </pre>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 락 이름 (Enum)
     */
    LockName value();

    /**
     * 락 키 (SpEL 표현식 지원)
     * <p>
     * 예시:
     * <ul>
     * <li>"#userId" - 파라미터명 직접 참조</li>
     * <li>"#request.diaryId" - 객체의 필드 참조</li>
     * <li>"#ids" - List 타입인 경우 MultiLock 적용</li>
     * </ul>
     */
    String key();

    /**
     * 락 획득 대기 시간 (밀리초)
     * <p>
     * 기본값: 5000ms (5초)
     */
    long waitTime() default 5000L;

    /**
     * 락 점유 시간 (밀리초)
     * <p>
     * 기본값: 5000ms (5초)
     * <p>
     * -1로 설정 시 Watchdog이 자동으로 락을 갱신합니다.
     */
    long leaseTime() default 5000L;

}