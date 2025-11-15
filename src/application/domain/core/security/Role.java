package application.domain.core.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p><strong>Role</strong> représente un groupe de permissions ({@link Capability})
 * associées à un utilisateur.</p>
 *
 * <p>Un rôle définit le périmètre d’action d’une personne
 * dans le système (ex. ADMIN, ADHERENT, BIBLIOTHECAIRE).</p>
 *
 * <p>Cette classe est <strong>purement métier</strong> : elle ne dépend
 * d’aucune technologie de persistance ou d’authentification.</p>
 *
 * @see Capability
 */
public final class Role {

    /** Nom symbolique standard du rôle Administrateur. */
    public static final String NAME_ADMIN = "ADMIN";

    /** Nom symbolique standard du rôle Adhérent. */
    public static final String NAME_ADHERENT = "ADHERENT";

    /** Nom symbolique du rôle (ex: "ADMIN", "ADHERENT") */
    private final String nom;

    /** Ensemble des capacités associées à ce rôle */
    private final Set<Capability> capabilities;

    /**
     * Construit un rôle avec un nom et un ensemble de capacités initiales.
     *
     * @param nom nom du rôle (non nul)
     * @param capabilities ensemble des capacités (modifiable en interne)
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
     * @return une nouvelle instance de {@code Role}
     */
    public static Role of(String nom) {
        return new Role(nom, new HashSet<>());
    }

    /**
     * Factory rôle standard ADMIN :
     * - accès au dashboard admin, catalogue, emprunts, pénalités,
     *   adhérents, utilisateurs, stats, historique, etc.
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
     * Factory rôle standard ADHERENT :
     * - consultation catalogue
     * - emprunt / retour
     * - consultation de ses emprunts / pénalités.
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

    /**
     * Retourne le nom du rôle.
     */
    public String getNom() {
        return nom;
    }

    /**
     * Nom du rôle (alias anglophone).
     */
    public String getName() {
        return nom;
    }

    /**
     * Retourne les capacités associées à ce rôle (lecture seule).
     */
    public Set<Capability> getCapabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    /**
     * Vérifie si le rôle possède une capacité donnée.
     */
    public boolean hasCapability(Capability capability) {
        return capabilities.contains(capability);
    }

    /**
     * Ajoute une capacité au rôle.
     */
    public void addCapability(Capability capability) {
        capabilities.add(Objects.requireNonNull(capability));
    }

    /**
     * Supprime une capacité du rôle.
     */
    public void removeCapability(Capability capability) {
        capabilities.remove(capability);
    }

    /** Rôle logique ADMIN ? */
    public boolean isAdmin() {
        return NAME_ADMIN.equalsIgnoreCase(nom);
    }

    /** Rôle logique ADHERENT ? */
    public boolean isAdherent() {
        return NAME_ADHERENT.equalsIgnoreCase(nom);
    }

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
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return nom.equalsIgnoreCase(role.nom);
    }

    @Override
    public int hashCode() {
        return nom.toLowerCase().hashCode();
    }
}