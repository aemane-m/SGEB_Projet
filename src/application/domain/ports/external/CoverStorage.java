package application.domain.ports.external;

import java.io.InputStream;
import java.util.Optional;

/**
 * <p>
 * Port pour la gestion du stockage des images de couverture
 * (en local, sur un serveur distant, ou en cloud).
 * </p>
 *
 * <p>
 * Le domaine ne connaît ni le système de fichiers ni le cloud :
 * il ne manipule que des flux et des références opaques.
 * </p>
 */
public interface CoverStorage {

    /**
     * Sauvegarde une image et retourne une référence opaque au fichier stocké.
     *
     * @param isbn        ISBN du document
     * @param coverStream flux de l’image
     * @param mimeType    type MIME (ex : "image/jpeg")
     * @return référence de stockage (nom de fichier, clé, ID…)
     */
    String saveCover(String isbn, InputStream coverStream, String mimeType);

    /**
     * Charge une image sauvegardée.
     *
     * @param coverRef référence du fichier
     * @return un flux si le fichier existe
     */
    Optional<InputStream> loadCover(String coverRef);

    /**
     * Supprime une image de couverture du stockage.
     *
     * @param coverRef référence du fichier
     * @return {@code true} si la suppression a réussi
     */
    boolean deleteCover(String coverRef);
}