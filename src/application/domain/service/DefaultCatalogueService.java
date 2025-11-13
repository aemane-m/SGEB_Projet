package application.domain.service;

import application.domain.core.document.Document;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.storage.CoverStoragePort;
import application.domain.ports.storage.SavedCover;

import java.io.IOException;
import java.nio.file.Path; // CORRIGÉ : Import correct
import java.util.*;

/**
 * Implémentation complète du CatalogueService.
 * Gère CRUD, recherche, et logique de couverture (URL et fichier local).
 */
public class DefaultCatalogueService implements CatalogueService {

    // --- Champs (Déclarés une seule fois) ---
    private final DocumentRepository documentRepository;
    private final EmpruntRepository empruntRepository;
    private final CoverStoragePort coverStorage;

    /**
     * Constructeur principal (Déclaré une seule fois).
     */
    public DefaultCatalogueService(DocumentRepository documentRepository,
                                   EmpruntRepository empruntRepository,
                                   CoverStoragePort coverStorage) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository ne peut être null");
        this.empruntRepository = Objects.requireNonNull(empruntRepository, "empruntRepository ne peut être null");
        this.coverStorage = coverStorage; // Peut être null si non configuré
    }

    // --- CRUD avec URL ---

    @Override // CORRIGÉ : Ajout @Override
    public Document create(Document doc, String coverUrlToDownload) {
        validateIsbnUniqueness(doc, null);
        handleCoverDownload(doc, coverUrlToDownload);
        return documentRepository.save(doc);
    }

    @Override // CORRIGÉ : Ajout @Override
    public Document update(Document doc, String coverUrlToDownload) {
        Document existing = findExistingOrThrow(doc);
        validateIsbnUniqueness(doc, existing.getId());
        handleCoverDownload(doc, coverUrlToDownload);
        return documentRepository.save(doc);
    }

    // --- CRUD avec fichier local ---

    @Override // CORRIGÉ : Ajout @Override
    public Document createWithLocalCover(Document doc, Path localCoverPath) throws IOException {
        validateIsbnUniqueness(doc, null);
        handleLocalCoverSave(doc, localCoverPath);
        return documentRepository.save(doc);
    }

    @Override // CORRIGÉ : Ajout @Override
    public Document updateWithLocalCover(Document doc, Path localCoverPath) throws IOException {
        Document existing = findExistingOrThrow(doc);
        validateIsbnUniqueness(doc, existing.getId());
        handleLocalCoverSave(doc, localCoverPath);
        return documentRepository.save(doc);
    }

    // --- Logique privée ---

    /** Valide l'unicité de l'ISBN avant création/MAJ. */
    private void validateIsbnUniqueness(Document doc, Long currentDocId) {
        if (doc.getIsbn() != null && !doc.getIsbn().isBlank()) {
            Optional<Document> matchIsbn = documentRepository.findByIsbn(doc.getIsbn());
            if (matchIsbn.isPresent() && !matchIsbn.get().getId().equals(currentDocId)) {
                throw new RuntimeException("Un autre document avec cet ISBN ("+doc.getIsbn()+") existe déjà.");
            }
        }
    }

    /** Vérifie si le document existe pour une mise à jour. */
    private Document findExistingOrThrow(Document doc) {
         if (doc.getId() == null) {
            throw new RuntimeException("ID manquant, mise à jour impossible.");
        }
        return documentRepository.findById(doc.getId())
                .orElseThrow(() -> new RuntimeException("Document non trouvé (id: " + doc.getId() + "), mise à jour impossible."));
    }

    /** Gère le téléchargement et l'attachement d'une couverture depuis une URL. */
    private void handleCoverDownload(Document doc, String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank() || coverStorage == null) return;
        try {
            SavedCover savedCover = coverStorage.saveCoverFromUrl(coverUrl, doc.getIsbn());
            attachCoverInfo(doc, savedCover);
        } catch (IOException e) {
            System.err.println("DefaultCatalogueService: Échec téléchargement couverture URL, doc sauvegardé sans. Erreur: " + e.getMessage());
        }
    }

    /** Gère la copie et l'attachement d'une couverture depuis un fichier local. */
    private void handleLocalCoverSave(Document doc, Path localCoverPath) throws IOException {
        // CORRIGÉ : Utilisation de java.nio.file.Path
        if (localCoverPath == null || coverStorage == null) return;
        try {
            SavedCover savedCover = coverStorage.saveCoverFromLocalFile(localCoverPath, doc.getIsbn());
            attachCoverInfo(doc, savedCover);
        } catch (IOException e) {
             System.err.println("DefaultCatalogueService: Échec copie couverture locale. Erreur: " + e.getMessage());
             throw e;
        }
    }

    /** Met à jour les champs de couverture sur l'objet Document. */
    private void attachCoverInfo(Document doc, SavedCover savedCover) {
        if (savedCover != null) {
            doc.setCoverRef(savedCover.relativePath());
            doc.setCoverMime(savedCover.mimeType());
            doc.setCoverUpdatedAt(savedCover.updatedAt());
            System.out.println("Info couverture attachée: " + savedCover.relativePath());
        }
    }

    // --- Anciennes méthodes (pour compatibilité, sans @Override) ---

    public Document create(Document doc) {
        return create(doc, null);
    }

    public Document update(Document doc) {
        return update(doc, null);
    }

    // --- Autres méthodes du service (avec @Override) ---

    @Override
    public List<Document> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        Set<Document> results = new HashSet<>();
        results.addAll(documentRepository.findByTitreContaining(query));
        results.addAll(documentRepository.findByAuteurContaining(query));
        documentRepository.findByIsbn(query).ifPresent(results::add);
        try {
            long id = Long.parseLong(query.trim());
            documentRepository.findById(id).ifPresent(results::add);
        } catch (NumberFormatException e) { /* ignore */ }
        return new ArrayList<>(results);
    }

    @Override
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    @Override
    public void delete(long id) {
        if (!canDelete(id)) {
            throw new RuntimeException("Suppression impossible : le document (ID: " + id + ") est actuellement emprunté.");
        }
        // Pourrait ajouter la suppression du fichier image ici si nécessaire
        documentRepository.delete(id);
    }

    @Override
    public boolean canDelete(long id) {
        if (documentRepository.findById(id).isEmpty()) {
            return false;
        }
        return !empruntRepository.isDocumentEmprunte(id);
    }
}

