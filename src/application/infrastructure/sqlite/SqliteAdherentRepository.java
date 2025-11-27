package application.infrastructure.sqlite;

import application.domain.core.personne.Adherent;
import application.domain.ports.repository.AdherentRepository;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation SQLite de {@link AdherentRepository} basée sur la table {@code PERSONNE}.
 *
 * Ici, on ne gère que les lignes où {@code isAdmin = 0} (côté domaine : les adhérents).
 */
public class SqliteAdherentRepository implements AdherentRepository {

    private static final String TABLE_NAME = "PERSONNE";

    // =============================
    // ========== CRUD =============
    // =============================

    @Override
    public Adherent save(Adherent adherent) {
        if (adherent == null) {
            throw new IllegalArgumentException("Adherent ne peut pas être null.");
        }
        validateAdherent(adherent);

        return (adherent.getId() == null) ? insert(adherent) : update(adherent);
    }

    @Override
    public Optional<Adherent> findById(Long id) {
        final String sql =
                "SELECT * FROM " + TABLE_NAME + " WHERE personne_id = ? AND isAdmin = 0";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapRowToAdherent(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("SqliteAdherentRepository.findById a échoué", e);
        }
    }

    @Override
    public List<Adherent> findAll() {
        final String sql =
                "SELECT * FROM " + TABLE_NAME + " WHERE isAdmin = 0 ORDER BY nom, prenom";

        List<Adherent> adherents = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                adherents.add(mapRowToAdherent(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("SqliteAdherentRepository.findAll a échoué", e);
        }

        return adherents;
    }

    @Override
    public void delete(Long id) {
        final String sql =
                "DELETE FROM " + TABLE_NAME + " WHERE personne_id = ? AND isAdmin = 0";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                System.err.println(
                        "Avertissement : suppression adhérent ID=" + id +
                        " sans ligne affectée (inexistant ou pas un adhérent)."
                );
            }

        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("foreign key") && msg.contains("constraint")) {
                throw new RuntimeException(
                        "Impossible de supprimer l'adhérent ID=" + id +
                        " : des emprunts ou autres données y sont rattachés.",
                        e
                );
            }
            throw new RuntimeException("SqliteAdherentRepository.delete a échoué", e);
        }
    }

    @Override
    public long count() {
        final String sql =
                "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE isAdmin = 0";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;

        } catch (SQLException e) {
            throw new RuntimeException("SqliteAdherentRepository.count a échoué", e);
        }
    }

    // ==========================================
    // ======== Requêtes spécifiques ============
    // ==========================================

    @Override
    public Optional<Adherent> findByEmail(String email) {
        final String sql =
                "SELECT * FROM " + TABLE_NAME + " WHERE email = ? AND isAdmin = 0";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapRowToAdherent(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("SqliteAdherentRepository.findByEmail a échoué", e);
        }
    }

    @Override
    public List<Adherent> findByNom(String nom) {
        final String sql =
                "SELECT * FROM " + TABLE_NAME +
                " WHERE LOWER(nom) LIKE LOWER(?) AND isAdmin = 0 ORDER BY nom, prenom";

        List<Adherent> adherents = new ArrayList<>();
        String pattern = "%" + (nom == null ? "" : nom.trim()) + "%";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    adherents.add(mapRowToAdherent(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("SqliteAdherentRepository.findByNom a échoué", e);
        }

        return adherents;
    }

    // =============================
    // ====== insert / update ======
    // =============================

    /**
     * Insère un nouvel adhérent.
     * Ici, on fixe :
     * - isAdmin = 0
     * - actif   = 1
     * - password_hash = hash d'un mot de passe temporaire.
     */
    private Adherent insert(Adherent adherent) {
        final String sql = """
            INSERT INTO PERSONNE(
                email,
                password_hash,
                nom,
                prenom,
                actif,
                isAdmin,
                created_at,
                telephone
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, adherent.getEmail());

            // ⚠ Mot de passe temporaire très simplifié, pour le TP uniquement.
            String temporaryPassword = "password";
            ps.setString(2, placeholderHash_DO_NOT_USE_IN_PRODUCTION(temporaryPassword));

            ps.setString(3, adherent.getNom());
            ps.setString(4, adherent.getPrenom());
            ps.setInt(5, 1);  // actif
            ps.setInt(6, 0);  // isAdmin = 0
            ps.setString(7, Instant.now().toString()); // created_at
            ps.setString(8, adherent.getTelephone());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Insertion adhérent : aucune ligne affectée.");
            }

            // Récupération de l'ID généré
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    adherent.setId(keys.getLong(1));
                } else {
                    throw new SQLException("Insertion adhérent : aucun ID généré.");
                }
            }

            return adherent;

        } catch (SQLException e) {
            handleSqlException(e, "insert");
            // handleSqlException lance toujours une RuntimeException
            return null;
        }
    }
    

    /**
     * Met à jour un adhérent existant (où isAdmin = 0).
     * Ne touche ni au mot de passe, ni au statut.
     */
    private Adherent update(Adherent adherent) {
        final String sql = """
            UPDATE PERSONNE
            SET email = ?, nom = ?, prenom = ?, telephone = ?
            WHERE personne_id = ? AND isAdmin = 0
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, adherent.getEmail());
            ps.setString(2, adherent.getNom());
            ps.setString(3, adherent.getPrenom());
            ps.setString(4, adherent.getTelephone());
            ps.setLong(5, adherent.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                System.err.println(
                        "Avertissement : mise à jour adhérent ID=" + adherent.getId() +
                        " sans ligne affectée (inexistant ou pas un adhérent)."
                );
            }

            return adherent;

        } catch (SQLException e) {
            handleSqlException(e, "update");
            return null;
        }
    }

    // =============================
    // ========== Mapping ==========
    // =============================

    /**
     * Transforme une ligne de la table PERSONNE en objet {@link Adherent}.
     */
    private Adherent mapRowToAdherent(ResultSet rs) throws SQLException {
        Adherent a = new Adherent();
        a.setId(rs.getLong("personne_id"));
        a.setNom(rs.getString("nom"));
        a.setPrenom(rs.getString("prenom"));
        a.setEmail(rs.getString("email"));
        a.setTelephone(rs.getString("telephone"));

        String createdAt = rs.getString("created_at");
        a.setDateInscription(parseLocalDateSafely(createdAt));
        return a;
    }

    // =============================
    // ========== Helpers ==========
    // =============================

    /** Validation très simple de l'adhérent. */
    private void validateAdherent(Adherent adherent) {
        if (adherent.getNom() == null || adherent.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide.");
        }
        if (adherent.getPrenom() == null || adherent.getPrenom().isBlank()) {
            throw new IllegalArgumentException("Le prénom ne peut pas être vide.");
        }
        if (adherent.getEmail() == null ||
            adherent.getEmail().isBlank() ||
            !adherent.getEmail().contains("@")) {
            throw new IllegalArgumentException("L'email est invalide.");
        }
    }

    /**
     * Gestion standardisée des erreurs SQL.
     * - Contrainte d'unicité sur l'email : message fonctionnel
     * - sinon : message technique plus générique
     */
    private void handleSqlException(SQLException e, String operation) {
        String msg = e.getMessage();
        String lower = msg == null ? "" : msg.toLowerCase();

        if (lower.contains("unique") && lower.contains("email")) {
            throw new RuntimeException("Un adhérent ou utilisateur avec cet email existe déjà.", e);
        }

        throw new RuntimeException(
                "SqliteAdherentRepository." + operation + " a échoué : " + msg,
                e
        );
    }

    /**
     * Convertit une chaîne ISO-8601 (ex: created_at) en {@link LocalDate}.
     * Si la conversion échoue, on retourne la date du jour (fallback simple).
     */

    private static LocalDate parseLocalDateSafely(String isoDateTimeString) {
        try {
            if (isoDateTimeString == null || isoDateTimeString.isBlank()) {
                return LocalDate.now();
            }
            return Instant.parse(isoDateTimeString)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
        } catch (Exception e) {
            System.err.println(
                    "Avertissement : impossible de parser la date '" +
                    isoDateTimeString + "', utilisation de la date du jour."
            );
            return LocalDate.now();
        }
    }
    
    

    /**
     * Hash ultra-simplifié utilisé uniquement pour ce projet.
     * À remplacer par BCrypt/Argon2 dans une vraie application.
     */
    private static String placeholderHash_DO_NOT_USE_IN_PRODUCTION(String clear) {
        return Integer.toHexString(clear == null ? 0 : clear.hashCode());
    }
}