package wiki.yuhang.xixu.ledger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LedgerServiceTest {
    private final LedgerEntryRepository entries = mock(LedgerEntryRepository.class);
    private final LedgerService ledger = new LedgerService(entries);

    private LedgerEntry entry(long id, long userId, int status) {
        return new LedgerEntry(id, userId, "EXPENSE", new BigDecimal("12.50"),
            LocalDate.of(2026, 10, 31), null, "吃饭", "微信", null, "午饭", "手动",
            status, null, null, null, null);
    }

    @Test
    void createInsertsAndReturnsEntry() {
        LedgerEntryRequest request = new LedgerEntryRequest("EXPENSE", new BigDecimal("12.50"),
            LocalDate.of(2026, 10, 31), null, "吃饭", "微信", null, "午饭");
        when(entries.insert(eq(1L), eq("EXPENSE"), eq(new BigDecimal("12.50")),
            eq(LocalDate.of(2026, 10, 31)), isNull(), eq("吃饭"), eq("微信"),
            isNull(), eq("午饭"), eq("手动"))).thenReturn(9L);
        when(entries.findById(9L)).thenReturn(Optional.of(entry(9L, 1L, 1)));

        LedgerEntry created = ledger.create(1L, request);

        assertEquals(9L, created.id());
        assertEquals("EXPENSE", created.entryType());
        assertEquals(new BigDecimal("12.50"), created.amount());
    }

    @Test
    void getReturnsOnlyOwnEntry() {
        when(entries.findById(5L)).thenReturn(Optional.of(entry(5L, 1L, 1)));

        assertTrue(ledger.get(1L, 5L).isPresent());
        assertTrue(ledger.get(2L, 5L).isEmpty());
    }

    @Test
    void listDefaultsToFullDateRangeAndClampsPageSize() {
        when(entries.findPage(eq(1L), any(), any(), eq(100), eq(0), eq(false)))
            .thenReturn(List.of(entry(5L, 1L, 1)));
        when(entries.count(eq(1L), any(), any(), eq(false))).thenReturn(1L);

        LedgerPage page = ledger.list(1L, null, null, 1, 9999, false);

        assertEquals(1, page.items().size());
        assertEquals(1L, page.total());
        assertEquals(100, page.size());
    }

    @Test
    void summaryUsesFullDateRangeWithoutPagination() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        LedgerSummary expected = new LedgerSummary(137, new BigDecimal("580.50"), new BigDecimal("1200.00"));
        when(entries.summary(1L, from, to)).thenReturn(expected);

        assertEquals(expected, ledger.summary(1L, from, to));
        verify(entries).summary(1L, from, to);
        verify(entries, never()).findPage(anyLong(), any(), any(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void listCanIncludeVoidedEntriesForAudit() {
        when(entries.findPage(eq(1L), any(), any(), eq(20), eq(0), eq(true)))
            .thenReturn(List.of(entry(5L, 1L, 0)));
        when(entries.count(eq(1L), any(), any(), eq(true))).thenReturn(1L);

        LedgerPage page = ledger.list(1L, null, null, 1, 20, true);

        assertEquals(0, page.items().get(0).status());
        assertEquals(1L, page.total());
    }

    @Test
    void voidEntrySetsStatusZero() {
        when(entries.findById(5L))
            .thenReturn(Optional.of(entry(5L, 1L, 1)), Optional.of(entry(5L, 1L, 0)));
        when(entries.voidEntry(eq(5L), eq(1L), eq("重复"), any())).thenReturn(1);

        LedgerService.VoidResult result = ledger.voidEntry(1L, 5L, "重复");

        assertEquals(LedgerService.VoidOutcome.VOIDED, result.outcome());
        assertEquals(0, result.entry().status());
        verify(entries).voidEntry(eq(5L), eq(1L), eq("重复"), any());
    }

    @Test
    void voidEntryAlreadyVoidedReturnsConflict() {
        when(entries.findById(5L)).thenReturn(Optional.of(entry(5L, 1L, 0)));

        LedgerService.VoidResult result = ledger.voidEntry(1L, 5L, "重复");

        assertEquals(LedgerService.VoidOutcome.ALREADY_VOIDED, result.outcome());
        verify(entries, never()).voidEntry(anyLong(), anyLong(), any(), any());
    }

    @Test
    void voidEntryMissingReturnsNotFound() {
        when(entries.findById(5L)).thenReturn(Optional.empty());

        LedgerService.VoidResult result = ledger.voidEntry(1L, 5L, "重复");

        assertEquals(LedgerService.VoidOutcome.NOT_FOUND, result.outcome());
        verify(entries, never()).voidEntry(anyLong(), anyLong(), any(), any());
    }

    @Test
    void voidEntryDetectsConcurrentChange() {
        when(entries.findById(5L)).thenReturn(Optional.of(entry(5L, 1L, 1)));
        when(entries.voidEntry(eq(5L), eq(1L), eq("重复"), any())).thenReturn(0);

        assertEquals(LedgerService.VoidOutcome.ALREADY_VOIDED,
            ledger.voidEntry(1L, 5L, "重复").outcome());
    }

    @Test
    void reversedDateRangeIsRejected() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> ledger.list(1L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1), 1, 20, false));
        verifyNoInteractions(entries);
    }
}
