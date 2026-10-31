package wiki.yuhang.xixu.assistant;

import java.time.DateTimeException;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import wiki.yuhang.xixu.journal.LogEntryRepository;
import wiki.yuhang.xixu.ledger.LedgerEntryRepository;
import wiki.yuhang.xixu.ledger.LedgerSummary;

@Service
public class AssistantToolService {
    private final LogEntryRepository logs;
    private final LedgerEntryRepository ledger;

    public AssistantToolService(LogEntryRepository logs, LedgerEntryRepository ledger) {
        this.logs = logs;
        this.ledger = ledger;
    }

    @Transactional(readOnly = true)
    public MonthlySnapshot monthlySnapshot(long userId, String monthText) {
        YearMonth month;
        try {
            month = YearMonth.parse(monthText);
        } catch (DateTimeException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "月份格式应为 YYYY-MM");
        }
        if (month.getYear() < 1000 || month.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "月份超出支持范围");
        }
        LedgerSummary money = ledger.summary(userId, month.atDay(1), month.atEndOfMonth());
        long workouts = logs.count(userId, month.atDay(1), month.atEndOfMonth(), "WORKOUT");
        return new MonthlySnapshot(month.toString(), workouts, money.count(),
            money.expense(), money.income());
    }
}
