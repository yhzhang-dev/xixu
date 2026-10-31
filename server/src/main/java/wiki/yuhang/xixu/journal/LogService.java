package wiki.yuhang.xixu.journal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LogService {
    private final LogEntryRepository entries;

    public LogService(LogEntryRepository entries) {
        this.entries = entries;
    }

    @Transactional
    public LogEntry create(long userId, LogEntryRequest request) {
        List<WorkoutSetRequest> sets = request.sets() == null ? List.of() : request.sets();
        if ("NOTE".equals(request.entryType()) && !sets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "普通日志不能包含训练组次");
        }
        if ("WORKOUT".equals(request.entryType()) && sets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "健身记录至少需要一组训练");
        }
        for (WorkoutSetRequest set : sets) {
            if (set == null || (set.repetitions() == null && set.durationSeconds() == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每组训练需填写次数或时长");
            }
        }
        long id = entries.insert(userId, request);
        if (!sets.isEmpty()) entries.insertSets(id, sets);
        return entries.findById(userId, id).orElseThrow();
    }

    public Optional<LogEntry> get(long userId, long id) {
        return entries.findById(userId, id);
    }

    public LogPage list(long userId, LocalDate from, LocalDate to, String type, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        if (type != null && !type.equals("NOTE") && !type.equals("WORKOUT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日志类型不正确");
        }
        LocalDate safeFrom = from == null ? LocalDate.of(1000, 1, 1) : from;
        LocalDate safeTo = to == null ? LocalDate.of(9999, 12, 31) : to;
        int safePage = Math.min(Math.max(page, 1), 1_000_000);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        return new LogPage(entries.findPage(userId, safeFrom, safeTo, type, safeSize, offset),
            entries.count(userId, safeFrom, safeTo, type), safePage, safeSize);
    }
}
