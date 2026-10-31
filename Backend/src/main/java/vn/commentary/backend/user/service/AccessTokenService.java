package vn.commentary.backend.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.commentary.backend.user.repository.AccessTokenRepository;
import vn.commentary.backend.user.util.TokenHasher;

import java.time.Clock;

/** Quản lý token trong mã QR cấp quyền (tương đương {@code qrToken} của server.mjs, nhưng lưu bền trong DB). */
@Service
public class AccessTokenService {
    private final AccessTokenRepository repository;
    private final Clock clock;

    public AccessTokenService(AccessTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Token hiện hành; tự tạo nếu chưa có. */
    @Transactional
    public String current() {
        return repository.findActive().orElseGet(this::create);
    }

    /** Vô hiệu token cũ và tạo token mới (các thiết bị đã vào vẫn giữ quyền theo phiên). */
    @Transactional
    public String rotate() {
        repository.deactivateAll();
        return create();
    }

    public boolean matches(String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        return repository.findActive().map(t -> TokenHasher.safeEquals(t, candidate.trim())).orElse(false);
    }

    private String create() {
        String token = TokenHasher.newQrToken();
        repository.insert(token, clock.millis());
        return token;
    }
}
