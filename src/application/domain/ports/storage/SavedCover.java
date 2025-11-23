package application.domain.ports.storage;

/**
 * "Bon de livraison" d'une image sauvegardée.
 * <p>
 * C'est un simple DTO (Data Transfer Object) qui contient les infos techniques
 * sur le fichier qui vient d'être créé sur le disque.
 * </p>
 */
public record SavedCover(
    /** Le nom final du fichier sur le disque (ex: "9780134685991.jpg"). */
    String finalName,

    /** Le chemin pour y accéder (ex: "covers/9780134685991.jpg"). */
    String relativePath,

    /** Le type de l'image (ex: "image/jpeg"). */
    String mimeType,

    /** La taille du fichier en octets. */
    long size,

    /** Date de création (Timestamp). */
    long updatedAt
) {
    // Record = Constructeur, getters, equals, hashCode automatiques.
}