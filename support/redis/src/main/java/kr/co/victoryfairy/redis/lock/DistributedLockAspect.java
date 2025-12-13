package kr.co.victoryfairy.redis.lock;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 분산 락 AOP Aspect
 * <p>
 * {@link DistributedLock} 어노테이션이 붙은 메서드에 대해 자동으로 락 획득/해제를 처리합니다.
 * <p>
 * {@code @Order(Ordered.HIGHEST_PRECEDENCE)}로 설정하여 트랜잭션보다 먼저 실행됩니다.
 * 이를 통해 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제 순서가 보장됩니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DistributedLockAspect {

	private final RedissonClient redissonClient;

	private static final String LOCK_PREFIX = ":";

	@Around("@annotation(DistributedLock)")
	public Object distributedLock(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		DistributedLock annotation = method.getAnnotation(DistributedLock.class);

		// SpEL을 통해 동적 락 키 생성
		List<String> dynamicValue = CustomSpringELParser.getDynamicValue(signature.getParameterNames(),
				joinPoint.getArgs(), annotation.key(), annotation.value());

		if (dynamicValue.isEmpty()) {
			throw new IllegalStateException(
					String.format("분산락 키가 비어있습니다 - method: %s, key expression: %s", method.getName(), annotation.key()));
		}

		RLock lock;

		// 여러 키에 대한 락이 필요한 경우 MultiLock 사용
		if (dynamicValue.size() > 1) {
			List<String> lockKeys = dynamicValue.stream().map(value -> method.getName() + LOCK_PREFIX + value).toList();
			lock = redissonClient.getMultiLock(lockKeys.stream().map(redissonClient::getLock).toArray(RLock[]::new));
		}
		else {
			lock = redissonClient.getLock(dynamicValue.get(0));
		}

		try {
			// 락 획득 시도
			boolean isLocked = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), TimeUnit.MILLISECONDS);

			if (!isLocked) {
				String lockKey = dynamicValue.get(0);
				String methodName = method.getName();
				throw new IllegalStateException(
						String.format("분산락 획득 실패 - method: %s, key: %s", methodName, lockKey));
			}

			log.debug("분산락 획득 성공 - method: {}, key: {}", method.getName(), dynamicValue.get(0));
			return joinPoint.proceed();
		}
		catch (InterruptedException e) {
			log.error("분산락 획득 중 인터럽트 발생 - method: {}", method.getName(), e);
			Thread.currentThread().interrupt();
			throw e;
		}
		finally {
			// 현재 스레드가 락을 보유하고 있는 경우에만 해제
			if (lock != null && lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.debug("분산락 해제 - method: {}, key: {}", method.getName(), dynamicValue.get(0));
			}
		}
	}

}