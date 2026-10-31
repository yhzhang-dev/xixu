package wiki.yuhang.xixu.journal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class LogEntryRepository {
    private static final String COLUMNS =
        "id, user_id, entry_type, category_name, occurred_on, time_label, title, content, " +
        "remark, source_type, source_ref, created_at, updated_at";
    private final JdbcTemplate jdbc;

    public LogEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(long userId, LogEntryRequest request) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO life_log_entry (user_id, entry_type, category_name, occurred_on, title, content, source_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'MANUAL')", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, request.entryType());
            ps.setString(3, "WORKOUT".equals(request.entryType()) ? "训练" : "随记");
            ps.setObject(4, request.occurredOn());
            ps.setString(5, request.title().trim());
            ps.setString(6, request.content() == null || request.content().isBlank()
                ? null : request.content().trim());
            return ps;
        }, keys);
        Number id = keys.getKey();
        if (id == null) throw new IllegalStateException("未取得日志 ID");
        return id.longValue();
    }

    public void insertSets(long entryId, List<WorkoutSetRequest> sets) {
        for (int index = 0; index < sets.size(); index++) {
            WorkoutSetRequest set = sets.get(index);
            jdbc.update("INSERT INTO workout_set (log_entry_id, sequence_no, exercise_name, " +
                    "repetitions, weight_kg, duration_seconds) VALUES (?, ?, ?, ?, ?, ?)",
                entryId, index + 1, set.exerciseName().trim(), set.repetitions(),
                set.weightKg(), set.durationSeconds());
        }
    }

    public Optional<LogEntry> findById(long userId, long id) {
        List<LogEntry> rows = jdbc.query("SELECT " + COLUMNS +
            " FROM life_log_entry WHERE user_id = ? AND id = ?", this::mapEntry, userId, id);
        return withSets(rows).stream().findFirst();
    }

    public List<LogEntry> findPage(long userId, LocalDate from, LocalDate to,
                                   String type, int limit, int offset) {
        String typeClause = type == null ? "" : " AND entry_type = ?";
        List<Object> args = new ArrayList<>(List.of(userId, from, to));
        if (type != null) args.add(type);
        args.add(limit);
        args.add(offset);
        List<LogEntry> rows = jdbc.query("SELECT " + COLUMNS +
                " FROM life_log_entry WHERE user_id = ? AND occurred_on BETWEEN ? AND ?" +
                typeClause + " ORDER BY occurred_on DESC, id DESC LIMIT ? OFFSET ?",
            this::mapEntry, args.toArray());
        return withSets(rows);
    }

    public long count(long userId, LocalDate from, LocalDate to, String type) {
        Long result = type == null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM life_log_entry WHERE user_id = ? " +
                "AND occurred_on BETWEEN ? AND ?", Long.class, userId, from, to)
            : jdbc.queryForObject("SELECT COUNT(*) FROM life_log_entry WHERE user_id = ? " +
                "AND occurred_on BETWEEN ? AND ? AND entry_type = ?", Long.class,
                userId, from, to, type);
        return result == null ? 0 : result;
    }

    private List<LogEntry> withSets(List<LogEntry> rows) {
        if (rows.isEmpty()) return rows;
        String placeholders = rows.stream().map(row -> "?").collect(Collectors.joining(","));
        Map<Long, List<WorkoutSet>> byEntry = new HashMap<>();
        jdbc.query("SELECT id, log_entry_id, sequence_no, exercise_name, repetitions, weight_kg, " +
                "duration_seconds FROM workout_set WHERE log_entry_id IN (" + placeholders +
                ") ORDER BY log_entry_id, sequence_no",
            rs -> {
                long entryId = rs.getLong("log_entry_id");
                byEntry.computeIfAbsent(entryId, ignored -> new ArrayList<>()).add(new WorkoutSet(
                    rs.getLong("id"), rs.getInt("sequence_no"), rs.getString("exercise_name"),
                    rs.getObject("repetitions", Integer.class), rs.getBigDecimal("weight_kg"),
                    rs.getObject("duration_seconds", Integer.class)));
            }, rows.stream().map(LogEntry::id).toArray());
        return rows.stream().map(row -> new LogEntry(row.id(), row.userId(), row.entryType(),
            row.categoryName(), row.occurredOn(), row.timeLabel(), row.title(), row.content(),
            row.remark(), row.sourceType(), row.sourceRef(), row.createdAt(), row.updatedAt(),
            byEntry.getOrDefault(row.id(), List.of()))).toList();
    }

    private LogEntry mapEntry(ResultSet rs, int rowNum) throws SQLException {
        return new LogEntry(rs.getLong("id"), rs.getLong("user_id"), rs.getString("entry_type"),
            rs.getString("category_name"), rs.getObject("occurred_on", LocalDate.class),
            rs.getString("time_label"), rs.getString("title"), rs.getString("content"),
            rs.getString("remark"), rs.getString("source_type"), rs.getString("source_ref"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class), List.of());
    }
}
