package wiki.yuhang.xixu.ledger;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wiki.yuhang.xixu.auth.AuthUser;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final LedgerService ledger;

    public LedgerController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/entries")
    public ResponseEntity<LedgerEntry> create(@AuthenticationPrincipal AuthUser user,
                                              @Valid @RequestBody LedgerEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledger.create(user.id(), request));
    }

    @GetMapping("/entries")
    public LedgerPage list(@AuthenticationPrincipal AuthUser user,
                           @RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(defaultValue = "false") boolean includeVoided) {
        return ledger.list(user.id(), from, to, page, size, includeVoided);
    }

    @GetMapping("/summary")
    public LedgerSummary summary(@AuthenticationPrincipal AuthUser user,
                                 @RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to) {
        return ledger.summary(user.id(), from, to);
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<?> get(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return ledger.get(user.id(), id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "账目不存在")));
    }

    @PostMapping("/entries/{id}/void")
    public ResponseEntity<?> voidEntry(@AuthenticationPrincipal AuthUser user,
                                       @PathVariable long id,
                                       @Valid @RequestBody VoidEntryRequest request) {
        LedgerService.VoidResult result = ledger.voidEntry(user.id(), id, request.reason());
        if (result.outcome() == LedgerService.VoidOutcome.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "账目不存在"));
        }
        if (result.outcome() == LedgerService.VoidOutcome.ALREADY_VOIDED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "该账目已作废"));
        }
        return ResponseEntity.ok(result.entry());
    }
}
