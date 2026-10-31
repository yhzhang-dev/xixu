package wiki.yuhang.xixu.journal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WorkoutSetRequest(
    @NotBlank(message = "请填写训练项目")
    @Size(max = 80, message = "训练项目最长 80 字")
    String exerciseName,

    @Min(value = 1, message = "次数必须大于 0")
    @Max(value = 10000, message = "单组次数不能超过 10000")
    Integer repetitions,

    @DecimalMin(value = "0.00", message = "重量不能为负")
    @Digits(integer = 5, fraction = 2, message = "重量最多 5 位整数、2 位小数")
    BigDecimal weightKg,

    @Min(value = 1, message = "时长必须大于 0 秒")
    @Max(value = 86400, message = "单组时长不能超过一天")
    Integer durationSeconds) {
}
