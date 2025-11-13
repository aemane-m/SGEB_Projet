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
 * Implémentation du port de stockage pour fichiers locaux.
 * Gère le téléchargement (URL) et la copie (local).
 */
public class LocalCoverStorageAdapter implements CoverStoragePort {

    private final Path storagePath;
    private final HttpClient http;

    public LocalCoverStorageAdapter(String storagePathStr) {
        this.storagePath = Paths.get(storagePathStr);
        try {
            if (!Files.exists(this.storagePath)) {
                Files.createDirectories(this.storagePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier de stockage", e);
        }
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    public LocalCoverStorageAdapter() { this("./covers"); }

    @Override
    public SavedCover saveCoverFromUrl(String url, String suggestedName) throws IOException {
        if (url == null || url.isBlank()) return null;
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("cover-url-", ".tmp");
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<Path> res = http.send(req, HttpResponse.BodyHandlers.ofFile(tempFile));

            if (res.statusCode() != 200) {
                System.err.println("DL Image échec (status: " + res.statusCode() + ") URL: " + url);
                Files.deleteIfExists(tempFile); // Nettoyage
                return null;
            }
            // Utilise la méthode commune pour finaliser la sauvegarde
            return finalizeSave(tempFile, suggestedName);

        } catch (Exception e) {
            Files.deleteIfExists(tempFile); // Nettoyage
            System.err.println("Erreur téléchargement URL " + url + ": " + e.getMessage());
            throw new IOException("Erreur téléchargement: " + e.getMessage(), e);
        }
    }

    /** NOUVELLE MÉTHODE */
    @Override
    public SavedCover saveCoverFromLocalFile(Path sourcePath, String suggestedName) throws IOException {
        if (sourcePath == null || !Files.exists(sourcePath) || !Files.isReadable(sourcePath)) {
             throw new IOException("Fichier source invalide ou illisible: " + sourcePath);
        }
        Path tempFile = null;
        try {
            // Crée un fichier temporaire pour standardiser le process
             tempFile = Files.createTempFile("cover-local-", ".tmp");
             // Copie le fichier source vers le temporaire
             Files.copy(sourcePath, tempFile, StandardCopyOption.REPLACE_EXISTING);
             // Utilise la méthode commune pour finaliser
             return finalizeSave(tempFile, suggestedName);

        } catch (IOException e) {
             Files.deleteIfExists(tempFile); // Nettoyage
             System.err.println("Erreur copie fichier local " + sourcePath + ": " + e.getMessage());
             throw e; // Relance l'exception
        }
    }

    /**
     * Méthode privée commune pour déterminer le nom final, déplacer
     * le fichier temporaire et créer l'objet SavedCover.
     */
    private SavedCover finalizeSave(Path tempFile, String suggestedName) throws IOException {
         try {
            String mimeType = Files.probeContentType(tempFile);
            if (mimeType == null || !mimeType.startsWith("image/")) {
                mimeType = "image/jpeg"; // Fallback
            }
            String extension = mimeType.equals("image/png") ? ".png" : (mimeType.equals("image/gif") ? ".gif" : ".jpg");

            // Crée un nom de fichier unique basé sur UUID
            String finalName = UUID.randomUUID().toString() + extension;
            Path finalPath = this.storagePath.resolve(finalName);

            // Déplace le fichier temporaire
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = this.storagePath.getFileName().toString() + "/" + finalName;

            // Retourne les métadonnées
            return new SavedCover(
                finalName,
                relativePath,
                mimeType,
                Files.size(finalPath),
                Instant.now().toEpochMilli()
            );
        } catch (IOException e) {
            Files.deleteIfExists(tempFile); // Assure le nettoyage même ici
            throw e; // Relance pour que l'appelant sache qu'il y a eu un problème
        } finally {
             // S'assure que le fichier temp est supprimé s'il existe encore (au cas où move échoue différemment)
             Files.deleteIfExists(tempFile);
        }
    }
}

