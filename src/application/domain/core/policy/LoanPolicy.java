package application.domain.core.policy;

import java.time.LocalDate;
import java.util.Objects;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;

/**
 * Contrat de politique d'emprunt.
 * Ferme la logique "qui peut emprunter quoi, combien de temps, combien en parallèle".
 *
 * Couche : Domaine (Policy). Sans dépendance vers l’infrastructure.
 */
public interface LoanPolicy {

    /** Durée d'emprunt (jours) par défaut lorsqu'aucune règle spécifique n'est appliquée. */
    int DEFAULT_LOAN_DURATION_DAYS = 21;

    /** Seuil par défaut d'emprunts simultanés autorisés. */
    int DEFAULT_MAX_CONCURRENT_LOANS = 5;

    /**
     * Indique si l'adhérent peut emprunter ce document à la date donnée,
     * au regard des règles métier (quota, retards, etc.).
     *
     * @param adherent           l'adhérent demandeur (non null)
     * @param document           le document ciblé (non null)
     * @param borrowDate         date d'emprunt envisagée (non null)
     * @param activeLoansCount   nombre d'emprunts actifs actuels pour l'adhérent (>=0)
     * @param hasOverdueLoans    vrai si l'adhérent a au moins un retard en cours
     * @return true si autorisé, false sinon
     */
    boolean canBorrow(Adherent adherent,
                      Document document,
                      LocalDate borrowDate,
                      int activeLoansCount,
                      boolean hasOverdueLoans);

    /**
     * Calcule la date d'échéance (retour prévu) pour un nouvel emprunt.
     * @param borrowDate date d'emprunt (non null)
     * @param document   document (non null)
     * @param adherent   adhérent (non null)
     * @return date d'échéance (non null)
     */
    LocalDate computeDueDate(LocalDate borrowDate, Document document, Adherent adherent);

    /**
     * Nombre maximal d'emprunts simultanés autorisés pour cet adhérent.
     * Peut varier selon le profil si tu ajoutes des rôles/capabilities.
     */
    int maxConcurrentLoans(Adherent adherent);

    /**
     * Durée d'emprunt en jours (peut dépendre du document et/ou de l'adhérent).
     */
    int loanDurationDays(Document document, Adherent adherent);

    // Helpers par défaut (optionnels)
    static void requireNonNullAll(Object... values) {
        for (Object v : values) Objects.requireNonNull(v, "Argument must not be null");
    }
}
