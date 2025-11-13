package application.domain.service;

import application.domain.core.document.Document;
import java.io.IOException; // Ajouté
import java.nio.file.Path; // Ajouté
import java.util.List;

/**
 * Service métier pour la gestion du catalogue.
 */
public interface CatalogueService {
    List<Document> findAll();
    List<Document> search(String query);

    /** Crée un document, téléchargeant potentiellement la couverture depuis une URL. */
    Document create(Document doc, String coverUrlToDownload);

    /** Met à jour un document, téléchargeant potentiellement la couverture depuis une URL. */
    Document update(Document doc, String coverUrlToDownload);

    /** Crée un document, copiant la couverture depuis un fichier local. */
    Document createWithLocalCover(Document doc, Path localCoverPath) throws IOException;

    /** Met à jour un document, copiant la couverture depuis un fichier local. */
    Document updateWithLocalCover(Document doc, Path localCoverPath) throws IOException;

    boolean canDelete(long id);
    void delete(long id);
}

