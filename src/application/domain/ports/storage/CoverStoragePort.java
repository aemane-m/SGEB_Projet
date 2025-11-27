package application.domain.ports.storage;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contrat pour gérer le stockage physique des images en local.
 * <p>
 * Ce port est spécialisé dans l'importation d'images depuis l'extérieur
 * (Internet ou un fichier local de l'ordinateur de l'utilisateur) vers notre bibliothèque.
 * </p>
 */
public interface CoverStoragePort {

    /**
     * Télécharge une image depuis Internet et la sauvegarde dans la bibliothèque.
     *
     * @param url L'adresse web de l'image (ex: "https://amazon.com/img.jpg").
     * @param nomSuggere Le nom qu'on aimerait donner au fichier (souvent l'ISBN).
     * @return Les infos du fichier sauvegardé (DTO).
     * @throws IOException Si ça rate (pas d'internet, disque plein...).
     */
    SavedCover saveCoverFromUrl(String url, String nomSuggere) throws IOException;

    /**
     * Copie une image depuis l'ordinateur de l'utilisateur vers la bibliothèque.
     *
     * @param fichierSource Le chemin du fichier original (ex: "C:/Users/Moi/Desktop/image.png").
     * @param nomSuggere Le nom qu'on aimerait donner au fichier.
     * @return Les infos du fichier sauvegardé (DTO).
     * @throws IOException Si ça rate (fichier introuvable, disque plein...).
     */
    SavedCover saveCoverFromLocalFile(Path fichierSource, String nomSuggere) throws IOException;

}
