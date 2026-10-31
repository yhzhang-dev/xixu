package wiki.yuhang.xixu.auth;

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

    public void create(long userId, String tokenHash, String userAgent, String ipAddress) {
        jdbc.update("INSERT INTO auth_session (user_id, token_hash, created_at, expires_at, " +
                "user_agent, ip_address) VALUES (?, ?, UTC_TIMESTAMP(3), " +
                "UTC_TIMESTAMP(3) + INTERVAL 24 HOUR, ?, ?)",
            userId, tokenHash, userAgent, ipAddress);
    }

    public Optional<AuthUser> findActiveUser(String tokenHash) {
        List<AuthUser> users = jdbc.query(
            "SELECT u.id, u.username, u.display_name, u.role_code " +
            "FROM auth_session s JOIN app_user u ON u.id = s.user_id " +
            "WHERE s.token_hash = ? AND s.revoked_at IS NULL " +
            "AND s.expires_at > UTC_TIMESTAMP(3) AND u.status = 1",
            (rs, rowNum) -> new AuthUser(rs.getLong("id"), rs.getString("username"),
                rs.getString("display_name"), rs.getString("role_code")), tokenHash);
        if (!users.isEmpty()) {
            jdbc.update("UPDATE auth_session SET last_used_at = UTC_TIMESTAMP(3) " +
                    "WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > UTC_TIMESTAMP(3) " +
                    "AND (last_used_at IS NULL OR last_used_at < UTC_TIMESTAMP(3) - INTERVAL 5 MINUTE)",
                tokenHash);
        }
        return users.stream().findFirst();
    }

    public void revoke(String tokenHash) {
        jdbc.update("UPDATE auth_session SET revoked_at = UTC_TIMESTAMP(3) " +
            "WHERE token_hash = ? AND revoked_at IS NULL", tokenHash);
    }

    public void lockUser(long userId) {
        jdbc.queryForObject("SELECT id FROM app_user WHERE id = ? FOR UPDATE", Long.class, userId);
    }

    public void keepNewestActive(long userId, int maximum) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM auth_session WHERE user_id = ? " +
                "AND revoked_at IS NULL AND expires_at > UTC_TIMESTAMP(3) " +
                "ORDER BY created_at DESC, id DESC", Long.class, userId);
        for (int i = maximum; i < ids.size(); i++) {
            jdbc.update("UPDATE auth_session SET revoked_at = UTC_TIMESTAMP(3) " +
                "WHERE id = ? AND revoked_at IS NULL", ids.get(i));
        }
    }

    public void purgeOld(long userId) {
        jdbc.update("DELETE FROM auth_session WHERE user_id = ? AND (" +
            "revoked_at < UTC_TIMESTAMP(3) - INTERVAL 30 DAY " +
            "OR expires_at < UTC_TIMESTAMP(3) - INTERVAL 30 DAY)", userId);
    }
}
