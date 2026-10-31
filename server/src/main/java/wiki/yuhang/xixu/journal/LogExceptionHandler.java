package wiki.yuhang.xixu.journal;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = LogController.class)
public class LogExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst().map(error -> error.getDefaultMessage()).orElse("请检查输入内容");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> invalidRequest(ResponseStatusException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", exception.getReason() == null ? "请求内容不正确" : exception.getReason()));
    }
}
