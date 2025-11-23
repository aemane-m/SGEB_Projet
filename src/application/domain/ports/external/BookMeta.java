package application.domain.ports.external;

/**
 * <p>
 * DTO (Data Transfer Object) contenant les métadonnées d’un livre
 * retournées par un {@link BookInfoProvider}.
 * </p>
 *
 * <p>
 * Ce type est immuable (record) et n’appartient à aucune technologie
 * particulière : il représente un contrat d’échange simple.
 * </p>
 */
public record BookMeta(
        String isbn,
        String titre,
        String auteur,
        String editeur,
        Integer anneePublication,
        String genre,
        Integer nbPages,
        String coverUrl
) {}