package wiki.yuhang.xixu.journal;

import java.math.BigDecimal;

public record WorkoutSet(long id, int sequenceNo, String exerciseName,
                         Integer repetitions, BigDecimal weightKg, Integer durationSeconds) {
}
