package application.domain.ports.storage;

import java.io.IOException;
import java.nio.file.Path; // Ajouté

/**
 * Port (interface) pour la sauvegarde physique des fichiers de couverture.
 */
public interface CoverStoragePort {

    /**
     * Télécharge une image depuis une URL et la sauvegarde.
     * @param url URL de l'image source.
     * @param suggestedName Nom suggéré (ex: ISBN), peut être null.
     * @return Métadonnées du fichier sauvegardé, ou null si échec.
     * @throws IOException Si une erreur réseau ou disque survient.
     */
    SavedCover saveCoverFromUrl(String url, String suggestedName) throws IOException;

    /**
     * Copie un fichier image local vers le stockage.
     * @param sourcePath Chemin absolu du fichier source local.
     * @param suggestedName Nom suggéré (ex: ISBN), peut être null.
     * @return Métadonnées du fichier sauvegardé, ou null si échec.
     * @throws IOException Si une erreur de lecture/écriture survient.
     */
    SavedCover saveCoverFromLocalFile(Path sourcePath, String suggestedName) throws IOException;

}

