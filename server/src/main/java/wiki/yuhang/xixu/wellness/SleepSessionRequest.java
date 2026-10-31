package wiki.yuhang.xixu.wellness;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record SleepSessionRequest(
    @NotNull(message = "请选择醒来日期") LocalDate sleepOn,
    @NotNull(message = "请选择睡眠类型")
    @Pattern(regexp = "NIGHT|NAP", message = "睡眠类型不正确") String sessionKind,
    @NotNull(message = "请填写睡眠时长")
    @Min(value = 1, message = "睡眠时长至少 1 分钟")
    @Max(value = 65535, message = "睡眠时长过长") Integer durationMinutes,
    @Min(value = 0, message = "深睡时长不能为负数")
    @Max(65535) Integer deepMinutes,
    @Min(value = 0, message = "浅睡时长不能为负数")
    @Max(65535) Integer lightMinutes,
    @Min(value = 0, message = "快速眼动时长不能为负数")
    @Max(65535) Integer remMinutes,
    @Min(value = 0, message = "清醒时长不能为负数")
    @Max(65535) Integer awakeMinutes) {
}
