package wiki.yuhang.xixu.ledger;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record LedgerEntryRequest(
    @NotBlank(message = "收支方向不能为空")
    @Pattern(regexp = "EXPENSE|INCOME", message = "收支方向必须为 EXPENSE 或 INCOME")
    String entryType,

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    @Digits(integer = 10, fraction = 2, message = "金额最多 10 位整数、2 位小数")
    BigDecimal amount,

    @NotNull(message = "发生日期不能为空")
    LocalDate occurredOn,

    LocalTime occurredTime,

    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类最长 50 字")
    String categoryName,

    @Size(max = 30, message = "渠道最长 30 字")
    String channelName,

    @Size(max = 100, message = "商家最长 100 字")
    String merchant,

    @Size(max = 500, message = "备注最长 500 字")
    String note) {
}
