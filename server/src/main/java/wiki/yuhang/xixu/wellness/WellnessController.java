package wiki.yuhang.xixu.wellness;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wiki.yuhang.xixu.auth.AuthUser;

@RestController
@RequestMapping("/api/wellness")
public class WellnessController {
    private final WellnessService wellness;

    public WellnessController(WellnessService wellness) {
        this.wellness = wellness;
    }

    @GetMapping("/body")
    public List<BodyMeasurement> body(@AuthenticationPrincipal AuthUser user,
                                       @RequestParam(required = false) LocalDate from,
                                       @RequestParam(required = false) LocalDate to) {
        return wellness.body(user.id(), from, to);
    }

    @PostMapping("/body")
    public ResponseEntity<BodyMeasurement> createBody(@AuthenticationPrincipal AuthUser user,
                                                       @Valid @RequestBody BodyMeasurementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wellness.createBody(user.id(), request));
    }

    @GetMapping("/sleep")
    public List<SleepSession> sleep(@AuthenticationPrincipal AuthUser user,
                                    @RequestParam(required = false) LocalDate from,
                                    @RequestParam(required = false) LocalDate to) {
        return wellness.sleep(user.id(), from, to);
    }

    @PostMapping("/sleep")
    public ResponseEntity<SleepSession> createSleep(@AuthenticationPrincipal AuthUser user,
                                                    @Valid @RequestBody SleepSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wellness.createSleep(user.id(), request));
    }
}
