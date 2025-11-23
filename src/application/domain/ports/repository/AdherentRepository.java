package application.domain.ports.repository;

import java.util.List;
import java.util.Optional;

import application.domain.core.personne.Adherent;

/**
 * Port de persistance pour les {@link Adherent}.
 *
 * <p>
 * Étend le repository générique avec des méthodes de recherche
 * spécifiques au domaine (email, nom).
 * </p>
 */
public interface AdherentRepository extends Repository<Adherent, Long> {

    /**
     * Recherche un adhérent par son adresse e-mail.
     *
     * @param email adresse e-mail à rechercher
     * @return un {@link Optional} contenant l’adhérent si trouvé, vide sinon
     */
    Optional<Adherent> findByEmail(String email);

    /**
     * Recherche les adhérents portant un nom donné.
     *
     * @param nom nom de famille à rechercher
     * @return liste des adhérents correspondant au nom
     */
    List<Adherent> findByNom(String nom);
}