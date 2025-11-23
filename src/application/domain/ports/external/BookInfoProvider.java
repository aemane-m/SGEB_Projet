package application.domain.ports.external;

import java.util.Optional;

/**
 * <p>Port externe permettant de rechercher les métadonnées d’un livre via son ISBN.</p>
 *
 * <p>
 * Ce port représente un service externe (API de livres, base documentaire, etc.)
 * sans imposer de technologie concrète à la couche domaine.
 * </p>
 */
public interface BookInfoProvider {

    /**
     * Recherche un livre à partir de son ISBN.
     *
     * @param isbn identifiant ISBN (non null, non vide)
     * @return un {@link BookMeta} si trouvé, sinon {@link Optional#empty()}
     */
    Optional<BookMeta> findBookByIsbn(String isbn);
}