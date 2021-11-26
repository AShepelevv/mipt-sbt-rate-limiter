import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.Math.ceil;
import static java.lang.System.nanoTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterImplTest {

    @Test
    void testSequential() throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiterImpl(5, 1000000, 10000000000000L);
        AtomicInteger counter = new AtomicInteger();

        IntStream.range(0, 6)
            .forEach(i -> rateLimiter.runWithRateLimit(counter::incrementAndGet));

        assertEquals(5, counter.get());
        Thread.sleep(1000);

        IntStream.range(0, 6)
                .forEach(i -> rateLimiter.runWithRateLimit(counter::incrementAndGet));
        assertEquals(10, counter.get());
    }

    @Test
    void testParallel() {
        long t1 = nanoTime();
        RateLimiter rateLimiter = new RateLimiterImpl(5, 1000000, 10000000000000L);
        AtomicInteger counter = new AtomicInteger();

        IntStream.range(0, 1000000000).parallel()
                .forEach(i -> rateLimiter.runWithRateLimit(counter::incrementAndGet));

        long t2 = nanoTime();
        assertTrue(2 * 5 * ceil((t2 - t1) / 1000 / 1000000.0) >  counter.get());
    }

}