package wiki.yuhang.xixu.journal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record LogEntryRequest(
    @NotBlank(message = "请选择日志类型")
    @Pattern(regexp = "NOTE|WORKOUT", message = "日志类型必须是 NOTE 或 WORKOUT")
    String entryType,

    @NotNull(message = "发生日期不能为空")
    LocalDate occurredOn,

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    String title,

    @Size(max = 2000, message = "正文最长 2000 字")
    String content,

    @Size(max = 30, message = "一次训练最多记录 30 组")
    List<@Valid WorkoutSetRequest> sets) {
}
