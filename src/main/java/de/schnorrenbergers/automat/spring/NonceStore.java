package de.schnorrenbergers.automat.spring;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class NonceStore {

    private final Cache<String, Boolean> cache =
            Caffeine.newBuilder()
                    .expireAfterWrite(60, TimeUnit.SECONDS)
                    .maximumSize(100_000)
                    .build();

    public boolean isReplay(String keyId, String nonce) {
        String key = keyId + ":" + nonce;
        if (cache.getIfPresent(key) != null) {
            return true;
        }
        cache.put(key, Boolean.TRUE);
        return false;
    }
}

