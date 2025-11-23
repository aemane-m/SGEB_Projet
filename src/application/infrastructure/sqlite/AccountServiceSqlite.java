package application.infrastructure.sqlite;

import application.domain.core.security.Capability;
import application.domain.core.security.Role;
import application.domain.service.AccountService;
import application.ui.navigation.Navigator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * Implémentation SQLite de {@link AccountService}.
 *
 * Cet adaptateur parle SQL (table PERSONNE) mais expose une API
 * métier simple pour le domaine.
 *
 * Schéma attendu pour PERSONNE :
 * <pre>
 *   personne_id           INTEGER PRIMARY KEY
 *   email                 TEXT UNIQUE NOT NULL
 *   password_hash         TEXT NOT NULL
 *   nom                   TEXT NOT NULL
 *   prenom                TEXT NOT NULL
 *   isAdmin               INTEGER NOT NULL -- 1 = admin, 0 = adhérent
 *   actif                 INTEGER NOT NULL -- 1 = actif, 0 = désactivé
 *   status                TEXT NOT NULL    -- "ACTIVE", "SUSPENDED", ...
 *   password_must_change  INTEGER NOT NULL -- 1 = doit changer MDP
 *   created_at            TEXT NOT NULL    -- ISO-8601
 * </pre>
 *
 * Raccourci utilisé dans ce projet :
 * <ul>
 *   <li>si isAdmin = 0, on considère que adherentId = personne_id</li>
 *   <li>si isAdmin = 1, alors adherentId = null</li>
 * </ul>
 */
public final class AccountServiceSqlite implements AccountService {

    // ============================
    // ====== Constructeurs =======
    // ============================

    /**
     * Constructeur par défaut.
     * Le schéma est initialisé ailleurs (DatabaseInitializer).
     */
    public AccountServiceSqlite() {
        // rien de spécial ici
    }

    /**
     * Ancien constructeur prenant un Navigator (UI).
     * Conservé uniquement pour compatibilité avec le code existant.
     * L'infrastructure NE devrait pas dépendre de la couche UI.
     */
    @Deprecated
    public AccountServiceSqlite(Navigator navigator) {
        this();
    }

    // ============================
    // ===== Helpers sécurité =====
    // ============================

    /**
     * Hash très simplifié pour le TP.
     * ⚠ À NE PAS UTILISER EN PRODUCTION.
     */
    private static String placeholderHash_DO_NOT_USE_IN_PRODUCTION(String clear) {
        return Integer.toHexString(clear == null ? 0 : clear.hashCode());
    }

    /**
     * Vérification du mot de passe pour le TP.
     * ⚠ À NE PAS UTILISER EN PRODUCTION.
     */
    private static boolean placeholderCheck_DO_NOT_USE_IN_PRODUCTION(String clear, String dbHash) {
        return dbHash != null && dbHash.equals(placeholderHash_DO_NOT_USE_IN_PRODUCTION(clear));
    }

