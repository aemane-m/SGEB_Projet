package application.domain.core.policy;

import java.time.LocalDate;
import java.util.Objects;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;

/**
 * <p>
 * Contrat de <strong>politique d'emprunt</strong> dans le domaine SGEB.
 * </p>
 *
 * <p>
 * Cette interface centralise la logique métier de haut niveau :
 * </p>
 *
 * <ul>
 *     <li>Qui peut emprunter quoi ?</li>
 *     <li>Combien de documents en parallèle ?</li>
 *     <li>Pour quelle durée (date de retour prévue) ?</li>
 * </ul>
 *
 * <p>
 * Elle est purement <strong>domaine</strong> : aucune dépendance à l’infrastructure
 * (base de données, UI, etc.).  
 * Les services d’application l’utilisent pour décider s’il faut créer un
 * {@link application.domain.core.emprunt.Emprunt} et avec quelle date d’échéance.
 * </p>
 *
 * <h3>Constants SGEB par défaut</h3>
 * <ul>
 *     <li>{@link #DEFAULT_LOAN_DURATION_DAYS} = 21 jours</li>
 *     <li>{@link #DEFAULT_MAX_CONCURRENT_LOANS} = 5 emprunts simultanés</li>
 * </ul>
 *
 * <p>
 * Ces valeurs sont alignées avec les règles métier déjà implémentées dans
 * {@link Adherent} (max 5 emprunts, durée 21 jours) pour éviter les
 * duplications et les divergences.
 * </p>
 */
public interface LoanPolicy {

    /** Durée d'emprunt (en jours) utilisée par défaut (SGEB : 21 jours). */
    int DEFAULT_LOAN_DURATION_DAYS = 21;

    /** Nombre maximal d'emprunts simultanés autorisés par défaut (SGEB : 5). */
    int DEFAULT_MAX_CONCURRENT_LOANS = 5;

    // ======================================================================
    // =============================== CONTRATS ==============================
    // ======================================================================

    /**
     * Détermine si l’adhérent est autorisé à emprunter le document à la date donnée,
     * du point de vue de la <strong>politique d’emprunt</strong>.
     *
     * <p>
     * Cette méthode complète les vérifications déjà faites dans :
     * </p>
     * <ul>
     *     <li>{@link Adherent#peutEmprunter(LocalDate)} pour l’état métier
     *         (retards, pénalités, plafond interne)</li>
     *     <li>Une éventuelle politique d’accès (RBAC) basée sur
     *         {@code Role}/{@code Capability}</li>
     * </ul>
     *
     * <p>
     * Les paramètres {@code activeLoansCount} et {@code hasOverdueLoans}
     * permettent aux implémentations d’éviter de recalculer l’état complet
     * de l’adhérent si cela a déjà été fait côté service.
     * </p>
     *
     * @param adherent         adhérent demandeur (non null)
     * @param document         document ciblé (non null)
     * @param borrowDate       date d’emprunt envisagée (non null)
     * @param activeLoansCount nombre d’emprunts actifs de l’adhérent (>= 0)
     * @param hasOverdueLoans  vrai si l’adhérent a au moins un emprunt en retard
     * @return {@code true} si, selon la politique d’emprunt, l’opération est autorisée
     */
    boolean canBorrow(Adherent adherent,
                      Document document,
                      LocalDate borrowDate,
                      int activeLoansCount,
                      boolean hasOverdueLoans);

    /**
     * Calcule la <strong>date de retour prévue</strong> pour un nouvel emprunt.
     *
     * <p>
     * Par défaut, une implémentation simple peut se contenter de faire :
     * </p>
     *
     * <pre>{@code
     * return borrowDate.plusDays(loanDurationDays(document, adherent));
     * }</pre>
     *
     * <p>
     * mais une politique plus avancée peut adapter la durée en fonction :
     * </p>
     * <ul>
     *     <li>du type de document (Livre, Magazine, CD…)</li>
     *     <li>du profil de l’adhérent (adhérent classique, VIP, partenaire…)</li>
     * </ul>
     *
     * @param borrowDate date d’emprunt (non null)
     * @param document   document concerné (non null)
     * @param adherent   adhérent qui emprunte (non null)
     * @return date d’échéance (non null)
     */
    LocalDate computeDueDate(LocalDate borrowDate, Document document, Adherent adherent);

    /**
     * Donne le <strong>nombre maximal d’emprunts simultanés</strong>
     * autorisés pour un adhérent donné.
     *
     * <p>
     * Une implémentation simple renverra systématiquement
     * {@link #DEFAULT_MAX_CONCURRENT_LOANS}.  
     * Des politiques plus évoluées peuvent augmenter ou diminuer ce plafond
     * selon le profil de l’adhérent.
     * </p>
     *
     * @param adherent adhérent concerné (non null)
     * @return plafond d’emprunts simultanés autorisés (&gt;= 0)
     */
    int maxConcurrentLoans(Adherent adherent);

    /**
     * Retourne la <strong>durée d’emprunt (en jours)</strong> applicable pour
     * un document donné et un adhérent donné.
     *
     * <p>
     * Une implémentation de base peut renvoyer simplement
     * {@link #DEFAULT_LOAN_DURATION_DAYS}.  
     * Une politique plus fine peut par exemple :
     * </p>
     * <ul>
     *     <li>Accorder une durée plus courte pour des documents très demandés.</li>
     *     <li>Accorder une durée plus longue à certains profils d’adhérents.</li>
     * </ul>
     *
     * @param document document concerné (non null)
     * @param adherent adhérent emprunteur (non null)
     * @return durée en jours (&gt; 0)
     */
    int loanDurationDays(Document document, Adherent adherent);

    // ======================================================================
    // ============================= HELPERS PAR DÉFAUT =====================
    // ======================================================================

    /**
     * Helper utilitaire pour vérifier qu’aucun argument n’est null.
     *
     * <p>
     * Utilisable dans les implémentations de {@link LoanPolicy}
     * pour éviter de répéter les mêmes blocs de vérification.
     * </p>
     *
     * <pre>{@code
     * LoanPolicy.requireNonNullAll(adherent, document, borrowDate);
     * }</pre>
     *
     * @param values liste de valeurs à vérifier
     * @throws NullPointerException si au moins une valeur est null
     */
    static void requireNonNullAll(Object... values) {
        for (Object v : values) {
            Objects.requireNonNull(v, "Argument must not be null");
        }
    }
}