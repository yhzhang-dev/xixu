package wiki.yuhang.xixu.assistant;

import java.math.BigDecimal;

public record MonthlySnapshot(String month, long workoutCount, long ledgerCount,
                              BigDecimal expense, BigDecimal income) {
}
