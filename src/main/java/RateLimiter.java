public interface RateLimiter {
    boolean runWithRateLimit(Runnable target);
}
