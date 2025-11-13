package application.domain.core.time;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Implémentation réelle du fournisseur de temps.
 * Utilise l'horloge système locale.
 */
public class SystemTimeProvider implements TimeProvider {

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
