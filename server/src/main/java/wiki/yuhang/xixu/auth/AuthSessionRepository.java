package wiki.yuhang.xixu.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthSessionRepository {
    private final JdbcTemplate jdbc;

    public AuthSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(long userId, String tokenHash, LocalDateTime now, LocalDateTime expiresAt) {
        jdbc.update("INSERT INTO auth_session (user_id, token_hash, created_at, expires_at) VALUES (?, ?, ?, ?)",
            userId, tokenHash, now, expiresAt);
    }

    public Optional<AuthUser> findActiveUser(String tokenHash, LocalDateTime now) {
        List<AuthUser> users = jdbc.query(
            "SELECT u.id, u.username, u.display_name, u.role_code " +
            "FROM auth_session s JOIN app_user u ON u.id = s.user_id " +
            "WHERE s.token_hash = ? AND s.revoked_at IS NULL AND s.expires_at > ? AND u.status = 1",
            (rs, rowNum) -> new AuthUser(rs.getLong("id"), rs.getString("username"),
                rs.getString("display_name"), rs.getString("role_code")), tokenHash, now);
        return users.stream().findFirst();
    }

    public void revoke(String tokenHash, LocalDateTime now) {
        jdbc.update("UPDATE auth_session SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL",
            now, tokenHash);
    }
}
