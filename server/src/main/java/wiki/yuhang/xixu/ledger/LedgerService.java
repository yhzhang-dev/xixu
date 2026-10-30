package wiki.yuhang.xixu.ledger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LedgerService {
    private final LedgerEntryRepository entries;

    public LedgerService(LedgerEntryRepository entries) {
        this.entries = entries;
    }

    public LedgerEntry create(long userId, LedgerEntryRequest request) {
        long id = entries.insert(userId, request.entryType(), request.amount(), request.occurredOn(),
            request.occurredTime(), request.categoryName().trim(), optionalText(request.channelName()),
            optionalText(request.merchant()), optionalText(request.note()), "手动");
        return entries.findById(id).orElseThrow();
    }

    private String optionalText(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    public Optional<LedgerEntry> get(long userId, long id) {
        return entries.findById(id).filter(entry -> entry.userId() == userId);
    }

    public LedgerPage list(long userId, LocalDate from, LocalDate to, int page, int size,
                           boolean includeVoided) {
        validateRange(from, to);
        LocalDate safeFrom = from == null ? LocalDate.of(1000, 1, 1) : from;
        LocalDate safeTo = to == null ? LocalDate.of(9999, 12, 31) : to;
        int safePage = Math.min(Math.max(page, 1), 1_000_000);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<LedgerEntry> items = entries.findPage(userId, safeFrom, safeTo, safeSize, offset, includeVoided);
        long total = entries.count(userId, safeFrom, safeTo, includeVoided);
        return new LedgerPage(items, total, safePage, safeSize);
    }

    public LedgerSummary summary(long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return entries.summary(userId,
            from == null ? LocalDate.of(1000, 1, 1) : from,
            to == null ? LocalDate.of(9999, 12, 31) : to);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
    }

    public VoidResult voidEntry(long userId, long id, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写 1–200 字的作废原因");
        }
        Optional<LedgerEntry> found = entries.findById(id);
        if (found.isEmpty() || found.get().userId() != userId) {
            return new VoidResult(VoidOutcome.NOT_FOUND, null);
        }
        if (found.get().status() != 1) {
            return new VoidResult(VoidOutcome.ALREADY_VOIDED, found.get());
        }
        if (entries.voidEntry(id, userId, reason.trim(), LocalDateTime.now()) == 0) {
            return new VoidResult(VoidOutcome.ALREADY_VOIDED, null);
        }
        LedgerEntry updated = entries.findById(id).orElseThrow();
        return new VoidResult(VoidOutcome.VOIDED, updated);
    }

    public enum VoidOutcome { VOIDED, NOT_FOUND, ALREADY_VOIDED }

    public record VoidResult(VoidOutcome outcome, LedgerEntry entry) {
    }
}
