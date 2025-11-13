package application.domain.ports.external;

import java.io.InputStream;
import java.util.Optional;

/**
 * Port pour la gestion des fichiers de couverture (images) sur le stockage local.
 */
public interface CoverStorage {

    /**
     * Sauvegarde une image de couverture et retourne la référence du fichier stocké.
     * @param isbn Identifiant du document
     * @param coverStream Flux d'entrée de l'image
     * @param mimeType Type MIME (ex: "image/jpeg")
     * @return Référence du fichier stocké (ex: nom ou chemin relatif)
     */
    String saveCover(String isbn, InputStream coverStream, String mimeType);

    /**
     * Récupère une couverture stockée localement.
     * @param coverRef Référence du fichier
     * @return Flux d’entrée de l’image si présente
     */
    Optional<InputStream> loadCover(String coverRef);

    /**
     * Supprime une couverture du stockage.
     * @param coverRef Référence du fichier à supprimer
     * @return true si supprimée avec succès
     */
    boolean deleteCover(String coverRef);
}
