package application.domain.service;

import application.domain.core.document.Document;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;
import application.domain.core.emprunt.Penalite;
import application.domain.core.personne.Adherent;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Implémentation du service Emprunt.
 * CORRIGÉ : Implémente isDocumentEmprunte(Long documentId) pour correspondre
 * à l'interface et gère la conversion Long -> long.
 */
public class DefaultEmpruntService implements EmpruntService {

    // --- Constantes Métier ---
    public static final int MAX_EMPRUNTS_PAR_ADHERENT = 5;
    public static final int DUREE_EMPRUNT_SEMAINES = 3;
    public static final double PENALITE_PAR_JOUR_RETARD = 0.50;

    // --- Dépendances (Repositories) ---
    private final EmpruntRepository empruntRepository;
    private final DocumentRepository documentRepository;
    private final AdherentRepository adherentRepository;
    private final PenaliteRepository penaliteRepository;

    public DefaultEmpruntService(EmpruntRepository empruntRepository,
                                 DocumentRepository documentRepository,
                                 AdherentRepository adherentRepository,
                                 PenaliteRepository penaliteRepository) {
        this.empruntRepository = Objects.requireNonNull(empruntRepository);
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.adherentRepository = Objects.requireNonNull(adherentRepository);
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Emprunt borrow(long adherentId, long documentId, LocalDate dateEmprunt) throws EmpruntException {
        
        Adherent adherent = adherentRepository.findById(adherentId)
                .orElseThrow(() -> new EmpruntException("Adhérent non trouvé (ID: " + adherentId + ")"));
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EmpruntException("Document non trouvé (ID: " + documentId + ")"));

        // Contrainte 1: Disponibilité (Appelle la méthode corrigée)
        if (this.isDocumentEmprunte(documentId)) {
            throw new EmpruntException("Le document '" + document.getTitre() + "' est déjà en cours d'emprunt.");
        }
        
        // Contrainte 3: Statut de l'adhérent (pénalités)
        List<Penalite> penalitesActives = penaliteRepository.findActivesByAdherentId(adherentId);
        if (!penalitesActives.isEmpty()) {
            throw new EmpruntException("L'adhérent a des pénalités non réglées (" + penalitesActives.size() + "). Emprunt impossible.");
        }

        // Contrainte 2: Nombre maximal d'emprunts
        // Note: 'findActiveEmprunts' est lent s'il y a bcp d'emprunts.
        // Une requête 'countActiveByAdherentId' serait mieux, mais c'est fonctionnel.
        List<Emprunt> empruntsActifsAdherent = empruntRepository.findActiveEmprunts().stream()
                .filter(e -> e.getEmprunteur() != null && e.getEmprunteur().getId() != null && e.getEmprunteur().getId().equals(adherentId))
                .toList();
        
        if (empruntsActifsAdherent.size() >= MAX_EMPRUNTS_PAR_ADHERENT) {
            throw new EmpruntException("L'adhérent a atteint le nombre maximal d'emprunts (" + MAX_EMPRUNTS_PAR_ADHERENT + ").");
        }

        // 3. Logique de création (Dates)
        LocalDate dateRetourPrevue = dateEmprunt.plusWeeks(DUREE_EMPRUNT_SEMAINES);
        
        // 4. Créer et sauvegarder
        Emprunt nouvelEmprunt = Emprunt.creer(document, adherent, dateEmprunt, dateRetourPrevue);
        return empruntRepository.save(nouvelEmprunt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Penalite returnEmprunt(long empruntId, LocalDate dateRetour) throws EmpruntException {
        Emprunt emprunt = empruntRepository.findById(empruntId)
                .orElseThrow(() -> new EmpruntException("Emprunt non trouvé (ID: " + empruntId + ")"));
        
        if (!emprunt.estEnCours()) {
            throw new EmpruntException("Cet emprunt a déjà été retourné le " + emprunt.getDateRetourEffective());
        }

        emprunt.enregistrerRetour(dateRetour);
        empruntRepository.save(emprunt); 

        long joursDeRetard = ChronoUnit.DAYS.between(emprunt.getDateRetourPrevue(), dateRetour);
        
        if (joursDeRetard > 0) {
            double montantDu = joursDeRetard * PENALITE_PAR_JOUR_RETARD;
            Money montantPenalite = Money.euros(BigDecimal.valueOf(montantDu));
            
            Penalite penalite = Penalite.creerPour(emprunt, montantPenalite, dateRetour);
            penaliteRepository.save(penalite);
            
            return penalite;
        }

        return null; // Pas de pénalité
    }

    @Override
    public List<Emprunt> findAll() {
        return empruntRepository.findAll();
    }

    @Override
    public List<Emprunt> findActive() {
        return empruntRepository.findActiveEmprunts();
    }

    @Override
    public List<Emprunt> findLate(LocalDate dateActuelle) {
        return empruntRepository.findLateEmprunts(dateActuelle);
    }

    /**
     * CORRIGÉ : Implémente la méthode de l'interface avec 'Long'.
     * Gère le cas 'null' et appelle le repository (qui prend 'long').
     */
    @Override
    public boolean isDocumentEmprunte(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return false; // Un document sans ID n'est pas emprunté
        }
        // Appelle la méthode du repository (qui prend un long primitif)
        return empruntRepository.isDocumentEmprunte(documentId);
    }
}

