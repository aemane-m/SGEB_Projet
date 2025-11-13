package application.domain.ports.external;

import java.util.Optional;

/**
 * Port (interface) pour un service externe capable de 
 * trouver des métadonnées de livre par ISBN.
 */
public interface BookInfoProvider {
    Optional<BookMeta> findBookByIsbn(String isbn);
}