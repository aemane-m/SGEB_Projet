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
 * Implémentation par défaut du service {@link EmpruntService}.
 *
 * <p>Ce service applique les règles métier pour :</p>
 * <ul>
 *     <li>emprunter un document,</li>
 *     <li>retourner un document,</li>
 *     <li>gérer les pénalités de retard,</li>
 *     <li>consulter les emprunts (tous, actifs, en retard, par adhérent).</li>
 * </ul>
 *
 * <p>Règles métier principales :</p>
 * <ul>
 *     <li>Un document ne peut être emprunté que s’il n’est pas déjà en cours d’emprunt.</li>
 *     <li>Un adhérent ne peut pas emprunter s’il a des pénalités actives.</li>
 *     <li>Un adhérent ne peut pas avoir plus de {@link #MAX_EMPRUNTS_PAR_ADHERENT} emprunts actifs.</li>
 *     <li>Une pénalité est générée automatiquement en cas de retard lors du retour.</li>
 * </ul>
 */
public class DefaultEmpruntService implements EmpruntService {

    // =========================================================
    // ==================== Règles métier =======================
    // =========================================================

    /** Nombre maximal d’emprunts simultanés autorisés pour un adhérent. */
    public static final int MAX_EMPRUNTS_PAR_ADHERENT = 5;

    /** Durée standard d’un emprunt, en semaines. */
    public static final int DUREE_EMPRUNT_SEMAINES = 3;

    // =========================================================
    // ======================= Dépendances ======================
    // =========================================================

    private final EmpruntRepository empruntRepository;
    private final DocumentRepository documentRepository;
    private final AdherentRepository adherentRepository;
    private final PenaliteRepository penaliteRepository;

    /**
     * Crée une instance de {@code DefaultEmpruntService}.
     *
     * @param empruntRepository   dépôt des emprunts
     * @param documentRepository  dépôt des documents
     * @param adherentRepository  dépôt des adhérents
     * @param penaliteRepository  dépôt des pénalités
     */
    public DefaultEmpruntService(EmpruntRepository empruntRepository,
                                 DocumentRepository documentRepository,
                                 AdherentRepository adherentRepository,
                                 PenaliteRepository penaliteRepository) {
        this.empruntRepository = Objects.requireNonNull(empruntRepository, "empruntRepository ne peut pas être null");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository ne peut pas être null");
        this.adherentRepository = Objects.requireNonNull(adherentRepository, "adherentRepository ne peut pas être null");
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository, "penaliteRepository ne peut pas être null");
    }

    // =========================================================
    // ==================== Use case : Emprunter ===============
    // =========================================================

    /**
     * Permet à un adhérent d’emprunter un document à une date donnée.
     *
     * <p>Cette méthode :</p>
     * <ol>
     *     <li>Vérifie que l’adhérent et le document existent,</li>
     *     <li>Vérifie que le document est disponible (non emprunté),</li>
     *     <li>Vérifie que l’adhérent n’a pas de pénalités actives,</li>
     *     <li>Vérifie que l’adhérent n’a pas dépassé le nombre maximum d’emprunts,</li>
     *     <li>Crée l’emprunt pour une durée standard de {@link #DUREE_EMPRUNT_SEMAINES} semaines,</li>
     *     <li>Sauvegarde et retourne le nouvel emprunt.</li>
     * </ol>
     *
     * @param adherentId  identifiant de l’adhérent
     * @param documentId  identifiant du document
     * @param dateEmprunt date de l’emprunt
     * @return l’emprunt créé et sauvegardé
     * @throws EmpruntException si une règle métier n’est pas respectée
     */
    @Override
    public Emprunt borrow(long adherentId, long documentId, LocalDate dateEmprunt) throws EmpruntException {
        Objects.requireNonNull(dateEmprunt, "dateEmprunt ne peut pas être null");

        // 1. Récupération des entités
        Adherent adherent = adherentRepository.findById(adherentId)
                .orElseThrow(() -> new EmpruntException("Adhérent non trouvé (ID: " + adherentId + ")"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EmpruntException("Document non trouvé (ID: " + documentId + ")"));

        // 2. Le document est-il déjà emprunté ?
        if (isDocumentEmprunte(documentId)) {
            throw new EmpruntException(
                    "Le document '" + document.getTitre() + "' est déjà en cours d'emprunt."
            );
        }

        // 3. L’adhérent a-t-il des pénalités actives ?
        List<Penalite> penalitesActives = penaliteRepository.findActivesByAdherentId(adherentId);
        if (!penalitesActives.isEmpty()) {
            throw new EmpruntException(
                    "L'adhérent a des pénalités non réglées (" + penalitesActives.size() + "). Emprunt impossible."
            );
        }

        // 4. L’adhérent a-t-il déjà trop d’emprunts actifs ?
        List<Emprunt> empruntsActifsAdherent = findActiveLoansForAdherent(adherentId);
        if (empruntsActifsAdherent.size() >= MAX_EMPRUNTS_PAR_ADHERENT) {
            throw new EmpruntException(
                    "L'adhérent a atteint le nombre maximal d'emprunts (" +
                            MAX_EMPRUNTS_PAR_ADHERENT + ")."
            );
        }

        // 5. Calcul de la date de retour prévue
        LocalDate dateRetourPrevue = dateEmprunt.plusWeeks(DUREE_EMPRUNT_SEMAINES);

        // 6. Création + persistance de l’emprunt
        Emprunt nouvelEmprunt = Emprunt.creer(document, adherent, dateEmprunt, dateRetourPrevue);
        return empruntRepository.save(nouvelEmprunt);
    }

    // =========================================================
    // =================== Use case : Retourner =================
    // =========================================================

    /**
     * Enregistre le retour d’un emprunt et génère une pénalité si nécessaire.
     *
     * <p>Étapes :</p>
     * <ol>
     *     <li>Vérifie que l’emprunt existe,</li>
     *     <li>Vérifie que l’emprunt est encore en cours,</li>
     *     <li>Clôture l’emprunt à la date de retour,</li>
     *     <li>Calcule la pénalité (si retard) avec le tarif par jour par défaut,</li>
     *     <li>Crée et sauvegarde la pénalité si besoin.</li>
     * </ol>
     *
     * @param empruntId  identifiant de l’emprunt à retourner
     * @param dateRetour date effective de retour
     * @return la pénalité générée, ou {@code null} si aucune pénalité
     * @throws EmpruntException si l’emprunt est introuvable ou déjà retourné
     */
    @Override
    public Penalite returnEmprunt(long empruntId, LocalDate dateRetour) throws EmpruntException {
        Objects.requireNonNull(dateRetour, "dateRetour ne peut pas être null");

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

        // 2. Générer une éventuelle pénalité
        Money tarifParJour = Emprunt.PENALITE_PAR_JOUR_DEFAUT;

        return emprunt
                .genererPenaliteSiNecessaire(dateRetour, tarifParJour)
                .map(penaliteRepository::save)
                .orElse(null);
    }

    // =========================================================
    // ==================== Méthodes de lecture =================
    // =========================================================

    /**
     * Retourne tous les emprunts (actifs + historiques).
     */
    @Override
    public List<Emprunt> findAll() {
        return empruntRepository.findAll();
    }

    /**
     * Retourne uniquement les emprunts actuellement en cours.
     */
    @Override
    public List<Emprunt> findActive() {
        return empruntRepository.findActiveEmprunts();
    }

    /**
     * Retourne les emprunts en retard à une date donnée.
     *
     * @param dateActuelle date de référence pour le calcul du retard
     */
    @Override
    public List<Emprunt> findLate(LocalDate dateActuelle) {
        Objects.requireNonNull(dateActuelle, "dateActuelle ne peut pas être null");
        return empruntRepository.findLateEmprunts(dateActuelle);
    }

    /**
     * Indique si un document est actuellement emprunté.
     *
     * @param documentId identifiant du document
     * @return {@code true} si le document a un emprunt en cours
     */
    @Override
    public boolean isDocumentEmprunte(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return false;
        }
        return empruntRepository.isDocumentEmprunte(documentId);
    }

    /**
     * Retourne tous les emprunts (actifs + historiques) d’un adhérent.
     *
     * @param adherentId identifiant de l’adhérent
     */
    @Override
    public List<Emprunt> findByAdherentId(long adherentId) {
        return empruntRepository.findByAdherentId(adherentId);
    }

    /**
     * Retourne uniquement les emprunts encore en cours pour un adhérent.
     *
     * @param adherentId identifiant de l’adhérent
     */
    @Override
    public List<Emprunt> findActiveByAdherentId(long adherentId) {
        // Si le dépôt ne fournit pas de méthode dédiée, on filtre en mémoire.
        return empruntRepository.findByAdherentId(adherentId).stream()
                .filter(Emprunt::estEnCours)
                .toList();
    }

    // =========================================================
    // ================== Méthodes utilitaires =================
    // =========================================================

    /**
     * Retourne la liste des emprunts en cours pour un adhérent donné.
     * Utilisée dans la logique d’emprunt pour contrôler le quota.
     */
    private List<Emprunt> findActiveLoansForAdherent(long adherentId) {
        return empruntRepository.findActiveEmprunts().stream()
                .filter(e -> e.getEmprunteur() != null
                        && e.getEmprunteur().getId() != null
                        && e.getEmprunteur().getId().equals(adherentId))
                .toList();
    }
}