package application.infrastructure.external;

import application.domain.ports.storage.CoverStoragePort;
import application.domain.ports.storage.SavedCover;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

/**
 * Adaptateur d'infrastructure qui enregistre les couvertures (images)
 * sur le disque local.
 *
 * Il sait :
 * <ul>
 *     <li>télécharger une image à partir d'une URL,</li>
 *     <li>copier une image depuis un fichier local,</li>
 *     <li>générer un objet {@link SavedCover} contenant les métadonnées.</li>
 * </ul>
 */
public class LocalCoverStorageAdapter implements CoverStoragePort {

    /** Dossier où seront stockées les couvertures. */
    private final Path storagePath;

    /** Client HTTP utilisé pour télécharger les images. */
    private final HttpClient httpClient;

    /**
     * Crée un adaptateur avec un chemin de stockage explicite.
     *
     * @param storagePathStr chemin du dossier où stocker les images (ex: "./covers").
     */
    public LocalCoverStorageAdapter(String storagePathStr) {
        this.storagePath = Paths.get(storagePathStr);

        try {
            if (!Files.exists(this.storagePath)) {
                Files.createDirectories(this.storagePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier de stockage des couvertures : " + storagePathStr, e);
        }

        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Constructeur par défaut.
     * Utilise le dossier "./covers" comme stockage.
     */
    public LocalCoverStorageAdapter() {
        this("./covers");
    }

    // =====================================================================
    // == Téléchargement depuis une URL ==
    // =====================================================================

    @Override
    public SavedCover saveCoverFromUrl(String url, String suggestedName) throws IOException {
        if (url == null || url.isBlank()) {
            return null;
        }

        Path tempFile = null;

        try {
            // 1. Crée un fichier temporaire
            tempFile = Files.createTempFile("cover-url-", ".tmp");

            // 2. Prépare et envoie la requête HTTP
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<Path> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofFile(tempFile)
            );

            // 3. Si le statut n'est pas 200, on abandonne
            if (response.statusCode() != 200) {
                System.err.println("Échec du téléchargement de la couverture (status: "
                        + response.statusCode() + ") URL: " + url);
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
                return null;
            }

            // 4. Finalise la sauvegarde dans le dossier définitif
            return finalizeSave(tempFile, suggestedName);

        } catch (Exception e) {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
            System.err.println("Erreur lors du téléchargement URL " + url + " : " + e.getMessage());
            throw new IOException("Erreur de téléchargement de la couverture", e);
        }
    }

    // =====================================================================
    // == Copie depuis un fichier local ==
    // =====================================================================

    @Override
    public SavedCover saveCoverFromLocalFile(Path sourcePath, String suggestedName) throws IOException {
        if (sourcePath == null || !Files.exists(sourcePath) || !Files.isReadable(sourcePath)) {
            throw new IOException("Fichier source invalide ou illisible : " + sourcePath);
        }

        Path tempFile = null;

        try {
            // 1. Crée un fichier temporaire
            tempFile = Files.createTempFile("cover-local-", ".tmp");

            // 2. Copie la source vers le temporaire
            Files.copy(sourcePath, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 3. Finalise la sauvegarde dans le dossier définitif
            return finalizeSave(tempFile, suggestedName);

        } catch (IOException e) {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
            System.err.println("Erreur lors de la copie de la couverture locale " + sourcePath + " : " + e.getMessage());
            throw e;
        }
    }

    // =====================================================================
    // == Méthode commune de finalisation ==
    // =====================================================================

    /**
     * Détermine le type et le nom de fichier final, déplace le fichier temporaire
     * vers le dossier de stockage, et construit un {@link SavedCover}.
     *
     * @param tempFile      fichier temporaire à déplacer
     * @param suggestedName nom suggéré (non utilisé ici mais gardé pour évolution)
     * @return un objet SavedCover avec les métadonnées
     */
    private SavedCover finalizeSave(Path tempFile, String suggestedName) throws IOException {
        try {
            // Type MIME (ex: image/jpeg, image/png, etc.)
            String mimeType = Files.probeContentType(tempFile);
            if (mimeType == null || !mimeType.startsWith("image/")) {
                mimeType = "image/jpeg"; // Valeur de secours
            }

            // Choix de l’extension en fonction du type mime
            String extension;
            if ("image/png".equals(mimeType)) {
                extension = ".png";
            } else if ("image/gif".equals(mimeType)) {
                extension = ".gif";
            } else {
                extension = ".jpg";
            }

            // Nom de fichier unique
            String finalName = UUID.randomUUID().toString() + extension;
            Path finalPath = storagePath.resolve(finalName);

            // Déplacement du fichier temporaire vers le fichier final
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);

            // Chemin relatif (par ex. "covers/abc123.jpg")
            String relativePath = storagePath.getFileName().toString() + "/" + finalName;

            // Création de l’objet SavedCover
            return new SavedCover(
                    finalName,
                    relativePath,
                    mimeType,
                    Files.size(finalPath),
                    Instant.now().toEpochMilli()
            );
        } finally {
            // Nettoyage du fichier temporaire s’il existe encore
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}