package application.infrastructure.sqlite;

import application.domain.service.AccountService;
import application.ui.navigation.Navigator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant; // <-- CORRIGÉ : Ré-importé car 'created_at' est NOT NULL

/**
 * Implémentation de AccountService pour SQLite (Admin uniquement).
 *
 * <p>Cette classe utilise un schéma simplifié (table PERSONNE avec 'isAdmin')
 * et la source de données centralisée (SqliteDataSource).</p>
 */
public final class AccountServiceSqlite implements AccountService {

    private final Navigator navigator;

    // ---- Constructeurs ----
    public AccountServiceSqlite() { this(null); }

    public AccountServiceSqlite(Navigator navigator) {
        this.navigator = navigator;
        // L'initialisation du schéma est gérée par DatabaseInitializer
    }

    // ============== Helpers (Sécurité) ==============
    
    // (Vos placeholders de hachage restent ici - N'oubliez pas de les remplacer !)
    private static String placeholderHash_DO_NOT_USE_IN_PRODUCTION(String clear) {
        // TODO: Implémenter BCrypt
        return Integer.toHexString(clear == null ? 0 : clear.hashCode());
    }

    private static boolean placeholderCheck_DO_NOT_USE_IN_PRODUCTION(String clear, String dbHash) {
        // TODO: Implémenter BCrypt
        return dbHash.equals(placeholderHash_DO_NOT_USE_IN_PRODUCTION(clear));
    }


    // ================================== API ==================================

    @Override
    public long countUsers() {
        // Cette requête reste la même
        final String sql = "SELECT COUNT(*) FROM PERSONNE";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("countUsers failed: " + e.getMessage(), e);
        }
    }

    /**
     * CORRIGÉ : Signature mise à jour pour inclure nom et prenom.
     * La requête SQL inclut désormais tous les champs NOT NULL.
     */
    @Override
    public void createAdmin(String email, String password, String nom, String prenom) {
        // Validation des nouveaux champs
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email vide");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Mot de passe trop court");
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Nom vide");
        if (prenom == null || prenom.isBlank()) throw new IllegalArgumentException("Prénom vide");


        // CORRIGÉ : SQL mis à jour pour inclure tous les champs NOT NULL
        final String sql = """
            INSERT INTO PERSONNE
            (email, password_hash, nom, prenom, isAdmin, actif, created_at) 
            VALUES(?, ?, ?, ?, 1, 1, ?)
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, email.trim());
            ps.setString(2, placeholderHash_DO_NOT_USE_IN_PRODUCTION(password));
            ps.setString(3, nom.trim()); // <-- CORRIGÉ
            ps.setString(4, prenom.trim()); // <-- CORRIGÉ
            ps.setString(5, Instant.now().toString()); // <-- CORRIGÉ (pour created_at)
            
            ps.executeUpdate();

        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("unique")) {
                throw new RuntimeException("Un administrateur avec cet email existe déjà.");
            }
            throw new RuntimeException("createAdmin failed: " + msg, e);
        }
    }


    /**
     * CORRIGÉ : Logique de connexion simplifiée pour 'Admin uniquement'.
     */
    @Override
    public LoginResult login(String email, String password) {
        
        final String sql = """
            SELECT
                password_hash,
                actif,
                isAdmin
            FROM
                PERSONNE
            WHERE
                email = ?
            """; 

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email == null ? "" : email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    placeholderCheck_DO_NOT_USE_IN_PRODUCTION(password, "dummy_hash_value");
                    return LoginResult.failure("Email ou mot de passe invalide.");
                }

                String dbHash = rs.getString("password_hash");
                boolean actif = rs.getInt("actif") == 1;
                boolean admin = rs.getInt("isAdmin") == 1;

                if (!placeholderCheck_DO_NOT_USE_IN_PRODUCTION(password, dbHash)) {
                    return LoginResult.failure("Email ou mot de passe invalide.");
                }
                if (!actif) {
                    return LoginResult.failure("Ce compte a été désactivé.");
                }
                if (!admin) {
                    return LoginResult.failure("Ce compte n'est pas un administrateur.");
                }
                
                return LoginResult.success(email);
            }
        } catch (SQLException e) {
            throw new RuntimeException("login failed: " + e.getMessage(), e);
        }
    }

    /**
     * CORRIGÉ : Le LoginResult n'a plus de rôle, mais le navigateur
     * (FxNavigator) en a besoin. On le hardcode ici.
     */
    @Override
    public void navigateToMainAfterLogin(LoginResult result) {
        if (result != null && result.success() && navigator != null) {
            navigator.showShell();
            navigator.goToDashboard(result.username(), "Admin"); 
        }
    }
}