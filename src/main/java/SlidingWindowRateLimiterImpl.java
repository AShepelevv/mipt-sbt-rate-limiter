import java.time.Duration;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.lang.System.nanoTime;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.LongStream.range;

public class SlidingWindowRateLimiterImpl implements RateLimiter {
    private final long maxExecutions;
    private final long intervalNanos;
    private final long timeoutNanos;
    private final ConcurrentSkipListSet<Long> runningLog = new ConcurrentSkipListSet<>();

    public SlidingWindowRateLimiterImpl(long maxExecutions, Duration slidingWindowInterval, Duration timeout) {
        this.maxExecutions = maxExecutions;
        this.intervalNanos = slidingWindowInterval.toNanos();
        this.timeoutNanos = timeout.toNanos();
        warmUp();
    }


    @Override
    public boolean runWithRateLimit(Runnable target) {
        long timestamp = microsecondsNow();
        if (canProceed(timestamp) && acquirePosition(timestamp)) { // TATAS semantics
            target.run();
            cleanUp();
            return true;
        }
        return false;
    }

    private synchronized boolean acquirePosition(long timestamp) {
        if (canProceed(timestamp)) {
            runningLog.add(timestamp);
            return true;
        }
        return false;
    }

    private boolean canProceed(long timestamp) {
        return timestamp >= microsecondsNow() - timeoutNanos &&
                hasFreePosition(timestamp);
    }

    private boolean hasFreePosition(long timestamp) { //Complexity: O(maxExecutions * log(maxExecutions * timeout / interval))
        return runningLog
                .tailSet(timestamp - intervalNanos)
                .headSet(timestamp + intervalNanos)
                .size() < maxExecutions;
    }

    private void cleanUp() {
        long timeNow = microsecondsNow();
        runningLog.removeIf(val -> val < timeNow - timeoutNanos);
    }

    private void warmUp() {
        long timestamp = microsecondsNow() - intervalNanos;
        runningLog.addAll(range(0, maxExecutions).mapToObj(i -> timestamp + i).collect(toUnmodifiableSet()));
    }

    private static long microsecondsNow() {
        return nanoTime();
    }
}
