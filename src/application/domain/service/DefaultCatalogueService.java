package application.domain.service;

import application.domain.core.document.Document;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.storage.CoverStoragePort;
import application.domain.ports.storage.SavedCover;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Implémentation par défaut du {@link CatalogueService}.
 *
 * <p>Ce service gère :</p>
 * <ul>
 *     <li>le CRUD des documents (création, mise à jour, suppression, recherche),</li>
 *     <li>le contrôle d’unicité de l’ISBN,</li>
 *     <li>la gestion des couvertures (depuis une URL ou un fichier local),</li>
 *     <li>le contrôle métier avant suppression (document non emprunté).</li>
 * </ul>
 *
 * <p>Il utilise uniquement des ports (interfaces) pour accéder :</p>
 * <ul>
 *     <li>aux documents ({@link DocumentRepository})</li>
 *     <li>aux emprunts ({@link EmpruntRepository})</li>
 *     <li>au stockage d’images ({@link CoverStoragePort})</li>
 * </ul>
 */
public class DefaultCatalogueService implements CatalogueService {

    // =========================================================
    // ===================== Dépendances ========================
    // =========================================================

    /** Accès aux documents (persistance). */
    private final DocumentRepository documentRepository;

    /** Accès aux emprunts (permet de savoir si un document est emprunté). */
    private final EmpruntRepository empruntRepository;

    /** Gestion des fichiers de couverture (peut être null si non configuré). */
    private final CoverStoragePort coverStorage;

