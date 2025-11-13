package application.domain.ports.repository;

import java.util.List;
import java.util.Optional;
import application.domain.core.personne.Adherent;

/**
 * Port pour la gestion des adhérents.
 */
public interface AdherentRepository extends Repository<Adherent, Long> {
    Optional<Adherent> findByEmail(String email);

	/**
	 * {@inheritDoc}
	 * <p>Implémentation de l'exigence : "Rechercher un adhérent par [...] nom."</p>
	 */
	List<Adherent> findByNom(String nom);
}
