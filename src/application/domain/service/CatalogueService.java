package application.domain.service;

import application.domain.core.document.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * <p>Service métier permettant de gérer le catalogue de documents
 * (livres, CD, magazines...).</p>
 *
 * <p>Ce service ne dépend d'aucune technologie particulière :
 * ni JavaFX, ni SQLite, ni HTTP.  
 * Il définit simplement les actions possibles côté domaine.</p>
 *
 * <h3>Fonctionnalités principales :</h3>
 * <ul>
 *     <li>Rechercher des documents</li>
 *     <li>Créer / mettre à jour un document</li>
 *     <li>Gérer les couvertures (URL ou fichier local)</li>
 *     <li>Supprimer un document lorsque c’est autorisé</li>
 * </ul>
 */
public interface CatalogueService {

    // =========================================================
    // ===================== Consultation =======================
    // =========================================================

    /**
     * Retourne tous les documents du catalogue.
     */
    List<Document> findAll();

    /**
     * Recherche les documents dont le titre ou l'auteur contient la requête.
     * @param query texte à rechercher (titre, auteur…)
     */
    List<Document> search(String query);


    // =========================================================
    // ===================== Création ===========================
    // =========================================================

    /**
     * Crée un nouveau document dans le catalogue.
     *
     * @param doc                 document à créer (métadonnées)
     * @param coverUrlToDownload  URL d’une image de couverture (optionnelle).
     *                            Si null, aucune couverture n'est téléchargée.
     * @return document créé (avec ID rempli)
     */
    Document create(Document doc, String coverUrlToDownload);

    /**
     * Crée un document en copiant une image de couverture depuis un fichier local.
     *
     * @param doc            document à créer
     * @param localCoverPath chemin vers l’image locale
     * @return document créé
     * @throws IOException si l’image ne peut pas être copiée
     */
    Document createWithLocalCover(Document doc, Path localCoverPath) throws IOException;


    // =========================================================
    // ===================== Mise à jour ========================
    // =========================================================

    /**
     * Met à jour un document existant.
     *
     * @param doc                 document mis à jour
     * @param coverUrlToDownload  URL éventuelle d’une nouvelle couverture
     * @return document mis à jour
     */
    Document update(Document doc, String coverUrlToDownload);

    /**
     * Met à jour un document avec une nouvelle couverture locale.
     *
     * @param doc            document mis à jour
     * @param localCoverPath nouvelle image locale
     * @return document mis à jour
     * @throws IOException si la copie échoue
     */
    Document updateWithLocalCover(Document doc, Path localCoverPath) throws IOException;


    // =========================================================
    // ===================== Suppression ========================
    // =========================================================

    /**
     * Indique si un document peut être supprimé
     * (ex : il n’est pas emprunté en ce moment).
     *
     * @param id identifiant du document
     */
    boolean canDelete(long id);

    /**
     * Supprime un document du catalogue.
     * @param id identifiant du document
     */
    void delete(long id);
}