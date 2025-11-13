package application.domain.core.policy;

import java.time.LocalDate;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;

/**
 * Implémentation par défaut des règles d'emprunt SGEB.
 * - 21 jours de durée
 * - 5 emprunts simultanés max
 * - Interdit si un retard en cours
 */
public final class StandardLoanPolicy implements LoanPolicy {

    private final int durationDays;
    private final int maxConcurrent;

    public StandardLoanPolicy() {
        this(LoanPolicy.DEFAULT_LOAN_DURATION_DAYS, LoanPolicy.DEFAULT_MAX_CONCURRENT_LOANS);
    }

    public StandardLoanPolicy(int durationDays, int maxConcurrent) {
        if (durationDays <= 0) throw new IllegalArgumentException("durationDays must be > 0");
        if (maxConcurrent <= 0) throw new IllegalArgumentException("maxConcurrent must be > 0");
        this.durationDays = durationDays;
        this.maxConcurrent = maxConcurrent;
    }

    @Override
    public boolean canBorrow(Adherent adherent,
                             Document document,
                             LocalDate borrowDate,
                             int activeLoansCount,
                             boolean hasOverdueLoans) {
        LoanPolicy.requireNonNullAll(adherent, document, borrowDate);
        if (hasOverdueLoans) return false;
        if (activeLoansCount >= maxConcurrent) return false;
        // Ici tu peux ajouter d'autres règles (ex: document de référence non empruntable, rôles, etc.)
        return true;
    }

    @Override
    public LocalDate computeDueDate(LocalDate borrowDate, Document document, Adherent adherent) {
        LoanPolicy.requireNonNullAll(borrowDate, document, adherent);
        return borrowDate.plusDays(loanDurationDays(document, adherent));
    }

    @Override
    public int maxConcurrentLoans(Adherent adherent) {
        // Extension future : adapter au rôle/capability de l'adhérent
        return maxConcurrent;
    }

    @Override
    public int loanDurationDays(Document document, Adherent adherent) {
        // Extension future : adapter selon le type de document ou le profil
        return durationDays;
    }
}
