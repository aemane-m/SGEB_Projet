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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Implémentation du service Emprunt.
 * Implémente les règles métier :
 * - disponibilité du document
 * - max 5 emprunts par adhérent
 * - pas de nouvel emprunt si pénalités actives
 * - génération de pénalité en cas de retard au retour
 */
public class DefaultEmpruntService implements EmpruntService {

    // --- Constantes Métier ---
    public static final int MAX_EMPRUNTS_PAR_ADHERENT = 5;
    public static final int DUREE_EMPRUNT_SEMAINES = 3;

    // --- Dépendances (Repositories) ---
    private final EmpruntRepository empruntRepository;
    private final DocumentRepository documentRepository;
    private final AdherentRepository adherentRepository;
    private final PenaliteRepository penaliteRepository;

    public DefaultEmpruntService(EmpruntRepository empruntRepository,
                                 DocumentRepository documentRepository,
                                 AdherentRepository adherentRepository,
                                 PenaliteRepository penaliteRepository) {
        this.empruntRepository = Objects.requireNonNull(empruntRepository, "empruntRepository");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository");
        this.adherentRepository = Objects.requireNonNull(adherentRepository, "adherentRepository");
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository, "penaliteRepository");
    }

    @Override
    public Emprunt borrow(long adherentId, long documentId, LocalDate dateEmprunt) throws EmpruntException {
        Objects.requireNonNull(dateEmprunt, "dateEmprunt");

        Adherent adherent = adherentRepository.findById(adherentId)
                .orElseThrow(() -> new EmpruntException("Adhérent non trouvé (ID: " + adherentId + ")"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EmpruntException("Document non trouvé (ID: " + documentId + ")"));

        // 1. Disponibilité
        if (this.isDocumentEmprunte(documentId)) {
            throw new EmpruntException(
                    "Le document '" + document.getTitre() + "' est déjà en cours d'emprunt."
            );
        }

        // 2. Statut de l'adhérent (pénalités actives)
        List<Penalite> penalitesActives = penaliteRepository.findActivesByAdherentId(adherentId);
        if (!penalitesActives.isEmpty()) {
            throw new EmpruntException(
                    "L'adhérent a des pénalités non réglées (" + penalitesActives.size() + "). Emprunt impossible."
            );
        }

        // 3. Nombre maximal d'emprunts actifs
        List<Emprunt> empruntsActifsAdherent = empruntRepository.findActiveEmprunts().stream()
                .filter(e -> e.getEmprunteur() != null
                        && e.getEmprunteur().getId() != null
                        && e.getEmprunteur().getId().equals(adherentId))
                .toList();

        if (empruntsActifsAdherent.size() >= MAX_EMPRUNTS_PAR_ADHERENT) {
            throw new EmpruntException(
                    "L'adhérent a atteint le nombre maximal d'emprunts (" + MAX_EMPRUNTS_PAR_ADHERENT + ")."
            );
        }

        // 4. Dates
        LocalDate dateRetourPrevue = dateEmprunt.plusWeeks(DUREE_EMPRUNT_SEMAINES);

        // 5. Création + persistance
        Emprunt nouvelEmprunt = Emprunt.creer(document, adherent, dateEmprunt, dateRetourPrevue);
        return empruntRepository.save(nouvelEmprunt);
    }

    @Override
    public Penalite returnEmprunt(long empruntId, LocalDate dateRetour) throws EmpruntException {
        Objects.requireNonNull(dateRetour, "dateRetour");

        Emprunt emprunt = empruntRepository.findById(empruntId)
                .orElseThrow(() -> new EmpruntException("Emprunt non trouvé (ID: " + empruntId + ")"));

        if (!emprunt.estEnCours()) {
            throw new EmpruntException(
                    "Cet emprunt a déjà été retourné le " + emprunt.getDateRetourEffective()
            );
        }

        // 1. Enregistrer le retour
        emprunt.enregistrerRetour(dateRetour);
        empruntRepository.save(emprunt);

        // 2. Générer une pénalité éventuelle
        Money tarifParJour = Emprunt.PENALITE_PAR_JOUR_DEFAUT;

        return emprunt
                .genererPenaliteSiNecessaire(dateRetour, tarifParJour)
                .map(penaliteRepository::save)
                .orElse(null);
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
        Objects.requireNonNull(dateActuelle, "dateActuelle");
        return empruntRepository.findLateEmprunts(dateActuelle);
    }

    @Override
    public boolean isDocumentEmprunte(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return false;
        }
        return empruntRepository.isDocumentEmprunte(documentId);
    }

    /**
     * Tous les emprunts (actifs + historiques) pour un adhérent.
     */
    @Override
    public List<Emprunt> findByAdherentId(long adherentId) {
        return empruntRepository.findByAdherentId(adherentId);
    }

    /**
     * Uniquement les emprunts encore en cours pour un adhérent.
     */
    @Override
    public List<Emprunt> findActiveByAdherentId(long adherentId) {
        // Si tu as une méthode dédiée dans le repo, tu peux déléguer directement.
        // Sinon, on filtre en mémoire.
        return empruntRepository.findByAdherentId(adherentId).stream()
                .filter(Emprunt::estEnCours)
                .toList();
    }
}