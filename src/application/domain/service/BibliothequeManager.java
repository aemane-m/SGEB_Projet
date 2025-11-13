package application.domain.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import application.domain.core.document.Document;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;
import application.domain.core.emprunt.Penalite;
import application.domain.core.personne.Adherent;
import application.domain.core.policy.LoanPolicy;
import application.domain.core.policy.PenaltyCalculator;
import application.domain.core.time.TimeProvider;
import application.domain.ports.external.NotificationPort;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;

/**
 * Façade métier pilotant les cas d'usage "Emprunter" et "Retourner".
 * <p>
 * (Simplifié : L'autorisation est implicite car seul un Admin peut
 * accéder à ce service).
 * </p>
 */
public final class BibliothequeManager {

    // SUPPRIMÉ : Les constantes de "Capability" ne sont plus nécessaires
    // public static final String CAP_BORROW = "BORROW";
    // public static final String CAP_RETURN = "RETURN";

    // Ports
    private final DocumentRepository documentRepo;
    private final AdherentRepository adherentRepo;
    private final EmpruntRepository empruntRepo;
    private final PenaliteRepository penaliteRepo;

    // Externals
    private final NotificationPort notifier;

    // Policies / Providers
    private final LoanPolicy loanPolicy;
    private final PenaltyCalculator penaltyCalculator;
    private final TimeProvider timeProvider;

    // SUPPRIMÉ : Le service d'autorisation
    // private final AuthorizationService authz;

    public BibliothequeManager(DocumentRepository documentRepo,
                               AdherentRepository adherentRepo,
                               EmpruntRepository empruntRepo,
                               PenaliteRepository penaliteRepo,
                               NotificationPort notifier,
                               LoanPolicy loanPolicy,
                               PenaltyCalculator penaltyCalculator,
                               TimeProvider timeProvider
                               /* SUPPRIMÉ : AuthorizationService authz */) {
        this.documentRepo = Objects.requireNonNull(documentRepo);
        this.adherentRepo = Objects.requireNonNull(adherentRepo);
        this.empruntRepo = Objects.requireNonNull(empruntRepo);
        this.penaliteRepo = Objects.requireNonNull(penaliteRepo);
        this.notifier = Objects.requireNonNull(notifier);
        this.loanPolicy = Objects.requireNonNull(loanPolicy);
        this.penaltyCalculator = Objects.requireNonNull(penaltyCalculator);
        this.timeProvider = Objects.requireNonNull(timeProvider);
        // SUPPRIMÉ : this.authz = ...
    }

