package application.domain.ports.repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface générique de base pour un dépôt (Repository Pattern).
 * @param <T>  Le type de l'entité
 * @param <ID> Le type de l'identifiant de l'entité
 */
public interface Repository<T, ID> {

    /**
     * Sauvegarde (crée ou met à jour) une entité.
     * @param entity L'entité à sauvegarder.
     * @return L'entité sauvegardée (potentiellement avec un ID mis à jour).
     */
    T save(T entity);

    /**
     * Trouve une entité par son identifiant.
     * @param id L'ID à rechercher.
     * @return Un Optional contenant l'entité si elle est trouvée.
     */
    Optional<T> findById(ID id);

    /**
     * Récupère toutes les entités de ce type.
     * @return Une liste de toutes les entités.
     */
    List<T> findAll();

    /**
     * Supprime une entité par son identifiant.
     * @param id L'ID de l'entité à supprimer.
     */
    void delete(ID id);

    /**
     * Compte le nombre total d'entités.
     * @return Le nombre total d'entités.
     */
    long count();
}
