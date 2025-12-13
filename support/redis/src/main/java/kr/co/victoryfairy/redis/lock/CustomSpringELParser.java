package kr.co.victoryfairy.redis.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpEL 표현식 파서
 * <p>
 * 메서드 파라미터에서 동적으로 락 키를 추출합니다.
 */
public class CustomSpringELParser {

	private CustomSpringELParser() {
	}

	/**
	 * SpEL 표현식을 파싱하여 동적 값 추출
	 * @param parameterNames 메서드 파라미터 이름 배열
	 * @param args 메서드 인자 값 배열
	 * @param key SpEL 표현식 키
	 * @param lockName 락 이름 Enum
	 * @return 파싱된 락 키 목록 (단일 값이면 1개, List면 여러 개)
	 */
	public static List<String> getDynamicValue(String[] parameterNames, Object[] args, String key, LockName lockName) {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();

		for (int i = 0; i < parameterNames.length; i++) {
			context.setVariable(parameterNames[i], args[i]);
		}

		Object value = parser.parseExpression(key).getValue(context, Object.class);

		if (value == null) {
			return Collections.emptyList();
		}
		else if (value instanceof List<?> list) {
			List<String> stringList = new ArrayList<>(list.size());
			for (Object obj : list) {
				stringList.add(RedisLock.from(lockName, obj.toString()).getLockName());
			}
			return stringList;
		}
		else {
			return Collections.singletonList(RedisLock.from(lockName, value.toString()).getLockName());
		}
	}

}