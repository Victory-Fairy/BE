package kr.co.victoryfairy.redis.lock;

import java.text.MessageFormat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 분산 락 이름 정의
 * <p>
 * 새로운 락이 필요한 경우 여기에 추가
 */
@Getter
@RequiredArgsConstructor
public enum LockName {

    MEMBER_REGISTER("MEMBER_REGISTER_{0}", "회원가입 시 SNS 타입_SNS ID 기준으로 lock 처리"),
    DIARY_WRITE("DIARY_WRITE_{0}", "일기 작성 시 사용자 PK 기준으로 lock 처리"),
    DIARY_UPDATE("DIARY_UPDATE_{0}", "일기 수정 시 일기 PK 기준으로 lock 처리"),
    DIARY_DELETE("DIARY_DELETE_{0}", "일기 삭제 시 일기 PK 기준으로 lock 처리"),
    USER_UPDATE("USER_UPDATE_{0}", "회원 정보 수정 시 사용자 PK 기준으로 lock 처리"),;

    private final String lockPattern;

    private final String description;

    /**
     * 락 키에 실제 값을 바인딩하여 최종 락 이름 생성
     * @param key 바인딩할 키 값
     * @return 최종 락 이름 (예: DIARY_WRITE_123)
     */
    public String createLockName(String key) {
        return MessageFormat.format(this.lockPattern, key);
    }

}