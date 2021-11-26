import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import static java.lang.System.nanoTime;

public class RateLimiterImpl implements RateLimiter {
    private final long maxExecutions;
    private final long intervalMicroseconds;
    private final long timeoutMicroseconds;
    private final ConcurrentSkipListSet<Long> runningLog = new ConcurrentSkipListSet<>();

    public RateLimiterImpl(long maxExecutions, long intervalMicroseconds, long timeoutMicroseconds) {
        this.maxExecutions = maxExecutions;
        this.intervalMicroseconds = intervalMicroseconds;
        this.timeoutMicroseconds = timeoutMicroseconds;
    }


    @Override
    public boolean runWithRateLimit(Runnable target) {
        long timestamp = microsecondsNow();
        if (hasFreePosition(timestamp) && acquirePosition(timestamp)) {
            target.run();
            cleanUp();
            return true;
        }
        return false;
    }

    private synchronized boolean acquirePosition(long timestamp) {
        if (timestamp >= microsecondsNow() - timeoutMicroseconds &&
                hasFreePosition(timestamp)) {
            runningLog.add(timestamp);
            return true;
        }
        return false;
    }

    private boolean hasFreePosition(long timestamp) {
        return runningLog
                .tailSet(timestamp - intervalMicroseconds / 2)
                .headSet(timestamp + intervalMicroseconds / 2)
                .size() < maxExecutions;
    }

    private void cleanUp() {
        long timeNow = microsecondsNow();
        runningLog.removeIf(val -> val < timeNow - timeoutMicroseconds);
    }

    private long microsecondsNow() {
        return nanoTime() / 1000;
    }
}
