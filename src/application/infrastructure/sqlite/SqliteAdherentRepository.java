package application.infrastructure.sqlite;

import application.domain.core.personne.Adherent;
import application.domain.ports.repository.AdherentRepository;

import java.sql.*;
import java.time.Instant; // <-- AJOUTÉ pour created_at
import java.time.LocalDate;
import java.time.ZoneOffset; // <-- AJOUTÉ pour convertir Instant en LocalDate
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation SQLite de {@link AdherentRepository} adossée à la table {@code PERSONNE}.
 * Gère uniquement les enregistrements où isAdmin = 0.
 */
public class SqliteAdherentRepository implements AdherentRepository {

    // Utilisation directe des noms de colonnes (moins de constantes à gérer)
    private static final String TABLE_NAME = "PERSONNE";

    // =============================
    // ========== CRUD =============
    // =============================

    /** {@inheritDoc} */
    @Override
    public Adherent save(Adherent adherent) {
        if (adherent == null) {
            throw new IllegalArgumentException("Adherent ne peut pas être null");
        }
        validateAdherent(adherent); // Valide les champs requis

        return (adherent.getId() == null) ? insert(adherent) : update(adherent);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Adherent> findById(Long id) {
        // CORRIGÉ: Ajout du filtre isAdmin = 0
        final String sql = "SELECT * FROM " + TABLE_NAME + " WHERE personne_id = ? AND isAdmin = 0";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRowToAdherent(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adherent.findById failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Adherent> findAll() {
        // CORRIGÉ: Ajout du filtre isAdmin = 0
        final String sql = "SELECT * FROM " + TABLE_NAME + " WHERE isAdmin = 0 ORDER BY nom, prenom";
        List<Adherent> adherents = new ArrayList<>();
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                adherents.add(mapRowToAdherent(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adherent.findAll failed", e);
        }
        return adherents;
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Long id) {
        // CORRIGÉ: Ajout du filtre isAdmin = 0
        // Attention: Vérifier les emprunts avant ou laisser la FK faire ?
        final String sql = "DELETE FROM " + TABLE_NAME + " WHERE personne_id = ? AND isAdmin = 0";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();
             if (affectedRows == 0) {
                 System.err.println("Avertissement: La suppression de l'adhérent ID=" + id + " n'a affecté aucune ligne (n'existait pas ou n'était pas un adhérent).");
             }
        } catch (SQLException e) {
            // Gérer les violations de FK si un emprunt existe
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key constraint failed")) {
                throw new RuntimeException("Impossible de supprimer l'adhérent ID=" + id + " car il a des emprunts associés.", e);
            }
            throw new RuntimeException("Adherent.delete failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long count() {
        // CORRIGÉ: Ajout du filtre isAdmin = 0
        final String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE isAdmin = 0";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Adherent.count failed", e);
        }
    }

    // ==========================================
    // ========== Requêtes spécifiques ============
    // ==========================================

    /** {@inheritDoc} */
    @Override
    public Optional<Adherent> findByEmail(String email) {
        // CORRIGÉ: Ajout du filtre isAdmin = 0
        final String sql = "SELECT * FROM " + TABLE_NAME + " WHERE email = ? AND isAdmin = 0";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRowToAdherent(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adherent.findByEmail failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Adherent> findByNom(String nom) {
        // CORRIGÉ: Ajout du filtre isAdmin = 0
        final String sql = "SELECT * FROM " + TABLE_NAME + " WHERE lower(nom) LIKE lower(?) AND isAdmin = 0 ORDER BY nom, prenom";
        List<Adherent> adherents = new ArrayList<>();
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + (nom == null ? "" : nom) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    adherents.add(mapRowToAdherent(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adherent.findByNom failed", e);
        }
        return adherents;
    }

    // =============================
    // ====== SQL: insert/update ===
    // =============================

    /**
     * Insère un nouvel adhérent. Définit isAdmin=0, actif=1, et génère un mot de passe temporaire.
     */
    private Adherent insert(Adherent adherent) {
        // CORRIGÉ: SQL utilise '?' pour tous les paramètres et inclut isAdmin, password_hash, created_at
        final String sql = """
            INSERT INTO PERSONNE
            (email, password_hash, nom, prenom, actif, isAdmin, created_at, telephone) 
            VALUES(?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // --- Bind des paramètres ---
            ps.setString(1, adherent.getEmail());
            // Utilisation d'un mot de passe par défaut (à remplacer !)
            String temporaryPassword = "password"; // CHANGEZ CECI !
            ps.setString(2, placeholderHash_DO_NOT_USE_IN_PRODUCTION(temporaryPassword));
            ps.setString(3, adherent.getNom());
            ps.setString(4, adherent.getPrenom());
            ps.setInt(5, 1); // actif = true
            ps.setInt(6, 0); // isAdmin = false pour Adherent
            ps.setString(7, Instant.now().toString()); // created_at
            ps.setString(8, adherent.getTelephone()); // telephone (peut être null)

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Échec de l'insertion de l'adhérent, aucune ligne affectée.");
            }

            // Récupère l'ID généré
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    adherent.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Échec de l'insertion de l'adhérent, aucun ID obtenu.");
                }
            }
            return adherent; // Retourne l'objet avec son ID

        } catch (SQLException e) {
            handleSqlException(e, "Adherent.insert");
            return null; // Ne sera jamais atteint si handleSqlException lance une RuntimeException
        }
    }

    /**
     * Met à jour un adhérent existant (où isAdmin=0).
     */
    private Adherent update(Adherent adherent) {
        // CORRIGÉ: Ajout de 'AND isAdmin = 0' pour la sécurité
        final String sql = """
            UPDATE PERSONNE SET email = ?, nom = ?, prenom = ?, telephone = ?
            WHERE personne_id = ? AND isAdmin = 0
            """;
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, adherent.getEmail());
            ps.setString(2, adherent.getNom());
            ps.setString(3, adherent.getPrenom());
            ps.setString(4, adherent.getTelephone());
            ps.setLong(5, adherent.getId());

            int affectedRows = ps.executeUpdate();
             if (affectedRows == 0) {
                 System.err.println("Avertissement: La mise à jour de l'adhérent ID=" + adherent.getId() + " n'a affecté aucune ligne (n'existait pas ou n'était pas un adhérent).");
             }
            return adherent;

        } catch (SQLException e) {
            handleSqlException(e, "Adherent.update");
            return null; // Ne sera jamais atteint
        }
    }

    // =============================
    // ========== Mapping ==========
    // =============================

    /** Convertit la ligne SQL en entité {@link Adherent}. */
    private Adherent mapRowToAdherent(ResultSet rs) throws SQLException {
        Adherent a = new Adherent();
        a.setId(rs.getLong("personne_id"));
        a.setNom(rs.getString("nom"));
        a.setPrenom(rs.getString("prenom"));
        a.setEmail(rs.getString("email"));
        a.setTelephone(rs.getString("telephone")); // Lit directement, gère le null

        // Utilise created_at pour dateInscription
        String createdAtStr = rs.getString("created_at");
        a.setDateInscription(parseLocalDateSafely(createdAtStr));

        // Note: On ne charge pas les emprunts ici pour éviter N+1
        return a;
    }

    // ===== Helpers =====

    /** Méthode de validation simple. */
    private void validateAdherent(Adherent adherent) {
        if (adherent.getNom() == null || adherent.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide.");
        }
        if (adherent.getPrenom() == null || adherent.getPrenom().isBlank()) {
            throw new IllegalArgumentException("Le prénom ne peut pas être vide.");
        }
        if (adherent.getEmail() == null || adherent.getEmail().isBlank() || !adherent.getEmail().contains("@")) {
            throw new IllegalArgumentException("L'email est invalide.");
        }
    }

    /** Gère les exceptions SQL courantes (Unique constraint) et relance. */
    private void handleSqlException(SQLException e, String operation) {
        String msg = e.getMessage();
        if (msg != null && msg.toLowerCase().contains("unique constraint failed") && msg.toLowerCase().contains("email")) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà.", e);
        }
        // Pour les autres erreurs SQL
        throw new RuntimeException("Échec de l'opération " + operation + ": " + msg, e);
    }

    /** Parse une date/datetime ISO string en LocalDate, avec fallback. */
    private static LocalDate parseLocalDateSafely(String isoDateTimeString) {
        try {
            if (isoDateTimeString == null || isoDateTimeString.isBlank()) return LocalDate.now(); // Ou null si préférable
            // Prend seulement la partie date avant le 'T' s'il existe
            return Instant.parse(isoDateTimeString).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (Exception e) {
            System.err.println("Avertissement: Impossible de parser la date '" + isoDateTimeString + "', utilisation de la date actuelle.");
            return LocalDate.now(); // Fallback sûr
        }
    }

    // --- N'oubliez pas le placeholder de hachage (à remplacer !) ---
    private static String placeholderHash_DO_NOT_USE_IN_PRODUCTION(String clear) {
        return Integer.toHexString(clear == null ? 0 : clear.hashCode());
    }
}