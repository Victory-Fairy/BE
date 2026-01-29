package kr.co.victoryfairy.logging.context;

/**
 * ThreadLocal 기반 LogContext 홀더
 */
public class LogContextHolder {

    private static final ThreadLocal<LogContext> HOLDER = new ThreadLocal<>();

    public static void set(LogContext logContext) {
        HOLDER.set(logContext);
    }

    public static LogContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static void increaseCall() {
        LogContext ctx = HOLDER.get();
        if (ctx != null) {
            ctx.increaseCall();
        }
    }

    public static void decreaseCall() {
        LogContext ctx = HOLDER.get();
        if (ctx != null) {
            ctx.decreaseCall();
        }
    }
}
