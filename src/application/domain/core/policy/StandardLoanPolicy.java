package application.domain.core.policy;

import java.time.LocalDate;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;

/**
 * <p>
 * Implémentation standard de la {@link LoanPolicy} pour le SGEB.
 * </p>
 *
 * <h3>Règles métier par défaut</h3>
 * <ul>
 *     <li>Durée d'emprunt : 21 jours (par défaut)</li>
 *     <li>Nombre maximal d'emprunts simultanés : 5</li>
 *     <li>Interdiction d'emprunter si l'adhérent a au moins un emprunt en retard</li>
 * </ul>
 *
 * <p>
 * Cette classe centralise la politique "globale" d’emprunt.  
 * Elle complète les règles déjà présentes dans {@link Adherent#peutEmprunter(LocalDate)}
 * (retards, pénalités, etc.) en offrant un point unique pour :
 * </p>
 *
 * <ul>
 *     <li>Calculer la date d’échéance d’un nouvel emprunt</li>
 *     <li>Déterminer le plafond d’emprunts simultanés</li>
 *     <li>Filtrer certaines catégories de documents ou profils à l’avenir</li>
 * </ul>
 *
 * <p>
 * Les valeurs par défaut sont alignées sur les constantes de l’interface
 * {@link LoanPolicy} afin d’éviter les divergences.
 * </p>
 */
public final class StandardLoanPolicy implements LoanPolicy {

    /** Durée d'emprunt en jours appliquée par cette policy. */
    private final int durationDays;

    /** Plafond d'emprunts simultanés pour cette policy. */
    private final int maxConcurrent;

    // ======================================================================
    // ============================= CONSTRUCTEURS ===========================
    // ======================================================================

    /**
     * Construit la politique standard SGEB avec les valeurs par défaut :
     * <ul>
     *     <li>{@link LoanPolicy#DEFAULT_LOAN_DURATION_DAYS}</li>
     *     <li>{@link LoanPolicy#DEFAULT_MAX_CONCURRENT_LOANS}</li>
     * </ul>
     */
    public StandardLoanPolicy() {
        this(LoanPolicy.DEFAULT_LOAN_DURATION_DAYS,
             LoanPolicy.DEFAULT_MAX_CONCURRENT_LOANS);
    }

    /**
     * Construit une politique d’emprunt paramétrable.
     *
     * @param durationDays durée d'emprunt en jours (&gt; 0)
     * @param maxConcurrent nombre maximum d'emprunts simultanés (&gt; 0)
     *
     * @throws IllegalArgumentException si l'un des paramètres est &le; 0
     */
    public StandardLoanPolicy(int durationDays, int maxConcurrent) {
        if (durationDays <= 0) {
            throw new IllegalArgumentException("durationDays must be > 0");
        }
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent must be > 0");
        }
        this.durationDays = durationDays;
        this.maxConcurrent = maxConcurrent;
    }

    // ======================================================================
       // ============================ LOGIQUE MÉTIER ===========================
    // ======================================================================

    /**
     * <p>
     * Règle standard :
     * </p>
     * <ul>
     *     <li>Refus si {@code hasOverdueLoans} est vrai (au moins un retard en cours).</li>
     *     <li>Refus si {@code activeLoansCount} atteint {@code maxConcurrent}.</li>
     *     <li>Sinon, la politique ne s’oppose pas à l’emprunt.</li>
     * </ul>
     */
    @Override
    public boolean canBorrow(Adherent adherent,
                             Document document,
                             LocalDate borrowDate,
                             int activeLoansCount,
                             boolean hasOverdueLoans) {

        LoanPolicy.requireNonNullAll(adherent, document, borrowDate);

        if (hasOverdueLoans) {
            return false;
        }
        if (activeLoansCount >= maxConcurrent) {
            return false;
        }
        return true;
    }

    /**
     * Calcule la date de retour prévue à partir de la date d’emprunt
     * et de la durée retournée par {@link #loanDurationDays(Document, Adherent)}.
     */
    @Override
    public LocalDate computeDueDate(LocalDate borrowDate, Document document, Adherent adherent) {
        LoanPolicy.requireNonNullAll(borrowDate, document, adherent);
        return borrowDate.plusDays(loanDurationDays(document, adherent));
    }

    /**
     * Nombre maximal d'emprunts simultanés autorisés pour cet adhérent
     * selon cette policy.
     *
     * <p>
     * Implémentation simple : retourne le plafond global
     * {@link #maxConcurrent}.  
     * </p>
     */
    @Override
    public int maxConcurrentLoans(Adherent adherent) {
        return maxConcurrent;
    }

    /**
     * Durée d'emprunt en jours pour ce document et cet adhérent.
     *
     * <p>
     * Implémentation standard : durée fixe {@link #durationDays}. 
     * </p>
     */
    @Override
    public int loanDurationDays(Document document, Adherent adherent) {
        return durationDays;
    }
}