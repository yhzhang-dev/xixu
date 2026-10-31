package wiki.yuhang.xixu.journal;

import java.util.List;

public record LogPage(List<LogEntry> items, long total, int page, int size) {
}
