package application.domain.ports.repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface générique de base pour un dépôt (Repository Pattern).
 *
 * <p>
 * Un {@code Repository} encapsule les opérations de base pour gérer
 * des entités : sauvegarde, lecture, suppression, comptage.
 * </p>
 *
 * @param <T>  type de l'entité (ex : Adherent, Document, Emprunt)
 * @param <ID> type de l'identifiant (ex : Long)
 */
public interface Repository<T, ID> {

    /**
     * Sauvegarde une entité (création ou mise à jour).
     *
     * @param entity entité à sauvegarder
     * @return entité sauvegardée (souvent avec un ID rempli ou mis à jour)
     */
    T save(T entity);

    /**
     * Recherche une entité par son identifiant.
     *
     * @param id identifiant à rechercher
     * @return un {@link Optional} contenant l'entité si trouvée, vide sinon
     */
    Optional<T> findById(ID id);

    /**
     * Retourne toutes les entités gérées par ce repository.
     *
     * @return liste de toutes les entités
     */
    List<T> findAll();

    /**
     * Supprime une entité par son identifiant.
     *
     * @param id identifiant de l'entité à supprimer
     */
    void delete(ID id);

    /**
     * Compte le nombre total d'entités.
     *
     * @return nombre d'entités stockées
     */
    long count();
}