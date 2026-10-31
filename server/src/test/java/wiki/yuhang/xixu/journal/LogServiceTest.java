package wiki.yuhang.xixu.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LogServiceTest {
    private final LogEntryRepository entries = mock(LogEntryRepository.class);
    private final LogService logs = new LogService(entries);
    private final LocalDate day = LocalDate.of(2026, 11, 1);

    @Test
    void workoutNeedsAtLeastOneMeasuredSetBeforeWriting() {
        assertThrows(ResponseStatusException.class,
            () -> logs.create(1L, new LogEntryRequest("WORKOUT", day, "力量训练", null, List.of())));
        assertThrows(ResponseStatusException.class,
            () -> logs.create(1L, new LogEntryRequest("WORKOUT", day, "力量训练", null,
                List.of(new WorkoutSetRequest("卧推", null, null, null)))));
        verify(entries, never()).insert(eq(1L), any());
    }

    @Test
    void noteCannotContainWorkoutSets() {
        assertThrows(ResponseStatusException.class,
            () -> logs.create(1L, new LogEntryRequest("NOTE", day, "今天", null,
                List.of(new WorkoutSetRequest("卧推", 8, null, null)))));
        verify(entries, never()).insert(eq(1L), any());
    }

    @Test
    void workoutWritesSetsAndReturnsCreatedEntry() {
        WorkoutSetRequest set = new WorkoutSetRequest("卧推", 8, new BigDecimal("40.00"), null);
        LogEntryRequest request = new LogEntryRequest("WORKOUT", day, "胸部训练", "共四组", List.of(set));
        LogEntry saved = new LogEntry(7L, 1L, "WORKOUT", "训练", day, null,
            "胸部训练", "共四组", null, "MANUAL", null, null, null,
            List.of(new WorkoutSet(9L, 1, "卧推", 8, new BigDecimal("40.00"), null)));
        when(entries.insert(1L, request)).thenReturn(7L);
        when(entries.findById(1L, 7L)).thenReturn(Optional.of(saved));

        assertEquals(saved, logs.create(1L, request));
        verify(entries).insertSets(7L, List.of(set));
    }

    @Test
    void listRejectsReversedDatesAndUnknownTypes() {
        assertThrows(ResponseStatusException.class,
            () -> logs.list(1L, day, day.minusDays(1), null, 1, 20));
        assertThrows(ResponseStatusException.class,
            () -> logs.list(1L, null, null, "OTHER", 1, 20));
        assertTrue(logs.get(1L, 999L).isEmpty());
    }
}
