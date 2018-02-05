package org.facboy.caffeine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;

class CaffeineExpiryTest {

    @Test
    @SuppressWarnings({"unchecked", "RedundantStringConstructorCall", "InfiniteLoopStatement"})
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

        Assertions.assertTimeoutPreemptively(Duration.ofMinutes(1), () -> {
            int counter = 0;

            String expected = cache.get(10);
            while (true) {
                String val = cache.get(10);

                assertThat("Failed after " + counter + " iterations", val, sameInstance(expected));

                counter++;
            }
        });
    }
}
