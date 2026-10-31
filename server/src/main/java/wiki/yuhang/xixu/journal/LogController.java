package wiki.yuhang.xixu.journal;

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
@RequestMapping("/api/logs")
public class LogController {
    private final LogService logs;

    public LogController(LogService logs) {
        this.logs = logs;
    }

    @PostMapping
    public ResponseEntity<LogEntry> create(@AuthenticationPrincipal AuthUser user,
                                           @Valid @RequestBody LogEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logs.create(user.id(), request));
    }

    @GetMapping
    public LogPage list(@AuthenticationPrincipal AuthUser user,
                        @RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        @RequestParam(required = false) String type,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int size) {
        return logs.list(user.id(), from, to, type, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        return logs.get(user.id(), id).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "日志不存在")));
    }
}
