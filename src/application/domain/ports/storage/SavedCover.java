package application.domain.ports.storage;

/**
 * DTO (Data Transfer Object) pour une couverture sauvegardée.
 */
public record SavedCover(
	    String finalName,     // ex: "9a7b... .jpg"
	    String relativePath,  // ex: "covers/9a7b... .jpg"
	    String mimeType,      // ex: "image/jpeg"
	    long size,            // Taille du fichier en octets
	    long updatedAt        // Timestamp de la création (epoch milli)
	) {
	    // Il n'y a plus besoin de constructeur personnalisé.
	}