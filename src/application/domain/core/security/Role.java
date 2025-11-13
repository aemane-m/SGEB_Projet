package application.domain.core.security;

import java.util.Collections;
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
 * @author SGEB
 * @version 1.0
 */
public final class Role {

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
        this.capabilities = capabilities != null ? new HashSet<>(capabilities) : new HashSet<>();
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
     * Retourne le nom du rôle.
     *
     * @return nom du rôle
     */
    public String getNom() {
        return nom;
    }

    /**
     * Retourne les capacités associées à ce rôle (lecture seule).
     *
     * @return ensemble immuable des capacités
     */
    public Set<Capability> getCapabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    /**
     * Vérifie si le rôle possède une capacité donnée.
     *
     * @param capability capacité à tester
     * @return {@code true} si présente, sinon {@code false}
     */
    public boolean hasCapability(Capability capability) {
        return capabilities.contains(capability);
    }

    /**
     * Ajoute une capacité au rôle.
     *
     * @param capability capacité à ajouter
     */
    public void addCapability(Capability capability) {
        capabilities.add(Objects.requireNonNull(capability));
    }

    /**
     * Supprime une capacité du rôle.
     *
     * @param capability capacité à retirer
     */
    public void removeCapability(Capability capability) {
        capabilities.remove(capability);
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

    public String getName() {
        return nom;
    }
}
