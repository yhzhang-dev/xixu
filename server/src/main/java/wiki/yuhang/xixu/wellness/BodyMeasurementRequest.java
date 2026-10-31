package wiki.yuhang.xixu.wellness;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BodyMeasurementRequest(
    @NotNull(message = "请选择测量日期") LocalDate observedOn,
    @DecimalMin(value = "0.01", message = "身高必须大于 0")
    @Digits(integer = 3, fraction = 2) BigDecimal heightCm,
    @DecimalMin(value = "0.01", message = "体重必须大于 0")
    @Digits(integer = 4, fraction = 2) BigDecimal weightKg,
    @DecimalMin(value = "0.00", message = "体脂率不能为负数")
    @DecimalMax(value = "100.00", message = "体脂率不能超过 100%")
    @Digits(integer = 3, fraction = 2) BigDecimal bodyFatPct,
    @DecimalMin(value = "0.00", message = "肌肉量不能为负数")
    @Digits(integer = 4, fraction = 2) BigDecimal muscleMassKg) {
}
