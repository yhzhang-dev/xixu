package wiki.yuhang.xixu.journal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LogEntry(long id, long userId, String entryType, String categoryName,
                       LocalDate occurredOn, String timeLabel, String title, String content,
                       String remark, String sourceType, String sourceRef,
                       LocalDateTime createdAt, LocalDateTime updatedAt,
                       List<WorkoutSet> sets) {
}
