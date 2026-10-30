package wiki.yuhang.xixu.ledger;

import java.math.BigDecimal;

public record LedgerSummary(long count, BigDecimal expense, BigDecimal income) {
}
