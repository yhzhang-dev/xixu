package wiki.yuhang.xixu.wellness;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class WellnessRepository {
    private static final String BODY_COLUMNS =
        "id, observed_on, height_cm, weight_kg, body_fat_pct, muscle_mass_kg, created_at_utc";
    private static final String SLEEP_COLUMNS =
        "id, sleep_on, session_kind, duration_minutes, deep_minutes, light_minutes, " +
        "rem_minutes, awake_minutes, created_at_utc";
    private final JdbcTemplate jdbc;

    public WellnessRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertBody(long userId, BodyMeasurementRequest request) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO health_body_measurement (user_id, observed_on, height_cm, weight_kg, " +
                    "body_fat_pct, muscle_mass_kg, source_type, created_at_utc) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'MANUAL', ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setObject(2, request.observedOn());
            ps.setObject(3, request.heightCm());
            ps.setObject(4, request.weightKg());
            ps.setObject(5, request.bodyFatPct());
            ps.setObject(6, request.muscleMassKg());
            ps.setObject(7, nowUtc);
            return ps;
        }, keys);
        return key(keys);
    }

    public Optional<BodyMeasurement> findBody(long userId, long id) {
        return jdbc.query("SELECT " + BODY_COLUMNS + " FROM health_body_measurement " +
            "WHERE user_id = ? AND id = ?", this::mapBody, userId, id).stream().findFirst();
    }

    public List<BodyMeasurement> listBody(long userId, LocalDate from, LocalDate to) {
        return jdbc.query("SELECT " + BODY_COLUMNS + " FROM health_body_measurement " +
            "WHERE user_id = ? AND observed_on BETWEEN ? AND ? " +
            "ORDER BY observed_on DESC, id DESC LIMIT 500", this::mapBody, userId, from, to);
    }

    public long insertSleep(long userId, SleepSessionRequest request) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO health_sleep_session (user_id, sleep_on, session_kind, duration_minutes, " +
                    "deep_minutes, light_minutes, rem_minutes, awake_minutes, source_type, created_at_utc) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'MANUAL', ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setObject(2, request.sleepOn());
            ps.setString(3, request.sessionKind());
            ps.setObject(4, request.durationMinutes());
            ps.setObject(5, request.deepMinutes());
            ps.setObject(6, request.lightMinutes());
            ps.setObject(7, request.remMinutes());
            ps.setObject(8, request.awakeMinutes());
            ps.setObject(9, nowUtc);
            return ps;
        }, keys);
        return key(keys);
    }

    public Optional<SleepSession> findSleep(long userId, long id) {
        return jdbc.query("SELECT " + SLEEP_COLUMNS + " FROM health_sleep_session " +
            "WHERE user_id = ? AND id = ?", this::mapSleep, userId, id).stream().findFirst();
    }

    public List<SleepSession> listSleep(long userId, LocalDate from, LocalDate to) {
        return jdbc.query("SELECT " + SLEEP_COLUMNS + " FROM health_sleep_session " +
            "WHERE user_id = ? AND sleep_on BETWEEN ? AND ? " +
            "ORDER BY sleep_on DESC, id DESC LIMIT 500", this::mapSleep, userId, from, to);
    }

    private long key(KeyHolder keys) {
        Number value = keys.getKey();
        if (value == null) throw new IllegalStateException("未取得健康记录 ID");
        return value.longValue();
    }

    private BodyMeasurement mapBody(ResultSet rs, int rowNum) throws SQLException {
        return new BodyMeasurement(rs.getLong("id"), rs.getObject("observed_on", LocalDate.class),
            rs.getBigDecimal("height_cm"), rs.getBigDecimal("weight_kg"),
            rs.getBigDecimal("body_fat_pct"), rs.getBigDecimal("muscle_mass_kg"),
            rs.getObject("created_at_utc", LocalDateTime.class));
    }

    private SleepSession mapSleep(ResultSet rs, int rowNum) throws SQLException {
        return new SleepSession(rs.getLong("id"), rs.getObject("sleep_on", LocalDate.class),
            rs.getString("session_kind"), rs.getObject("duration_minutes", Integer.class),
            rs.getObject("deep_minutes", Integer.class), rs.getObject("light_minutes", Integer.class),
            rs.getObject("rem_minutes", Integer.class), rs.getObject("awake_minutes", Integer.class),
            rs.getObject("created_at_utc", LocalDateTime.class));
    }
}
