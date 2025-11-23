package application.domain.ports.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import application.domain.core.document.Document;

/**
 * Port de persistance pour les {@link Document} de la bibliothèque.
 *
 * <p>
 * Permet de rechercher les documents selon plusieurs critères :
 * titre, auteur, genre, ISBN, etc.
 * </p>
 */
public interface DocumentRepository extends Repository<Document, Long> {

    /**
     * Recherche les documents dont le titre correspond exactement à la valeur donnée.
     *
     * @param titre titre exact à rechercher
     * @return liste des documents trouvés
     */
    List<Document> findByTitre(String titre);

    /**
     * Recherche les documents par auteur exact.
     *
     * @param auteur nom de l’auteur
     * @return liste des documents de cet auteur
     */
    List<Document> findByAuteur(String auteur);

    /**
     * Recherche les documents par genre exact (ex : "Roman", "SF").
     *
     * @param genre genre à rechercher
     * @return liste des documents du genre donné
     */
    List<Document> findByGenre(String genre);

    /**
     * Recherche un document par son ISBN.
     *
     * @param isbn identifiant ISBN
     * @return document trouvé ou vide si aucun
     */
    Optional<Document> findByIsbn(String isbn);

    /**
     * Recherche les documents dont le titre contient une chaîne de caractères
     * (recherche partielle, ex : "java" → "Java pour les nuls").
     *
     * @param query fragment de texte à chercher dans le titre
     * @return collection de documents trouvés
     */
    Collection<? extends Document> findByTitreContaining(String query);

    /**
     * Recherche les documents dont le nom d’auteur contient une chaîne de caractères
     * (recherche partielle).
     *
     * @param query fragment de texte à chercher dans le nom de l’auteur
     * @return collection de documents trouvés
     */
    Collection<? extends Document> findByAuteurContaining(String query);
}