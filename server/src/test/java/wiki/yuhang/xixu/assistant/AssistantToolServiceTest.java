package wiki.yuhang.xixu.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import wiki.yuhang.xixu.journal.LogEntryRepository;
import wiki.yuhang.xixu.ledger.LedgerEntryRepository;
import wiki.yuhang.xixu.ledger.LedgerSummary;

class AssistantToolServiceTest {
    private final LogEntryRepository logs = mock(LogEntryRepository.class);
    private final LedgerEntryRepository ledger = mock(LedgerEntryRepository.class);
    private final AssistantToolService tools = new AssistantToolService(logs, ledger);

    @Test
    void snapshotCombinesOnlyCurrentUsersMonth() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        when(ledger.summary(9L, from, to))
            .thenReturn(new LedgerSummary(3, new BigDecimal("120.50"), BigDecimal.ZERO));
        when(logs.count(9L, from, to, "WORKOUT")).thenReturn(4L);

        MonthlySnapshot result = tools.monthlySnapshot(9L, "2026-11");

        assertEquals("2026-11", result.month());
        assertEquals(4L, result.workoutCount());
        assertEquals(3L, result.ledgerCount());
        assertEquals(new BigDecimal("120.50"), result.expense());
        verify(ledger).summary(9L, from, to);
        verify(logs).count(9L, from, to, "WORKOUT");
    }

    @Test
    void invalidMonthDoesNotQueryData() {
        assertThrows(ResponseStatusException.class, () -> tools.monthlySnapshot(9L, "2026-13"));
        verifyNoInteractions(logs, ledger);
    }
}
