package kr.co.victoryfairy.logging.aspect;

import kr.co.victoryfairy.logging.LayerLogger;
import kr.co.victoryfairy.logging.context.LogContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LogAop {

    private static final long SLOW_THRESHOLD_MS = 500;

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restControllerAnnotatedClass() {
    }

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceAnnotatedClass() {
    }

    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryAnnotatedClass() {
    }

    @Pointcut("within(kr.co.victoryfairy..*)")
    public void withinProject() {
    }

    @Around("(restControllerAnnotatedClass() || serviceAnnotatedClass() || repositoryAnnotatedClass()) && withinProject()")
    public Object logMethodCall(ProceedingJoinPoint pjp) throws Throwable {
        if (LogContextHolder.get() == null) {
            return pjp.proceed();
        }

        String className = getClassSimpleName(pjp);
        String methodName = getMethodName(pjp);
        long startTime = System.currentTimeMillis();

        LayerLogger.methodCall(className, methodName);
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed > SLOW_THRESHOLD_MS) {
                LayerLogger.methodReturnSlow(className, methodName, elapsed);
            } else {
                LayerLogger.methodReturn(className, methodName);
            }
            return result;
        } catch (Throwable e) {
            LayerLogger.throwException(className, methodName, e);
            throw e;
        }
    }

    private String getClassSimpleName(ProceedingJoinPoint pjp) {
        Class<?> clazz = pjp.getTarget().getClass();
        String className = clazz.getSimpleName();
        // CGLIB 프록시인 경우 원본 클래스명 추출
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }
        return className;
    }

    private String getMethodName(ProceedingJoinPoint pjp) {
        return pjp.getSignature().getName();
    }
}
