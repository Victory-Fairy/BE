package kr.co.victoryfairy.logging;

import kr.co.victoryfairy.logging.context.LogContext;
import kr.co.victoryfairy.logging.context.LogContextHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LayerLogger {

    private static final String CALL_PREFIX = "--->";
    private static final String RETURN_PREFIX = "<---";
    private static final String EX_PREFIX = "<X--";

    public static void methodCall(String className, String methodName) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) {
            return;
        }
        LogContextHolder.increaseCall();
        log.info("{}",
                formattedClassAndMethod(logContext.depthPrefix(CALL_PREFIX), className, methodName)
        );
    }

    public static void methodReturn(String className, String methodName) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) {
            return;
        }
        log.info("{} time={}ms",
                formattedClassAndMethod(logContext.depthPrefix(RETURN_PREFIX), className, methodName),
                logContext.totalTakenTime()
        );
        LogContextHolder.decreaseCall();
    }

    public static void methodReturnSlow(String className, String methodName, long timeMs) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) {
            return;
        }
        log.warn("{} time={}ms [SLOW]",
                formattedClassAndMethod(logContext.depthPrefix(RETURN_PREFIX), className, methodName),
                timeMs
        );
        LogContextHolder.decreaseCall();
    }

    public static void throwException(String className, String methodName, Throwable exception) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) {
            return;
        }

        String logPrefix = formattedClassAndMethod(logContext.depthPrefix(EX_PREFIX), className, methodName);
        long time = logContext.totalTakenTime();
        StackTraceElement[] stackTrace = exception.getStackTrace();

        if (stackTrace.length > 0) {
            log.warn("{} time={}ms, throws {} at {}:{}",
                    logPrefix,
                    time,
                    exception.getClass().getSimpleName(),
                    stackTrace[0].getFileName(),
                    stackTrace[0].getLineNumber()
            );
        } else {
            log.warn("{} time={}ms, throws {}", logPrefix, time, exception.getClass().getSimpleName());
        }
        LogContextHolder.decreaseCall();
    }

    private static String formattedClassAndMethod(String prefix, String className, String methodName) {
        return String.format("%-80s", prefix + className + "." + methodName + "()");
    }
}
