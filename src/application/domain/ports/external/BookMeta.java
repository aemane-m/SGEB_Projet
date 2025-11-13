package application.domain.ports.external;

/**
 * DTO (Data Transfer Object) pour les métadonnées
 * retournées par le BookInfoProvider.
 * CORRIGÉ : Ajout de nbPages, suppression de description (non fournie).
 */
public record BookMeta(
    String isbn,
    String titre,
    String auteur,
    String editeur,
    Integer anneePublication,
    String genre,       // Gardé (sera peuplé)
    Integer nbPages,    // <-- AJOUTÉ
    String coverUrl
) {}

