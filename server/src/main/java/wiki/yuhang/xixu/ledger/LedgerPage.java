package wiki.yuhang.xixu.ledger;

import java.util.List;

public record LedgerPage(List<LedgerEntry> items, long total, int page, int size) {
}
