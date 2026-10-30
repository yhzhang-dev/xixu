package wiki.yuhang.xixu.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record LedgerEntry(long id, long userId, String entryType, BigDecimal amount,
                          LocalDate occurredOn, LocalTime occurredTime, String categoryName,
                          String channelName, String merchant, String note, String sourceType,
                          int status, String voidReason, LocalDateTime voidedAt,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
}