    /**
     * Constructeur principal.
     *
     * @param documentRepository dépôt des documents (non null)
     * @param empruntRepository  dépôt des emprunts (non null)
     * @param coverStorage       service de stockage des couvertures (peut être null)
     */
    public DefaultCatalogueService(DocumentRepository documentRepository,
                                   EmpruntRepository empruntRepository,
                                   CoverStoragePort coverStorage) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository ne peut pas être null");
        this.empruntRepository = Objects.requireNonNull(empruntRepository, "empruntRepository ne peut pas être null");
        this.coverStorage = coverStorage; // peut être null si tu ne gères pas encore les couvertures
    }

    // =========================================================
    // ====================== Consultation ======================
    // =========================================================

    @Override
    public List<Document> findAll() {
        // Retourne simplement tous les documents du dépôt.
        return documentRepository.findAll();
    }

    @Override
    public List<Document> search(String query) {
        // Si la requête est vide ou null → on renvoie tout.
        if (query == null || query.isBlank()) {
            return findAll();
        }

        // On utilise un Set pour éviter les doublons (titre, auteur, ISBN, ID peuvent se recouper).
        Set<Document> results = new LinkedHashSet<>();

        // Recherche partielle sur le titre et l'auteur
        results.addAll(documentRepository.findByTitreContaining(query));
        results.addAll(documentRepository.findByAuteurContaining(query));

        // Recherche exacte par ISBN
        documentRepository.findByIsbn(query).ifPresent(results::add);

        // Si la requête ressemble à un ID numérique, on tente aussi par identifiant technique.
        try {
            long id = Long.parseLong(query.trim());
            documentRepository.findById(id).ifPresent(results::add);
        } catch (NumberFormatException ignored) {
            // Si ce n'est pas un nombre, on ignore simplement.
        }

        return new ArrayList<>(results);
    }

    // =========================================================
    // ======================= Création =========================
    // =========================================================

    /**
     * Crée un document en optionnellement téléchargeant une couverture depuis une URL.
     */
    @Override
    public Document create(Document doc, String coverUrlToDownload) {
        // Vérifier qu'il n'existe pas déjà un autre document avec le même ISBN.
        validateIsbnUniqueness(doc, null);

        // Télécharger et attacher la couverture si une URL est fournie.
        handleCoverDownload(doc, coverUrlToDownload);

        // Sauvegarde en base et retour du document (avec ID).
        return documentRepository.save(doc);
    }

    /**
     * Crée un document en copiant une couverture depuis un fichier local.
     */
    @Override
    public Document createWithLocalCover(Document doc, Path localCoverPath) throws IOException {
        validateIsbnUniqueness(doc, null);
        handleLocalCoverSave(doc, localCoverPath);
        return documentRepository.save(doc);
    }

    // =========================================================
    // ======================= Mise à jour ======================
    // =========================================================

    /**
     * Met à jour un document et, si une URL est fournie, télécharge une nouvelle couverture.
     */
    @Override
    public Document update(Document doc, String coverUrlToDownload) {
        // On s'assure d'abord que le document à mettre à jour existe.
        Document existing = findExistingOrThrow(doc);

        // On vérifie que l'ISBN n'entre pas en conflit avec un autre document.
        validateIsbnUniqueness(doc, existing.getId());

        // On gère éventuellement le téléchargement de la couverture.
        handleCoverDownload(doc, coverUrlToDownload);

        return documentRepository.save(doc);
    }

    /**
     * Met à jour un document en remplaçant sa couverture par un fichier local.
     */
    @Override
    public Document updateWithLocalCover(Document doc, Path localCoverPath) throws IOException {
        Document existing = findExistingOrThrow(doc);
        validateIsbnUniqueness(doc, existing.getId());
        handleLocalCoverSave(doc, localCoverPath);
        return documentRepository.save(doc);
    }

    // =========================================================
    // ======================= Suppression ======================
    // =========================================================

    @Override
    public boolean canDelete(long id) {
        // Si le document n'existe pas, on considère qu'il n'est pas supprimable.
        if (documentRepository.findById(id).isEmpty()) {
            return false;
        }

        // On ne supprime pas un document actuellement emprunté.
        return !empruntRepository.isDocumentEmprunte(id);
    }

    @Override
    public void delete(long id) {
        if (!canDelete(id)) {
            throw new RuntimeException(
                    "Suppression impossible : le document (ID: " + id + ") est actuellement emprunté."
            );
        }
        documentRepository.delete(id);
    }

    // =========================================================
    // ============ Méthodes utilitaires privées ===============
    // =========================================================

    /**
     * Vérifie qu'aucun autre document ne possède le même ISBN.
     *
     * @param doc           document courant
     * @param currentDocId  ID du document en cours de modification (null en création)
     */
    private void validateIsbnUniqueness(Document doc, Long currentDocId) {
        String isbn = doc.getIsbn();
        if (isbn == null || isbn.isBlank()) {
            return; // Rien à vérifier si pas d'ISBN
        }

        documentRepository.findByIsbn(isbn).ifPresent(existing -> {
            // Si on trouve un document avec le même ISBN mais un ID différent → erreur.
            if (!existing.getId().equals(currentDocId)) {
                throw new RuntimeException(
                        "Un autre document avec cet ISBN (" + isbn + ") existe déjà."
                );
            }
        });
    }

    /**
     * Récupère le document existant correspondant à l'ID du document fourni.
     * Lance une exception si :
     * <ul>
     *     <li>l'ID est null</li>
     *     <li>aucun document n'est trouvé en base</li>
     * </ul>
     */
    private Document findExistingOrThrow(Document doc) {
        if (doc.getId() == null) {
            throw new RuntimeException("ID manquant, mise à jour impossible.");
        }
        return documentRepository.findById(doc.getId())
                .orElseThrow(() ->
                        new RuntimeException("Document non trouvé (id: " + doc.getId() + "), mise à jour impossible.")
                );
    }

    /**
     * Télécharge une couverture depuis une URL et met à jour le document
     * si le stockage de couvertures est configuré.
     */
    private void handleCoverDownload(Document doc, String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank() || coverStorage == null) {
            return; // Rien à faire
        }

        try {
            SavedCover savedCover = coverStorage.saveCoverFromUrl(coverUrl, doc.getIsbn());
            attachCoverInfo(doc, savedCover);
        } catch (IOException e) {
            // Ici on loggue simplement l'erreur et on continue sans couverture.
            System.err.println(
                    "DefaultCatalogueService: échec du téléchargement de la couverture (" +
                    coverUrl + "). Le document sera sauvegardé sans. Erreur : " + e.getMessage()
            );
        }
    }

    /**
     * Sauvegarde une couverture à partir d’un fichier local et met à jour le document.
     *
     * @throws IOException si la copie échoue
     */
    private void handleLocalCoverSave(Document doc, Path localCoverPath) throws IOException {
        if (localCoverPath == null || coverStorage == null) {
            return; // Rien à faire
        }

        try {
            SavedCover savedCover = coverStorage.saveCoverFromLocalFile(localCoverPath, doc.getIsbn());
            attachCoverInfo(doc, savedCover);
        } catch (IOException e) {
            System.err.println(
                    "DefaultCatalogueService: échec de la copie de la couverture locale (" +
                    localCoverPath + "). Erreur : " + e.getMessage()
            );
            // On relaie l’exception pour que l’appelant sache que l’opération a échoué.
            throw e;
        }
    }

    /**
     * Copie les informations de la couverture sauvegardée dans le document :
     * référence du fichier, type MIME, date de mise à jour.
     */
    private void attachCoverInfo(Document doc, SavedCover savedCover) {
        if (savedCover == null) {
            return;
        }
        doc.setCoverRef(savedCover.relativePath());
        doc.setCoverMime(savedCover.mimeType());
        doc.setCoverUpdatedAt(savedCover.updatedAt());

        System.out.println("Couverture attachée au document : " + savedCover.relativePath());
    }

    // =========================================================
    // =========== Surcharges pratiques (sans URL) =============
    // =========================================================

    /**
     * Création sans couverture distante.
     */
    public Document create(Document doc) {
        return create(doc, null);
    }

    /**
     *  Mise à jour sans couverture distante.
     */
    public Document update(Document doc) {
        return update(doc, null);
    }
}