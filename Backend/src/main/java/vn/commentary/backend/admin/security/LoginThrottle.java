package vn.commentary.backend.admin.security;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Chặn dò mật khẩu: tối đa {@code maxFailures} lần sai trong {@code windowSeconds} giây cho mỗi địa chỉ IP. */
public class LoginThrottle {
    private final int maxFailures;
    private final long windowSeconds;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public LoginThrottle(int maxFailures, long windowSeconds, Clock clock) {
        this.maxFailures = maxFailures;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    /** Số giây phải chờ nếu đang bị khoá, 0 nếu được phép thử. */
    public long retryAfterSeconds(String key) {
        Deque<Long> q = failures.get(key);
        if (q == null) return 0;
        synchronized (q) {
            long now = clock.instant().getEpochSecond();
            prune(q, now);
            if (q.size() < maxFailures) return 0;
            return Math.max(1, q.peekFirst() + windowSeconds - now);
        }
    }

    public void recordFailure(String key) {
        Deque<Long> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            long now = clock.instant().getEpochSecond();
            prune(q, now);
            q.addLast(now);
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Long> q, long now) {
        while (!q.isEmpty() && q.peekFirst() + windowSeconds <= now) q.pollFirst();
    }
}
