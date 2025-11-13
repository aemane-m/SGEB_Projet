package application.domain.core.time;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FixedTimeProvider implements TimeProvider {
    private final LocalDateTime fixed;

    public FixedTimeProvider(LocalDateTime fixed) {
        this.fixed = fixed;
    }

    public LocalDate today() { return fixed.toLocalDate(); }
    public LocalDateTime now() { return fixed; }
}

