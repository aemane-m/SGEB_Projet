package application.domain.core.personne;

import java.util.EnumSet;

import application.domain.core.security.Capability;
import application.domain.core.security.Role;

/**
 * <p><strong>Administrateur</strong> représente un utilisateur disposant
 * des droits complets sur le système SGEB.</p>
 *
 * <p>Il hérite de {@link Personne} et possède un {@link Role}
 * fixe nommé <code>ADMIN</code>, contenant toutes les
 * {@link Capability} critiques du système.</p>
 *
 * <p>Les administrateurs peuvent :
 * <ul>
 *     <li>Gérer les adhérents et leurs emprunts</li>
 *     <li>Gérer le catalogue de documents</li>
 *     <li>Superviser les pénalités</li>
 *     <li>Consulter les statistiques globales</li>
 * </ul>
 * </p>
 *
 * @see application.domain.core.personne.Personne
 * @see application.domain.core.personne.security.security.Role
 * @see application.domain.core.personne.security.security.Capability
 *
 * @author SGEB
 * @version 1.0
 */
public final class Administrateur extends Personne {

    /** Rôle fixe associé à tout administrateur */
    private final Role role;

    /**
     * Construit un administrateur avec un identifiant.
     *
     * @param id identifiant unique
     * @param nom nom de l’administrateur
     * @param prenom prénom de l’administrateur
     * @param email adresse e-mail
     * @param telephone numéro de téléphone
     */
    public Administrateur(Long id, String nom, String prenom, String email, String telephone) {
        super(id, nom, prenom, email, telephone);
        this.role = initialiserRoleAdmin();
    }

    /**
     * Construit un administrateur sans identifiant initial (ex: avant persistance).
     *
     * @param nom nom de l’administrateur
     * @param prenom prénom de l’administrateur
     * @param email adresse e-mail
     * @param telephone numéro de téléphone
     */
    public Administrateur(String nom, String prenom, String email, String telephone) {
        super(null, nom, prenom, email, telephone);
        this.role = initialiserRoleAdmin();
    }

    /**
     * Initialise le rôle ADMIN avec les capacités par défaut.
     *
     * @return rôle configuré avec ses capacités
     */
    private Role initialiserRoleAdmin() {
        Role r = Role.of("ADMIN");
        for (Capability c : EnumSet.of(
                Capability.MANAGE_ADHERENTS,
                Capability.MANAGE_DOCUMENTS,
                Capability.MANAGE_EMPRUNTS,
                Capability.MANAGE_PENALITES,
                Capability.VIEW_STATS
        )) {
            r.addCapability(c);
        }
        return r;
    }

    /**
     * Retourne le rôle de l’administrateur.
     *
     * @return rôle ADMIN
     */
    public Role getRole() {
        return role;
    }

    /**
     * Vérifie si l’administrateur possède une capacité donnée.
     *
     * @param capability capacité à vérifier
     * @return {@code true} si autorisé
     */
    public boolean peut(Capability capability) {
        return role.hasCapability(capability);
    }

    @Override
    public String toString() {
        return super.toString() +
                " | Administrateur (" + role.getCapabilities().size() + " capacités)";
    }
}
