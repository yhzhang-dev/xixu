package wiki.yuhang.xixu.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AppUserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwords;

    public AuthService(AppUserRepository users, AuthSessionRepository sessions, PasswordEncoder passwords) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
    }

    public Optional<LoginResult> login(String username, String password) {
        Optional<AppUser> found = users.findByUsername(username);
        if (found.isEmpty() || found.get().status() != 1 ||
            !passwords.matches(password, found.get().passwordHash())) {
            return Optional.empty();
        }

        AppUser user = found.get();
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        LocalDateTime now = utcNow();
        LocalDateTime expiresAt = now.plusHours(24);
        sessions.create(user.id(), hashToken(token), now, expiresAt);
        users.recordLogin(user.id(), LocalDateTime.now());
        return Optional.of(new LoginResult(token, 86400, new AuthUser(user.id(), user.username(),
            user.displayName(), user.roleCode())));
    }

    public Optional<AuthUser> authenticate(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            return Optional.empty();
        }
        return sessions.findActiveUser(hashToken(token), utcNow());
    }

    public void logout(String token) {
        if (token != null && token.matches("[A-Za-z0-9_-]{43}")) {
            sessions.revoke(hashToken(token), utcNow());
        }
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.ofInstant(java.time.Instant.now(), ZoneOffset.UTC);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record LoginResult(String token, long expiresInSeconds, AuthUser user) {
    }
}
