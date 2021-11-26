import java.util.Set;

public interface RateLimiter {
    boolean runWithRateLimit(Runnable target);

    //Set<Long> getExecutionLog();
}
