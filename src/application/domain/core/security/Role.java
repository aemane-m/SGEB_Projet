package application.domain.core.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * <strong>Role</strong> représente un groupe de permissions
 * ({@link Capability}) associées à un utilisateur.
 * </p>
 *
 * <p>
 * Un rôle définit le <strong>périmètre d’action</strong> d’une personne
 * dans le système (ex. {@code ADMIN}, {@code ADHERENT}, {@code BIBLIOTHECAIRE}).
 * </p>
 *
 * <p>
 * Classe purement domaine : aucune dépendance à la persistance ou à un framework
 * d’authentification.
 * </p>
 *
 * @see Capability
 */
public final class Role {

    /** Nom symbolique standard du rôle Administrateur. */
    public static final String NAME_ADMIN = "ADMIN";

    /** Nom symbolique standard du rôle Adhérent. */
    public static final String NAME_ADHERENT = "ADHERENT";

    /** Nom symbolique du rôle (ex : "ADMIN", "ADHERENT"). */
    private final String nom;

    /** Ensemble des capacités associées à ce rôle. */
    private final Set<Capability> capabilities;

    // ======================================================================
    // ============================ CONSTRUCTEURS ===========================
    // ======================================================================

    /**
     * Construit un rôle avec un nom et un ensemble de capacités initiales.
     *
     * @param nom          nom du rôle (non null)
     * @param capabilities ensemble initial de capacités (peut être null → vide)
     */
    public Role(String nom, Set<Capability> capabilities) {
        this.nom = Objects.requireNonNull(nom, "nom");
        this.capabilities = (capabilities != null)
                ? new HashSet<>(capabilities)
                : new HashSet<>();
    }

    /**
     * Crée un rôle vide avec le nom spécifié.
     *
     * @param nom nom du rôle
     * @return nouvelle instance de {@code Role} sans capability
     */
    public static Role of(String nom) {
        return new Role(nom, new HashSet<>());
    }

    /**
     * Factory pour le rôle standard ADMIN.
     *
     * <p>
     * Contient les principales capabilities d’administration :
     * dashboard, gestion des utilisateurs, catalogue, emprunts, pénalités,
     * statistiques, historique, etc.
     * </p>
     */
    public static Role adminRole() {
        Set<Capability> caps = EnumSet.of(
                Capability.VIEW_ADMIN_DASHBOARD,
                Capability.MANAGE_USERS,
                Capability.MANAGE_ADHERENTS,
                Capability.MANAGE_DOCUMENTS,
                Capability.MANAGE_CATALOGUE,
                Capability.MANAGE_EMPRUNTS,
                Capability.MANAGE_PENALITES,
                Capability.VIEW_STATS,
                Capability.VIEW_HISTORY,
                Capability.CONSULT_CATALOG
        );
        return new Role(NAME_ADMIN, caps);
    }

    /**
     * Factory pour le rôle standard ADHERENT.
     *
     * <p>
     * Capacités :
     * </p>
     * <ul>
     *     <li>Consulter le catalogue</li>
     *     <li>Emprunter / retourner des documents</li>
     *     <li>Consulter ses emprunts et pénalités</li>
     * </ul>
     */
    public static Role adherentRole() {
        Set<Capability> caps = EnumSet.of(
                Capability.CONSULT_CATALOG,
                Capability.BORROW_DOCUMENT,
                Capability.RETURN_DOCUMENT,
                Capability.VIEW_SELF_LOANS,
                Capability.VIEW_SELF_PENALTIES
        );
        return new Role(NAME_ADHERENT, caps);
    }

    // ======================================================================
    // ================================ ACCÈS ================================
    // ======================================================================

    /** @return nom du rôle (ex : "ADMIN"). */
    public String getNom() {
        return nom;
    }

    /** Alias anglophone de {@link #getNom()}. */
    public String getName() {
        return nom;
    }

    /**
     * Retourne les capabilities associées à ce rôle.
     *
     * @return vue non modifiable de l’ensemble des capabilities
     */
    public Set<Capability> getCapabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    // ======================================================================
    // ============================= MÉTHODES MÉTIER ========================
    // ======================================================================

    /**
     * Vérifie si le rôle possède une capability donnée.
     *
     * @param capability capability à tester
     * @return {@code true} si présente
     */
    public boolean hasCapability(Capability capability) {
        return capabilities.contains(capability);
    }

    /**
     * Ajoute une capability au rôle.
     *
     * @param capability capability à ajouter (non null)
     */
    public void addCapability(Capability capability) {
        capabilities.add(Objects.requireNonNull(capability));
    }

    /**
     * Supprime une capability du rôle.
     *
     * @param capability capability à retirer
     */
    public void removeCapability(Capability capability) {
        capabilities.remove(capability);
    }

    /** @return {@code true} si le rôle est logiquement ADMIN. */
    public boolean isAdmin() {
        return NAME_ADMIN.equalsIgnoreCase(nom);
    }

    /** @return {@code true} si le rôle est logiquement ADHERENT. */
    public boolean isAdherent() {
        return NAME_ADHERENT.equalsIgnoreCase(nom);
    }

    // ======================================================================
    // =========================== OBJECT METHODS ============================
    // ======================================================================

    @Override
    public String toString() {
        return "Role{" +
                "nom='" + nom + '\'' +
                ", capabilities=" + capabilities +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        // Un rôle est identifié par son nom (insensible à la casse)
        return nom.equalsIgnoreCase(other.nom);
    }

    @Override
    public int hashCode() {
        return nom.toLowerCase().hashCode();
    }
}