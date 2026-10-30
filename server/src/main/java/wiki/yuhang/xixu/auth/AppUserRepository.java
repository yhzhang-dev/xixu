package wiki.yuhang.xixu.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppUserRepository {
    private final JdbcTemplate jdbc;

    public AppUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByUsername(String username) {
        List<AppUser> users = jdbc.query(
            "SELECT id, username, password_hash, display_name, role_code, status FROM app_user WHERE username = ?",
            (rs, rowNum) -> new AppUser(rs.getLong("id"), rs.getString("username"),
                rs.getString("password_hash"), rs.getString("display_name"),
                rs.getString("role_code"), rs.getInt("status")), username);
        return users.stream().findFirst();
    }

    public int countUsers() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class);
        return count == null ? 0 : count;
    }

    public void createAdmin(String username, String passwordHash) {
        jdbc.update("INSERT INTO app_user (username, password_hash, display_name, role_code) VALUES (?, ?, ?, 'ADMIN')",
            username, passwordHash, username);
    }

    public void recordLogin(long userId, LocalDateTime now) {
        jdbc.update("UPDATE app_user SET last_login_at = ? WHERE id = ?", now, userId);
    }
}