    /**
     * Validation basique d'un mot de passe côté domaine.
     * Ici : longueur minimale = 6 caractères.
     */
    private static void requireValidPassword(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("Mot de passe trop court (minimum 6 caractères).");
        }
    }

    /**
     * Construit une {@link UserSession} cohérente à partir des attributs de base d'un compte.
     *
     * <p>Ce helper est utilisé par :
     * <ul>
     *     <li>{@link #login(String, String)} après vérification du mot de passe,</li>
     *     <li>{@link #findSessionForPasswordReset(String)} dans le flux "mot de passe oublié".</li>
     * </ul>
     *
     * @param userId              identifiant technique PERSONNE
     * @param email               email du compte
     * @param isAdmin             flag administrateur
     * @param passwordMustChange  indique si un changement de mot de passe est imposé
     * @return une instance de {@link UserSession} prête à être propagée au reste de l'application
     */
    private static UserSession buildSession(long userId,
                                            String email,
                                            boolean isAdmin,
                                            boolean passwordMustChange) {

        Role role = isAdmin ? Role.adminRole() : Role.adherentRole();
        Set<Capability> caps = role.getCapabilities();
        Long adherentId = isAdmin ? null : userId;

        return new UserSession(
                userId,
                email,
                role,
                caps,
                passwordMustChange,
                adherentId
        );
    }

    // ============================
    // ====== Méthodes CRUD =======
    // ============================

    @Override
    public long countUsers() {
        final String sql = "SELECT COUNT(*) FROM PERSONNE";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.countUsers a échoué", e);
        }
    }

    @Override
    public boolean hasAnyAdmin() {
        final String sql = "SELECT 1 FROM PERSONNE WHERE isAdmin = 1 LIMIT 1";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next(); // true si au moins un admin existe

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.hasAnyAdmin a échoué", e);
        }
    }

    @Override
    public void createFirstAdmin(String email, String rawPassword, String nom, String prenom) {
        // On impose que ce soit le tout premier compte.
        if (countUsers() > 0) {
            throw new IllegalStateException("Un premier administrateur existe déjà.");
        }

        createAccountInternal(
                email,
                rawPassword,
                nom,
                prenom,
                true,          // isAdmin
                "ACTIVE",      // status initial
                false,         // passwordMustChange
                true           // actif
        );
    }

    @Override
    public void createAdmin(String email, String rawPassword, String nom, String prenom) {
        // Admin créé par un autre admin : on force le changement de mot de passe.
        createAccountInternal(
                email,
                rawPassword,
                nom,
                prenom,
                true,           // isAdmin
                "ACTIVE",       // status
                true,           // passwordMustChange
                true            // actif
        );
    }

    @Override
    public void createAdherentPublic(String email, String rawPassword, String nom, String prenom) {
        // L'adhérent est créé avec un mot de passe qui devra être changé.
        createAccountInternal(
                email,
                rawPassword,
                nom,
                prenom,
                false,          // isAdmin
                "ACTIVE",       // status
                true,           // passwordMustChange
                true            // actif
        );
    }

    /**
     * Méthode interne de création de compte paramétrable.
     */
    private void createAccountInternal(String email,
                                       String rawPassword,
                                       String nom,
                                       String prenom,
                                       boolean isAdmin,
                                       String status,
                                       boolean passwordMustChange,
                                       boolean actif) {

        // === Validation basique ===
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(prenom, "prenom");
        Objects.requireNonNull(status, "status");
        requireValidPassword(rawPassword);

        String trimmedEmail = email.trim();
        String trimmedNom = nom.trim();
        String trimmedPrenom = prenom.trim();

        if (trimmedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email vide.");
        }
        if (trimmedNom.isEmpty()) {
            throw new IllegalArgumentException("Nom vide.");
        }
        if (trimmedPrenom.isEmpty()) {
            throw new IllegalArgumentException("Prénom vide.");
        }

        final String sql = """
            INSERT INTO PERSONNE(
                email,
                password_hash,
                nom,
                prenom,
                isAdmin,
                actif,
                status,
                password_must_change,
                created_at
            ) VALUES (?,?,?,?,?,?,?,?,?)
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, trimmedEmail);
            ps.setString(i++, placeholderHash_DO_NOT_USE_IN_PRODUCTION(rawPassword));
            ps.setString(i++, trimmedNom);
            ps.setString(i++, trimmedPrenom);
            ps.setInt(i++, isAdmin ? 1 : 0);
            ps.setInt(i++, actif ? 1 : 0);
            ps.setString(i++, status);
            ps.setInt(i++, passwordMustChange ? 1 : 0);
            ps.setString(i, Instant.now().toString());

            ps.executeUpdate();

        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("unique")) {
                throw new RuntimeException("Un compte avec cet email existe déjà.");
            }
            throw new RuntimeException("AccountServiceSqlite.createAccountInternal a échoué : " + e.getMessage(), e);
        }
    }

    // ============================
    // ============ LOGIN =========
    // ============================

    @Override
    public LoginResult login(String email, String rawPassword) {
        if (email == null || email.isBlank()) {
            return LoginResult.failure("Email requis.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            return LoginResult.failure("Mot de passe requis.");
        }

        final String sql = """
            SELECT
                personne_id,
                email,
                password_hash,
                isAdmin,
                actif,
                status,
                password_must_change
            FROM PERSONNE
            WHERE email = ?
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // "Dummy check" pour ne pas révéler si l'email existe ou non
                    placeholderCheck_DO_NOT_USE_IN_PRODUCTION(rawPassword, "dummy_hash");
                    return LoginResult.failure("Email ou mot de passe invalide.");
                }

                long userId = rs.getLong("personne_id");
                String dbEmail = rs.getString("email");
                String dbHash = rs.getString("password_hash");
                boolean isAdmin = rs.getInt("isAdmin") == 1;
                boolean actif = rs.getInt("actif") == 1;
                String status = rs.getString("status");
                boolean passwordMustChange = rs.getInt("password_must_change") == 1;

                // 1) Vérification du mot de passe
                if (!placeholderCheck_DO_NOT_USE_IN_PRODUCTION(rawPassword, dbHash)) {
                    return LoginResult.failure("Email ou mot de passe invalide.");
                }

                // 2) Compte actif ?
                if (!actif) {
                    return LoginResult.failure("Ce compte est désactivé.");
                }

                // 3) Statut logique
                if (status == null || !"ACTIVE".equalsIgnoreCase(status)) {
                    return LoginResult.failure("Ce compte n'est pas actif (status = " + status + ").");
                }

                // 4) Construction de la session via le helper commun
                UserSession session = buildSession(userId, dbEmail, isAdmin, passwordMustChange);
                return LoginResult.success(session);
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.login a échoué", e);
        }
    }

    // ============================
    // === Changement de MDP ======
    // ============================

    @Override
    public void changePasswordAndClearFlag(long userId, String rawPassword) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId invalide.");
        }
        requireValidPassword(rawPassword);

        final String sql = """
            UPDATE PERSONNE
            SET password_hash = ?, password_must_change = 0
            WHERE personne_id = ?
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, placeholderHash_DO_NOT_USE_IN_PRODUCTION(rawPassword));
            ps.setLong(2, userId);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("Utilisateur introuvable pour id = " + userId);
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.changePasswordAndClearFlag a échoué : " + e.getMessage(), e);
        }
    }

    @Override
    public void adminResetPassword(long targetUserId,
                                   String rawPassword,
                                   boolean forcePasswordChange) {

        if (targetUserId <= 0) {
            throw new IllegalArgumentException("userId invalide.");
        }
        requireValidPassword(rawPassword);

        final String sql = """
            UPDATE PERSONNE
            SET password_hash = ?,
                password_must_change = ?
            WHERE personne_id = ?
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, placeholderHash_DO_NOT_USE_IN_PRODUCTION(rawPassword));
            ps.setInt(2, forcePasswordChange ? 1 : 0);
            ps.setLong(3, targetUserId);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("Aucun compte trouvé pour id = " + targetUserId);
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.adminResetPassword a échoué : " + e.getMessage(), e);
        }
    }

    /**
     * Recherche un utilisateur par email dans le but d'un changement de mot de passe
     * déclenché via la fonctionnalité "mot de passe oublié".
     *
     * <p>⚠️ Cette méthode NE vérifie PAS le mot de passe. Elle est donc destinée
     * exclusivement à un flux contrôlé côté UI, utilisé dans le cadre d'une
     * démonstration. En production, il faudrait :
     * <ul>
     *     <li>envoyer un email de réinitialisation avec un token sécurisé,</li>
     *     <li>ne jamais permettre un changement de mot de passe direct à partir du seul email.</li>
     * </ul>
     *
     * <p>Règles métier appliquées :
     * <ul>
     *     <li>Retourne {@code Optional.empty()} si l'email n'existe pas.</li>
     *     <li>Retourne {@code Optional.empty()} si le compte est inactif (actif = 0).</li>
     *     <li>Retourne {@code Optional.empty()} si le statut n'est pas "ACTIVE".</li>
     *     <li>Sinon, construit une {@link UserSession} minimale identique à celle de {@link #login}
     *         (via {@link #buildSession(long, String, boolean, boolean)}) sans vérifier le mot de passe.</li>
     * </ul>
     *
     * <p>Cette session peut ensuite être passée à
     * {@link application.ui.navigation.Navigator#goToChangePassword(UserSession)}
     * pour afficher l'écran de changement de mot de passe.
     *
     * @param email email déclaré par l'utilisateur
     * @return un {@code Optional<UserSession>} si l'utilisateur existe et peut changer son mot de passe,
     *         sinon {@code Optional.empty()}
     */
    @Override
    public Optional<UserSession> findSessionForPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        final String sql = """
            SELECT
                personne_id,
                email,
                isAdmin,
                actif,
                status,
                password_must_change
            FROM PERSONNE
            WHERE email = ?
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // compte inexistant
                    return Optional.empty();
                }

                long userId = rs.getLong("personne_id");
                String dbEmail = rs.getString("email");
                boolean isAdmin = rs.getInt("isAdmin") == 1;
                boolean actif = rs.getInt("actif") == 1;
                String status = rs.getString("status");
                boolean passwordMustChange = rs.getInt("password_must_change") == 1;

                // Compte actif ?
                if (!actif) return Optional.empty();
                if (status == null || !"ACTIVE".equalsIgnoreCase(status)) return Optional.empty();

                // Construction de la session via le helper commun
                UserSession session = buildSession(userId, dbEmail, isAdmin, passwordMustChange);
                return Optional.of(session);
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.findSessionForPasswordReset a échoué : " + e.getMessage(), e);
        }
    }

    // =========================================================
    // ============ LISTE DES COMPTES ==========================
    // =========================================================

    @Override
    public List<UserAccountSummary> listAllAccounts() {
        final String sql = """
            SELECT
                personne_id,
                email,
                isAdmin,
                actif,
                status,
                password_must_change
            FROM PERSONNE
            ORDER BY isAdmin DESC, email ASC
            """;

        List<UserAccountSummary> list = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToSummary(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.listAllAccounts a échoué", e);
        }

        return list;
    }

    @Override
    public Optional<UserAccountSummary> findAccountByAdherentId(long adherentId) {
        final String sql = """
            SELECT
                personne_id,
                email,
                isAdmin,
                actif,
                status,
                password_must_change
            FROM PERSONNE
            WHERE personne_id = ? AND isAdmin = 0
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, adherentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRowToSummary(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.findAccountByAdherentId a échoué : " + e.getMessage(), e);
        }
    }

    /**
     * Construit un {@link UserAccountSummary} à partir de la ligne courante du ResultSet.
     */
    private static UserAccountSummary mapRowToSummary(ResultSet rs) throws SQLException {
        long userId = rs.getLong("personne_id");
        String email = rs.getString("email");
        boolean isAdmin = rs.getInt("isAdmin") == 1;
        boolean actif = rs.getInt("actif") == 1;
        String status = rs.getString("status");
        boolean passwordMustChange = rs.getInt("password_must_change") == 1;

        String roleName = isAdmin
                ? Role.adminRole().getName()
                : Role.adherentRole().getName();

        Long adherentId = isAdmin ? null : userId;

        return new UserAccountSummary(
                userId,
                email,
                roleName,
                status,
                actif,
                passwordMustChange,
                adherentId
        );
    }

    // =========================================================
    // ============ CHANGEMENT STATUT ==========================
    // =========================================================

    @Override
    public void changeAccountStatus(long targetUserId, String newStatus, long actingUserId) {
        if (targetUserId <= 0 || actingUserId <= 0) {
            throw new IllegalArgumentException("Ids utilisateurs invalides.");
        }
        Objects.requireNonNull(newStatus, "newStatus");

        // Règle métier : on ne modifie pas son propre statut
        if (targetUserId == actingUserId) {
            throw new IllegalArgumentException("Impossible de modifier votre propre statut de compte.");
        }

        String normalized = newStatus.trim().toUpperCase();
        int actifValue;

        // Pour le TP : on gère seulement ACTIVE / SUSPENDED.
        switch (normalized) {
            case "ACTIVE" -> actifValue = 1;
            case "SUSPENDED" -> actifValue = 0;
            default -> throw new IllegalArgumentException(
                    "Statut invalide : " + newStatus + " (utiliser ACTIVE ou SUSPENDED)"
            );
        }

        final String sql = """
            UPDATE PERSONNE
            SET status = ?, actif = ?
            WHERE personne_id = ?
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, normalized);
            ps.setInt(2, actifValue);
            ps.setLong(3, targetUserId);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("Aucun compte trouvé pour id = " + targetUserId);
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.changeAccountStatus a échoué : " + e.getMessage(), e);
        }
    }

    // =========================================================
    // ============ SUPPRESSION COMPTE =========================
    // =========================================================

    @Override
    public void deleteAccount(long targetUserId, long actingUserId) {
        if (targetUserId <= 0 || actingUserId <= 0) {
            throw new IllegalArgumentException("Ids utilisateurs invalides.");
        }

        // Règle métier : on ne supprime pas son propre compte
        if (targetUserId == actingUserId) {
            throw new IllegalArgumentException("Impossible de supprimer votre propre compte.");
        }

        final String sql = "DELETE FROM PERSONNE WHERE personne_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, targetUserId);
            int deleted = ps.executeUpdate();

            if (deleted == 0) {
                throw new IllegalStateException("Aucun compte trouvé pour id = " + targetUserId);
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.deleteAccount a échoué : " + e.getMessage(), e);
        }
    }
}