    /**
     * Exception métier levée en cas de violation d'une règle de domaine
     * (ex. adhérent inactif, document indisponible, quota dépassé).
     */
    public static final class DomainRuleViolation extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public DomainRuleViolation(String message) { super(message); }
    }

    // ======================================================================
    // Use case : Emprunter
    // ======================================================================

    /**
     * Démarre un emprunt si les prérequis de métier sont satisfaits.
     *
     * @param adherentId  identifiant adhérent
     * @param documentId  identifiant document
     * @return l'emprunt persisté
     * @throws DomainRuleViolation si une contrainte métier est violée
     */
    public Emprunt emprunterDocument(/* SUPPRIMÉ: UserContext ctx, */ Long adherentId, Long documentId) {
        // SUPPRIMÉ : L'appel à authz.requireCapability(...)

        Adherent adh = adherentRepo.findById(adherentId)
                .orElseThrow(() -> new DomainRuleViolation("Adhérent introuvable: " + adherentId));

        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new DomainRuleViolation("Document introuvable: " + documentId));

        LocalDate today = today();

        // Quota & retards
        List<Emprunt> loansOfAdh = empruntRepo.findByAdherentId(adherentId);
        int activeLoansCount = (int) loansOfAdh.stream().filter(Emprunt::estEnCours).count();
        boolean hasOverdueLoans = loansOfAdh.stream().anyMatch(e -> e.estEnRetard(today));

        // Disponibilité
        boolean documentDisponible = empruntRepo.findByDocumentId(documentId)
                .stream().noneMatch(Emprunt::estEnCours);
        if (!documentDisponible) {
            throw new DomainRuleViolation("Document déjà emprunté");
        }

        // Règles métier
        if (!loanPolicy.canBorrow(adh, doc, today, activeLoansCount, hasOverdueLoans)) {
            throw new DomainRuleViolation("Règles métier: emprunt non autorisé (quota/retards)");
        }

        // Création
        int dureeJours = loanPolicy.loanDurationDays(doc, adh);
        Emprunt e = Emprunt.creer(doc, adh, today, dureeJours);

        Emprunt saved = empruntRepo.save(e);
        safeNotifyBorrow(adh, doc, saved);
        return saved;
    }

    // ======================================================================
    // Use case : Retourner
    // ======================================================================

    /**
     * Enregistre le retour d'un emprunt et génère, le cas échéant, une pénalité.
     *
     * @param empruntId identifiant d'emprunt
     * @return l'emprunt mis à jour
     * @throws DomainRuleViolation si l'emprunt est introuvable
     */
    public Emprunt retournerDocument(/* SUPPRIMÉ: UserContext ctx, */ Long empruntId) {
        // SUPPRIMÉ : L'appel à authz.requireCapability(...)

        Emprunt e = empruntRepo.findById(empruntId)
                .orElseThrow(() -> new DomainRuleViolation("Emprunt introuvable: " + empruntId));

        if (!e.estEnCours()) {
            return e; // Déjà retourné, on ne fait rien
        }

        LocalDate today = today();
        e.cloturerRetour(today);

        Money penalty = penaltyCalculator.computePenalty(e, today);
        if (penalty.isPositive()) {
            Penalite p = Penalite.creerPour(e, penalty, today);
            penaliteRepo.save(p);
        }

        Emprunt saved = empruntRepo.save(e);
        safeNotifyReturn(saved, penalty);
        return saved;
    }

    // ======================================================================
    // Use case : Consultation pénalité
    // ======================================================================

    /**
     * Calcule la pénalité due au jour courant pour un emprunt donné (sans persistance).
     * @param empruntId identifiant de l'emprunt
     * @return montant de pénalité calculé à la date du jour
     * @throws DomainRuleViolation si l'emprunt est introuvable
     */
    public Money calculerPenaliteCourante(Long empruntId) {
        Emprunt e = empruntRepo.findById(empruntId)
                .orElseThrow(() -> new DomainRuleViolation("Emprunt introuvable: " + empruntId));
        return penaltyCalculator.computePenalty(e, today());
    }

    // ======================================================================
    // Helpers (inchangés)
    // ======================================================================

    private LocalDate today() {
        return timeProvider.now().toLocalDate();
    }

    private void safeNotifyBorrow(Adherent adh, Document doc, Emprunt e) {
        try {
            String destinataire = adh.getEmail(); 
            String sujet = "Confirmation d'emprunt";
            String contenu = "Bonjour,\n\nVous avez emprunté : " + doc.getTitre()
                    + "\nDate d'emprunt : " + e.getDateEmprunt()
                    + "\nDate de retour prévue : " + e.getDateRetourPrevue()
                    + "\n\nBonne lecture.";
            notifier.sendNotification(destinataire, sujet, contenu);
        } catch (Exception ignore) { /* best-effort */ }
    }

    private void safeNotifyReturn(Emprunt e, Money penalty) {
        try {
            String destinataire = e.getEmprunteur().getEmail();
            String sujet = "Retour enregistré";
            String contenu = "Bonjour,\n\nRetour enregistré pour : " + e.getDocument().getTitre()
                    + "\nDate de retour : " + e.getDateRetourEffective()
                    + (penalty.isPositive() ? ("\nPénalité due : " + penalty) : "\nAucune pénalité due.")
                    + "\n\nMerci.";
            notifier.sendNotification(destinataire, sujet, contenu);
        } catch (Exception ignore) { /* best-effort */ }
    }
}