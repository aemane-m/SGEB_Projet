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
import application.domain.ports.external.NotificationPort;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;

/**
 * Façade métier pilotant les cas d'usage "Emprunter" et "Retourner".
 */
public final class BibliothequeManager {

    private final DocumentRepository documentRepo;
    private final AdherentRepository adherentRepo;
    private final EmpruntRepository empruntRepo;
    private final PenaliteRepository penaliteRepo;

    // Services externes
    private final NotificationPort notifier;

    // Règles métier
    private final LoanPolicy loanPolicy;
    private final PenaltyCalculator penaltyCalculator;

    public BibliothequeManager(DocumentRepository documentRepo,
                               AdherentRepository adherentRepo,
                               EmpruntRepository empruntRepo,
                               PenaliteRepository penaliteRepo,
                               NotificationPort notifier,
                               LoanPolicy loanPolicy,
                               PenaltyCalculator penaltyCalculator) {

        this.documentRepo = Objects.requireNonNull(documentRepo);
        this.adherentRepo = Objects.requireNonNull(adherentRepo);
        this.empruntRepo = Objects.requireNonNull(empruntRepo);
        this.penaliteRepo = Objects.requireNonNull(penaliteRepo);
        this.notifier = Objects.requireNonNull(notifier);
        this.loanPolicy = Objects.requireNonNull(loanPolicy);
        this.penaltyCalculator = Objects.requireNonNull(penaltyCalculator);
    }

    // ======================================================================
    // Exception métier
    // ======================================================================

    public static final class DomainRuleViolation extends RuntimeException {
        public DomainRuleViolation(String message) { super(message); }
    }

    // ======================================================================
    // Use case : Emprunter
    // ======================================================================

    public Emprunt emprunterDocument(Long adherentId, Long documentId) {

        Adherent adh = adherentRepo.findById(adherentId)
                .orElseThrow(() -> new DomainRuleViolation("Adhérent introuvable: " + adherentId));

        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new DomainRuleViolation("Document introuvable: " + documentId));

        LocalDate today = LocalDate.now();

        // Quota + retards
        List<Emprunt> loansOfAdh = empruntRepo.findByAdherentId(adherentId);
        int activeLoans = (int) loansOfAdh.stream().filter(Emprunt::estEnCours).count();
        boolean hasOverdue = loansOfAdh.stream().anyMatch(e -> e.estEnRetard(today));

        // Document déjà emprunté ?
        boolean disponible =
                empruntRepo.findByDocumentId(documentId).stream().noneMatch(Emprunt::estEnCours);

        if (!disponible) {
            throw new DomainRuleViolation("Document déjà emprunté.");
        }

        // Règles métier
        if (!loanPolicy.canBorrow(adh, doc, today, activeLoans, hasOverdue)) {
            throw new DomainRuleViolation("Règles métier: emprunt non autorisé (quota/retards).");
        }

        // Création
        int duree = loanPolicy.loanDurationDays(doc, adh);
        Emprunt e = Emprunt.creer(doc, adh, today, duree);

        Emprunt saved = empruntRepo.save(e);
        safeNotifyBorrow(adh, doc, saved);

        return saved;
    }

    // ======================================================================
    // Use case : Retourner
    // ======================================================================

    public Emprunt retournerDocument(Long empruntId) {

        Emprunt e = empruntRepo.findById(empruntId)
                .orElseThrow(() -> new DomainRuleViolation("Emprunt introuvable: " + empruntId));

        if (!e.estEnCours()) {
            return e; // déjà retourné
        }

        LocalDate today = LocalDate.now();
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

    public Money calculerPenaliteCourante(Long empruntId) {
        Emprunt e = empruntRepo.findById(empruntId)
                .orElseThrow(() -> new DomainRuleViolation("Emprunt introuvable: " + empruntId));
        return penaltyCalculator.computePenalty(e, LocalDate.now());
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private void safeNotifyBorrow(Adherent adh, Document doc, Emprunt e) {
        try {
            notifier.sendNotification(
                    adh.getEmail(),
                    "Confirmation d'emprunt",
                    "Bonjour,\n\nVous avez emprunté : " + doc.getTitre()
                            + "\nDate d'emprunt : " + e.getDateEmprunt()
                            + "\nDate de retour prévue : " + e.getDateRetourPrevue()
                            + "\n\nBonne lecture."
            );
        } catch (Exception ignore) {}
    }

    private void safeNotifyReturn(Emprunt e, Money penalty) {
        try {
            notifier.sendNotification(
                    e.getEmprunteur().getEmail(),
                    "Retour enregistré",
                    "Bonjour,\n\nRetour enregistré pour : " + e.getDocument().getTitre()
                            + "\nDate de retour : " + e.getDateRetourEffective()
                            + (penalty.isPositive() ? ("\nPénalité due : " + penalty) : "\nAucune pénalité due.")
                            + "\n\nMerci."
            );
        } catch (Exception ignore) {}
    }
}
