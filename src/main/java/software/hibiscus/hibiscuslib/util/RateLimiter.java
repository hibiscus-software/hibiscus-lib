/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player rate limiting for client requests, so a modified client can't flood the server with payloads (team
 * invites, quest claims). Each player gets a bucket of {@code burst} requests that refills at {@code perSecond}.
 * One limiter per kind of request; only touch it from the server thread.
 *
 * <pre>{@code
 * private static final RateLimiter INVITES = new RateLimiter(5, 1.0);
 * if (!INVITES.tryAcquire(player.getUUID())) return; // drop the request
 * }</pre>
 */
public final class RateLimiter {
    private record Bucket(double tokens, long lastNanos) {}

    private final int burst;
    private final double perSecond;
    private final Map<UUID, Bucket> buckets = new HashMap<>();

    public RateLimiter(int burst, double perSecond) {
        if (burst < 1 || perSecond <= 0) {
            throw new IllegalArgumentException("burst must be at least 1 and perSecond above 0");
        }
        this.burst = burst;
        this.perSecond = perSecond;
    }

    // Takes one request from the player's bucket: false if it's empty (drop the request).
    public boolean tryAcquire(UUID player) {
        return tryAcquire(player, System.nanoTime());
    }

    // The same, at a given System.nanoTime() (for tests).
    public boolean tryAcquire(UUID player, long nowNanos) {
        Bucket bucket = buckets.get(player);
        double tokens = bucket == null ? burst
                : Math.min(burst, bucket.tokens() + (nowNanos - bucket.lastNanos()) / 1e9 * perSecond);
        if (tokens < 1) {
            buckets.put(player, new Bucket(tokens, nowNanos));
            return false;
        }
        buckets.put(player, new Bucket(tokens - 1, nowNanos));
        return true;
    }

    // Forgets a player (on logout), so the map doesn't grow forever.
    public void forget(UUID player) {
        buckets.remove(player);
    }
}
