package wiki.yuhang.xixu.wellness;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WellnessService {
    private final WellnessRepository repository;

    public WellnessService(WellnessRepository repository) {
        this.repository = repository;
    }

    public BodyMeasurement createBody(long userId, BodyMeasurementRequest request) {
        if (request.weightKg() == null && request.bodyFatPct() == null && request.muscleMassKg() == null) {
            throw badRequest("请至少填写体重、体脂率或肌肉量中的一项");
        }
        long id = repository.insertBody(userId, request);
        return repository.findBody(userId, id).orElseThrow();
    }

    public SleepSession createSleep(long userId, SleepSessionRequest request) {
        int measuredStages = optional(request.deepMinutes()) + optional(request.lightMinutes()) +
            optional(request.remMinutes());
        if (measuredStages > request.durationMinutes() + 5) {
            throw badRequest("深睡、浅睡与快速眼动时长不能超过总睡眠时长");
        }
        long id = repository.insertSleep(userId, request);
        return repository.findSleep(userId, id).orElseThrow();
    }

    public List<BodyMeasurement> body(long userId, LocalDate from, LocalDate to) {
        DateRange range = range(from, to);
        return repository.listBody(userId, range.from(), range.to());
    }

    public List<SleepSession> sleep(long userId, LocalDate from, LocalDate to) {
        DateRange range = range(from, to);
        return repository.listSleep(userId, range.from(), range.to());
    }

    private int optional(Integer value) { return value == null ? 0 : value; }

    private DateRange range(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(89) : from;
        if (start.isAfter(end)) throw badRequest("开始日期不能晚于结束日期");
        if (ChronoUnit.DAYS.between(start, end) > 366) throw badRequest("一次最多查询 366 天");
        return new DateRange(start, end);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record DateRange(LocalDate from, LocalDate to) { }
}
