package wiki.yuhang.xixu.wellness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BodyMeasurement(long id, LocalDate observedOn, BigDecimal heightCm,
                              BigDecimal weightKg, BigDecimal bodyFatPct,
                              BigDecimal muscleMassKg, LocalDateTime createdAtUtc) {
}
