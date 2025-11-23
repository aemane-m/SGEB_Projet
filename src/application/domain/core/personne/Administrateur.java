package application.domain.core.personne;

import java.util.EnumSet;

import application.domain.core.security.Capability;
import application.domain.core.security.Role;

/**
 * Représente un <strong>administrateur du système SGEB</strong>.
 *
 * <p>
 * Un administrateur est une personne disposant des droits complets sur
 * l'application. Il hérite de {@link Personne} et se voit attribuer un
 * rôle fixe {@code ADMIN} contenant l’ensemble des {@link Capability}
 * critiques du système.
 * </p>
 *
 * <h3>Responsabilités typiques d'un Administrateur</h3>
 * <ul>
 *     <li>Gérer les adhérents et leurs emprunts</li>
 *     <li>Gérer et maintenir le catalogue de documents</li>
 *     <li>Superviser et valider les pénalités</li>
 *     <li>Accéder au tableau de bord et aux statistiques globales</li>
 * </ul>
 *
 * <p>
 * Le rôle ADMIN ne peut pas être altéré par l’utilisateur et contient
 * systématiquement toutes les capacités définies dans
 * {@link Capability}.
 * </p>
 *
 * <h3>Modèle d'autorisation (RBAC)</h3>
 * <p>
 * L’application utilise un système d’autorisations basé sur les rôles (RBAC) :
 * {@link Role} → ensemble de {@link Capability}.  
 * Cette structure permet d’écrire :
 * </p>
 *
 * <pre>{@code
 * Administrateur admin = ...;
 * if (admin.peut(Capability.MANAGE_DOCUMENTS)) {
 *     // action autorisée
 * }
 * }</pre>
 *
 * @see Personne
 * @see Capability
 * @see Role
 */
public final class Administrateur extends Personne {

    /** Rôle fixe associé à tout administrateur SGEB. */
    private final Role role;

    // ======================================================================
    // ============================= CONSTRUCTEURS ===========================
    // ======================================================================

    /**
     * Construit un administrateur avec un identifiant existant (chargement DB).
     *
     * @param id identifiant unique (non null après persistance)
     * @param nom nom de l’administrateur
     * @param prenom prénom de l’administrateur
     * @param email e-mail de contact
     * @param telephone numéro de téléphone
     */
    public Administrateur(Long id, String nom, String prenom, String email, String telephone) {
        super(id, nom, prenom, email, telephone);
        this.role = initialiserRoleAdmin();
    }

    /**
     * Construit un administrateur sans identifiant initial
     * (utilisé lors de la création du premier compte via le Setup Wizard).
     *
     * @param nom nom de l’administrateur
     * @param prenom prénom de l’administrateur
     * @param email e-mail de contact
     * @param telephone numéro de téléphone
     */
    public Administrateur(String nom, String prenom, String email, String telephone) {
        super(null, nom, prenom, email, telephone);
        this.role = initialiserRoleAdmin();
    }

    // ======================================================================
    // ============================== MÉTIER / RBAC ==========================
    // ======================================================================

    /**
     * Initialise le rôle ADMIN avec l'ensemble des capacités critiques.
     *
     * <p>
     * Le rôle est créé en utilisant un pattern factory ({@link Role#of(String)}),
     * puis enrichi avec les {@link Capability} prédéfinies.
     * </p>
     *
     * @return instance immutable du rôle ADMIN
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
     * Retourne le rôle attaché à l’administrateur.
     *
     * @return rôle ADMIN contenant toutes les capabilities.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Vérifie si l’administrateur possède une capacité donnée.
     *
     * @param capability capacité à tester
     * @return true si la capacité est présente dans le rôle ADMIN
     */
    public boolean peut(Capability capability) {
        return role.hasCapability(capability);
    }

    // ======================================================================
    // ================================ UTILITAIRE ===========================
    // ======================================================================

    @Override
    public String toString() {
        return super.toString() +
                " | Administrateur (" + role.getCapabilities().size() + " capacités)";
    }
}