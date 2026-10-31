package wiki.yuhang.xixu.wellness;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SleepSession(long id, LocalDate sleepOn, String sessionKind,
                           Integer durationMinutes, Integer deepMinutes,
                           Integer lightMinutes, Integer remMinutes,
                           Integer awakeMinutes, LocalDateTime createdAtUtc) {
}
