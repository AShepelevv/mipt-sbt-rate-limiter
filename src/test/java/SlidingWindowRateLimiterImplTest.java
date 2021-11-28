import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.Math.ceil;
import static java.lang.System.nanoTime;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowRateLimiterImplTest {

    @Test
    void testSequential() throws InterruptedException {
        RateLimiter rateLimiter = new SlidingWindowRateLimiterImpl(5, ofSeconds(1), ofSeconds(10));
        AtomicInteger counter = new AtomicInteger();

        range(0, 10)
            .forEach(i -> rateLimiter.runWithRateLimit(counter::incrementAndGet));
        assertEquals(5, counter.get());

        Thread.sleep(1000);

        range(0, 10)
                .forEach(i -> rateLimiter.runWithRateLimit(counter::incrementAndGet));
        assertEquals(10, counter.get());
    }

    @Test
    void testParallel() {
        long t1 = nanoTime();
        RateLimiter rateLimiter = new SlidingWindowRateLimiterImpl(5, ofSeconds(1), ofSeconds(10));
        AtomicInteger counter = new AtomicInteger();

        range(0, 1000000000).parallel()
                .forEach(i -> rateLimiter.runWithRateLimit(counter::incrementAndGet));

        int expectedCalls = (int)(5 * ceil((nanoTime() - t1) / 1000 / 1000000.0));
        assertThat(counter.get())
                .isLessThanOrEqualTo(expectedCalls)
                .isCloseTo(expectedCalls, withPercentage(10));
    }

    @Test
    void testTimeout() {
        RateLimiter rateLimiter = new SlidingWindowRateLimiterImpl(5, ofSeconds(1), Duration.ZERO);
        AtomicInteger counter = new AtomicInteger();

        range(0, 1000000000).parallel()
                .forEach(i -> rateLimiter.runWithRateLimit(counter::incrementAndGet));

        assertEquals(0, counter.get());
    }
}