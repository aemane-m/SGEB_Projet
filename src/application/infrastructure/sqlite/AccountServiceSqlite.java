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
import java.util.Objects;
import java.util.Set;

/**
 * Implémentation SQLite de {@link AccountService}.
 *
 * Adapter d'infrastructure : parle SQL, mais expose l'interface
 * métier {@link AccountService}.
 *
 * Hypothèse de schéma pour la table PERSONNE :
 *   - personne_id           INTEGER PRIMARY KEY
 *   - email                 TEXT UNIQUE NOT NULL
 *   - password_hash         TEXT NOT NULL
 *   - nom                   TEXT NOT NULL
 *   - prenom                TEXT NOT NULL
 *   - isAdmin               INTEGER NOT NULL
 *   - actif                 INTEGER NOT NULL
 *   - status                TEXT NOT NULL
 *   - password_must_change  INTEGER NOT NULL
 *   - created_at            TEXT NOT NULL
 *
 * Pour simplifier dans ce TP :
 *   - si isAdmin = 0  => on considère que l'adhérentId == personne_id
 *   - si isAdmin = 1  => adherentId = null
 */
public final class AccountServiceSqlite implements AccountService {

    // ============================
    // ====== Constructeurs =======
    // ============================

    public AccountServiceSqlite() {
        // DatabaseInitializer s'occupe du schéma
    }

    /**
     * Ancien constructeur prenant un Navigator.
     * On le garde pour compat avec Main, mais il ne fait rien de spécial.
     * L’infrastructure NE devrait PAS dépendre de la UI.
     */
    public AccountServiceSqlite(Navigator navigator) {
        this();
    }

    // ============================
    // ===== Helpers sécurité =====
    // ============================

    // TODO: remplacer par BCrypt (ou autre) pour une vraie appli.
    private static String placeholderHash_DO_NOT_USE_IN_PRODUCTION(String clear) {
        return Integer.toHexString(clear == null ? 0 : clear.hashCode());
    }

    private static boolean placeholderCheck_DO_NOT_USE_IN_PRODUCTION(String clear, String dbHash) {
        return dbHash != null && dbHash.equals(placeholderHash_DO_NOT_USE_IN_PRODUCTION(clear));
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
            throw new RuntimeException("AccountServiceSqlite.countUsers failed", e);
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
        // Pour un TP : on le met directement en ACTIVE.
        // En prod-like, on pourrait choisir "PENDING_EMAIL_VERIFICATION".
        createAccountInternal(
                email,
                rawPassword,
                nom,
                prenom,
                false,          // isAdmin
                "ACTIVE",       // status
                false,          // passwordMustChange
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
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(prenom, "prenom");
        Objects.requireNonNull(status, "status");

        String trimmedEmail = email.trim();
        String trimmedNom = nom.trim();
        String trimmedPrenom = prenom.trim();

        if (trimmedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email vide");
        }
        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("Mot de passe trop court (min 6 caractères).");
        }
        if (trimmedNom.isEmpty()) {
            throw new IllegalArgumentException("Nom vide");
        }
        if (trimmedPrenom.isEmpty()) {
            throw new IllegalArgumentException("Prénom vide");
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
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("unique")) {
                throw new RuntimeException("Un compte avec cet email existe déjà.");
            }
            throw new RuntimeException("AccountServiceSqlite.createAccountInternal failed: " + msg, e);
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
                nom,
                prenom,
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
                    // Dummy check pour éviter le timing attack / user enumeration
                    placeholderCheck_DO_NOT_USE_IN_PRODUCTION(rawPassword, "dummy_hash_value");
                    return LoginResult.failure("Email ou mot de passe invalide.");
                }

                long userId = rs.getLong("personne_id");
                String dbEmail = rs.getString("email");
                String dbHash = rs.getString("password_hash");
                boolean actif = rs.getInt("actif") == 1;
                boolean isAdmin = rs.getInt("isAdmin") == 1;
                String status = rs.getString("status");
                boolean passwordMustChange = rs.getInt("password_must_change") == 1;

                // 1) Mot de passe
                if (!placeholderCheck_DO_NOT_USE_IN_PRODUCTION(rawPassword, dbHash)) {
                    return LoginResult.failure("Email ou mot de passe invalide.");
                }

                // 2) Actif ?
                if (!actif) {
                    return LoginResult.failure("Ce compte a été désactivé.");
                }

                // 3) Status ?
                if (status == null || !"ACTIVE".equalsIgnoreCase(status)) {
                    return LoginResult.failure("Ce compte n'est pas encore actif (status = " + status + ").");
                }

                // 4) Construction du Role + capabilities
                Role role = isAdmin ? Role.adminRole() : Role.adherentRole();
                Set<Capability> caps = role.getCapabilities();

                // 5) Détermination de l'adherentId (simplification pour le TP)
                //    - admin : pas d'adhérent ⇒ adherentId = null
                //    - adhérent : on suppose adherentId == personne_id
                Long adherentId = isAdmin ? null : userId;

                // 6) Création de la session domaine
                UserSession session = new UserSession(
                        userId,
                        dbEmail,
                        role,
                        caps,
                        passwordMustChange,
                        adherentId
                );

                return LoginResult.success(session);
            }

        } catch (SQLException e) {
            throw new RuntimeException("AccountServiceSqlite.login failed", e);
        }
    }

    // ============================
    // === Changement de mot de passe
    // ============================

    @Override
    public void changePasswordAndClearFlag(long userId, String rawPassword) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId invalide.");
        }
        Objects.requireNonNull(rawPassword, "rawPassword");

        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("Mot de passe trop court (min 6 caractères).");
        }

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
            String msg = e.getMessage();
            throw new RuntimeException("AccountServiceSqlite.changePasswordAndClearFlag failed: " + msg, e);
        }
    }
}