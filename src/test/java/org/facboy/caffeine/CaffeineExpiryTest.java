package org.facboy.caffeine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CaffeineExpiryTest {

    @Test
    @Disabled
    void maxExpiryShouldNotExpireImmediately() {
        LoadingCache<Integer, String> cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<Integer, String>() {
                    public long expireAfterCreate(Integer key, String value, long currentTime) {
                        return Long.MAX_VALUE;
                    }

                    public long expireAfterUpdate(Integer key, String value, long currentTime, long currentDuration) {
                        return Long.MAX_VALUE;
                    }

                    public long expireAfterRead(Integer key, String value, long currentTime, long currentDuration) {
                        return Long.MAX_VALUE;
                    }
                })
                .build(key -> new String("10"));

        final long end = System.currentTimeMillis() + Duration.ofSeconds(5).toMillis();
        int counter = 0;

        String expected = cache.get(10);
        while (System.currentTimeMillis() < end) {
            String val = cache.get(10);

            assertThat("Failed after " + counter + " iterations", val, sameInstance(expected));

            counter++;
        }
    }

    @SuppressWarnings({"BusyWait", "unchecked"})
    @Test
    void variableExpiryShouldExpireAfterUpdate() throws InterruptedException {
        final RemovalListener<Integer, String> removalListener = mock(RemovalListener.class);

        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        try {
            Cache<Integer, String> cache = Caffeine.newBuilder()
                    .expireAfter(new Expiry<Integer, String>() {
                        public long expireAfterCreate(Integer key, String value, long currentTime) {
                            return Long.MAX_VALUE;
                        }

                        public long expireAfterUpdate(Integer key, String value, long currentTime, long currentDuration) {
                            return Duration.ofSeconds(1).toNanos();
                        }

                        public long expireAfterRead(Integer key, String value, long currentTime, long currentDuration) {
                            return Long.MAX_VALUE;
                        }
                    })
                    .scheduler(Scheduler.forScheduledExecutorService(scheduledExecutorService))
                    .removalListener(removalListener)
                    .build();

            final long end = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();

            while (System.currentTimeMillis() < end) {
                cache.put(10, "10");
                Thread.sleep(500);

                String removed = new String("11");
                cache.put(10, removed);

                Thread.sleep(2000);

                verify(removalListener).onRemoval(eq(10), eq("10"), eq(RemovalCause.REPLACED));
                verify(removalListener).onRemoval(eq(10), same(removed), eq(RemovalCause.EXPIRED));
            }
        } finally {
            scheduledExecutorService.shutdownNow();
        }
    }
}
