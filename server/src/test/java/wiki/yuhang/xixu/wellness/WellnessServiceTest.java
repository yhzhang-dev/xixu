package wiki.yuhang.xixu.wellness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class WellnessServiceTest {
    private final WellnessRepository repository = mock(WellnessRepository.class);
    private final WellnessService service = new WellnessService(repository);
    private final LocalDate day = LocalDate.of(2026, 11, 1);

    @Test
    void bodyNeedsAnActualMeasurement() {
        assertThrows(ResponseStatusException.class, () -> service.createBody(1,
            new BodyMeasurementRequest(day, new BigDecimal("185"), null, null, null)));
        verify(repository, never()).insertBody(eq(1L), any());
    }

    @Test
    void sleepRejectsStageMinutesThatExceedTotal() {
        assertThrows(ResponseStatusException.class, () -> service.createSleep(1,
            new SleepSessionRequest(day, "NIGHT", 240, 120, 120, 30, null)));
        verify(repository, never()).insertSleep(eq(1L), any());
    }

    @Test
    void bodyWritesToTheSignedInUser() {
        BodyMeasurementRequest request = new BodyMeasurementRequest(day, null,
            new BigDecimal("67.35"), null, null);
        BodyMeasurement saved = new BodyMeasurement(8, day, null, request.weightKg(), null, null, null);
        when(repository.insertBody(42, request)).thenReturn(8L);
        when(repository.findBody(42, 8)).thenReturn(Optional.of(saved));
        assertEquals(saved, service.createBody(42, request));
    }

    @Test
    void trendRangeIsBounded() {
        assertThrows(ResponseStatusException.class, () -> service.body(1, day, day.minusDays(1)));
        assertThrows(ResponseStatusException.class, () -> service.sleep(1, day.minusDays(500), day));
    }
}
