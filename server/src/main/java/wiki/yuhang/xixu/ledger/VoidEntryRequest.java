package wiki.yuhang.xixu.ledger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidEntryRequest(
    @NotBlank(message = "请填写作废原因")
    @Size(max = 200, message = "作废原因最长 200 字")
    String reason) {
}
