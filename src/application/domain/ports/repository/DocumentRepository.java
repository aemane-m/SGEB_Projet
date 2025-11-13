package application.domain.ports.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import application.domain.core.document.Document;

/**
 * Port pour la gestion des documents de bibliothèque.
 */
public interface DocumentRepository extends Repository<Document, Long> {
    List<Document> findByTitre(String titre);
    List<Document> findByAuteur(String auteur);
    List<Document> findByGenre(String genre);
	/**
	 * {@inheritDoc}
	 * <p>Implémentation de l'exigence : "Rechercher un document par [...] ISBN."</p>
	 */
	Optional<Document> findByIsbn(String isbn);
	Collection<? extends Document> findByTitreContaining(String query);
	Collection<? extends Document> findByAuteurContaining(String query);
}
