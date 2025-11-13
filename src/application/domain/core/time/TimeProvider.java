package application.domain.core.time;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fournit la date et l'heure courante du système.
 * 
 * Cette interface permet de découpler la logique métier du temps système.
 * On peut ainsi injecter un mock dans les tests (ex: FixedTimeProvider).
 */
public interface TimeProvider {

    /**
     * Retourne la date courante.
     * @return LocalDate du jour.
     */
    LocalDate today();

    /**
     * Retourne la date et l'heure courante.
     * @return LocalDateTime courant.
     */
    LocalDateTime now();
}
