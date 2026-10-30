package wiki.yuhang.xixu.ledger;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class LedgerEntryRepository {
    private static final String SELECT_COLUMNS =
        "id, user_id, entry_type, amount, occurred_on, occurred_time, category_name, " +
        "channel_name, merchant, note, source_type, status, void_reason, voided_at, " +
        "created_at, updated_at";

    private final JdbcTemplate jdbc;

    public LedgerEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(long userId, String entryType, BigDecimal amount, LocalDate occurredOn,
                       LocalTime occurredTime, String categoryName, String channelName,
                       String merchant, String note, String sourceType) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ledger_entry (user_id, entry_type, amount, occurred_on, occurred_time, " +
                "category_name, channel_name, merchant, note, source_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, entryType);
            ps.setBigDecimal(3, amount);
            ps.setObject(4, occurredOn);
            ps.setObject(5, occurredTime);
            ps.setString(6, categoryName);
            ps.setString(7, channelName);
            ps.setString(8, merchant);
            ps.setString(9, note);
            ps.setString(10, sourceType);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    public Optional<LedgerEntry> findById(long id) {
        List<LedgerEntry> rows = jdbc.query(
            "SELECT " + SELECT_COLUMNS + " FROM ledger_entry WHERE id = ?", this::mapRow, id);
        return rows.stream().findFirst();
    }

    public List<LedgerEntry> findPage(long userId, LocalDate from, LocalDate to, int limit, int offset,
                                      boolean includeVoided) {
        String statusFilter = includeVoided ? "" : "AND status = 1 ";
        return jdbc.query(
            "SELECT " + SELECT_COLUMNS + " FROM ledger_entry " +
            "WHERE user_id = ? " + statusFilter + "AND occurred_on BETWEEN ? AND ? " +
            "ORDER BY occurred_on DESC, occurred_time DESC, id DESC LIMIT ? OFFSET ?",
            this::mapRow, userId, from, to, limit, offset);
    }

    public long count(long userId, LocalDate from, LocalDate to, boolean includeVoided) {
        String statusFilter = includeVoided ? "" : "AND status = 1 ";
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM ledger_entry WHERE user_id = ? " + statusFilter +
                "AND occurred_on BETWEEN ? AND ?",
            Long.class, userId, from, to);
        return count == null ? 0L : count;
    }

    public LedgerSummary summary(long userId, LocalDate from, LocalDate to) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) AS entry_count, " +
            "COALESCE(SUM(CASE WHEN entry_type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense, " +
            "COALESCE(SUM(CASE WHEN entry_type = 'INCOME' THEN amount ELSE 0 END), 0) AS income " +
            "FROM ledger_entry WHERE user_id = ? AND status = 1 AND occurred_on BETWEEN ? AND ?",
            (rs, rowNum) -> new LedgerSummary(rs.getLong("entry_count"),
                rs.getBigDecimal("expense"), rs.getBigDecimal("income")), userId, from, to);
    }

    public int voidEntry(long id, long userId, String reason, LocalDateTime voidedAt) {
        return jdbc.update(
            "UPDATE ledger_entry SET status = 0, void_reason = ?, voided_at = ? " +
            "WHERE id = ? AND user_id = ? AND status = 1",
            reason, voidedAt, id, userId);
    }

    private LedgerEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new LedgerEntry(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("entry_type"),
            rs.getBigDecimal("amount"),
            rs.getObject("occurred_on", LocalDate.class),
            rs.getObject("occurred_time", LocalTime.class),
            rs.getString("category_name"),
            rs.getString("channel_name"),
            rs.getString("merchant"),
            rs.getString("note"),
            rs.getString("source_type"),
            rs.getInt("status"),
            rs.getString("void_reason"),
            rs.getObject("voided_at", LocalDateTime.class),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class));
    }
}
