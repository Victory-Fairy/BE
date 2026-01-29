package kr.co.victoryfairy.logging.context;

public class LogContext {

    private final String logId;
    private final long startTimeMillis;
    private int methodDepth = 0;

    public LogContext(String logId) {
        this.logId = logId;
        this.startTimeMillis = System.currentTimeMillis();
    }

    public void increaseCall() {
        methodDepth++;
    }

    public void decreaseCall() {
        methodDepth--;
    }

    public String logId() {
        return logId;
    }

    public int depth() {
        return methodDepth;
    }

    public String depthPrefix(String prefixString) {
        if (methodDepth == 1) {
            return "|" + prefixString;
        }
        String bar = "|" + " ".repeat(prefixString.length());
        return bar.repeat(methodDepth - 1) + "|" + prefixString;
    }

    public long totalTakenTime() {
        return System.currentTimeMillis() - startTimeMillis;
    }
}
