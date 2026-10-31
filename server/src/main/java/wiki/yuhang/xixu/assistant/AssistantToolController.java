package wiki.yuhang.xixu.assistant;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import wiki.yuhang.xixu.auth.AuthUser;

@RestController
@RequestMapping("/api/assistant/tools")
public class AssistantToolController {
    private final AssistantToolService tools;

    public AssistantToolController(AssistantToolService tools) {
        this.tools = tools;
    }

    @GetMapping("/monthly-snapshot")
    public MonthlySnapshot monthlySnapshot(@AuthenticationPrincipal AuthUser user,
                                           @RequestParam String month) {
        return tools.monthlySnapshot(user.id(), month);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> invalidMonth(ResponseStatusException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", exception.getReason() == null ? "月份不正确" : exception.getReason()));
    }
}
