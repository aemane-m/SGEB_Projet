package application.infrastructure.sqlite;

// Imports Java
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Imports Domaine
import application.domain.core.emprunt.Money;
import application.domain.core.emprunt.Penalite;
import application.domain.ports.repository.PenaliteRepository;

/**
 * Implémentation SQLite de {@link PenaliteRepository}.
 *
 * CORRIGÉ :
 * - Utilise la colonne 'adherent_id' directement (confirmée par DatabaseInitializer).
 * - Supprime les JOINs inutiles dans 'findActivesByAdherentId' et 'findActivePenaltyCountsMap'.
 * - Implémente l'interface 'Repository<Penalite, Long>' complète.
 * - Nettoyage des méthodes de mapping dupliquées.
 */
public class SqlitePenaliteRepository implements PenaliteRepository {

    private static final String DEFAULT_TYPE = "RETARD";

    public SqlitePenaliteRepository() {
        // Constructeur
    }

    // ==========================================
    // ========== CRUD (Interface Repository) ====
    // ==========================================

    @Override
    public Penalite save(Penalite p) {
        if (p == null) throw new NullPointerException("La pénalité ne peut être nulle.");
        // Valide la logique métier (ex: montant positif)
        if (p.getMontant() == null || !p.getMontant().isPositive()) {
            throw new IllegalArgumentException("Le montant de la pénalité doit être positif.");
        }
        return (p.getId() == null) ? insert(p) : update(p);
    }

    @Override
    public Optional<Penalite> findById(Long id) {
        final String sql = "SELECT * FROM PENALITE WHERE penalite_id = ?";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(mapRowToPenalite(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findById", e);
        }
    }

    @Override
    public List<Penalite> findAll() {
        final String sql = "SELECT * FROM PENALITE";
        List<Penalite> results = new ArrayList<>();
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Penalite p = mapRowToPenalite(rs);
                if (p != null) results.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findAll", e);
        }
        return results;
    }

    @Override
    public void delete(Long id) {
        final String sql = "DELETE FROM PENALITE WHERE penalite_id=?";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.delete", e);
        }
    }

    @Override
    public long count() {
        final String sql = "SELECT COUNT(*) FROM PENALITE";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.count", e);
        }
    }

    // ==========================================
    // ========== Requêtes Spécifiques ==========
    // ==========================================

    /**
     * {@inheritDoc}
     * CORRIGÉ : N'utilise plus de JOIN, la table PENALITE a l'adherent_id.
     */
    @Override
    public List<Penalite> findActivesByAdherentId(Long adherentId) {
        final String sql = "SELECT * FROM PENALITE WHERE adherent_id = ? AND date_fin IS NULL";
        List<Penalite> results = new ArrayList<>();
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, adherentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToPenalite(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Penalite.findActivesByAdherentId failed", e);
        }
        return results;
    }

    /**
     * NOUVEAU : Méthode pour récupérer toutes les pénalités actives
     * (requise par PenalitesViewModel).
     */
    public List<Penalite> findAllActives() {
        final String sql = "SELECT * FROM PENALITE WHERE date_fin IS NULL";
        List<Penalite> results = new ArrayList<>();
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Penalite p = mapRowToPenalite(rs);
                if (p != null) results.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findAllActives", e);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * CORRIGÉ : N'utilise plus de JOIN, la table PENALITE a l'adherent_id.
     */
    @Override
    public Map<Long, Integer> findActivePenaltyCountsMap() {
        final String sql = """
            SELECT adherent_id, COUNT(penalite_id) as count
            FROM PENALITE
            WHERE date_fin IS NULL
            GROUP BY adherent_id
            """;
        Map<Long, Integer> counts = new HashMap<>();
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                counts.put(rs.getLong("adherent_id"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Penalite.findActivePenaltyCountsMap failed", e);
        }
        return counts;
    }
    
    /**
     * NOUVEAU : Implémentation de la requête UPDATE pour tout régler.
     */
    @Override
    public int reglerToutesPourAdherent(long adherentId, LocalDate datePaiement) {
        final String sql = "UPDATE PENALITE SET date_fin = ? WHERE adherent_id = ? AND date_fin IS NULL";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, datePaiement.toString());
            ps.setLong(2, adherentId);
            
            return ps.executeUpdate(); // Retourne le nombre de lignes modifiées
            
        } catch (SQLException e) {
            throw new RuntimeException("Penalite.reglerToutesPourAdherent failed", e);
        }
    }

    // =============================
    // ========== SQL IO ===========
    // =============================

    private Penalite insert(Penalite p) {
        // Le schéma (DatabaseInitializer) confirme ces colonnes
        final String sql = """
            INSERT INTO PENALITE(adherent_id, emprunt_id, type, montant, date_debut, date_fin, description)
            VALUES(?,?,?,?,?,?,?)
            """;
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) p.setId(k.getLong(1));
            }
            return p;
        } catch (SQLException ex) {
            throw new RuntimeException("PENALITE.insert", ex);
        }
    }

    private Penalite update(Penalite p) {
        final String sql = """
            UPDATE PENALITE
               SET adherent_id=?, emprunt_id=?, type=?, montant=?, date_debut=?, date_fin=?, description=?
             WHERE penalite_id=?
            """;
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int i = bind(ps, p);
            ps.setLong(i, p.getId());
            ps.executeUpdate();
            return p;
        } catch (SQLException ex) {
            throw new RuntimeException("PENALITE.update", ex);
        }
    }

    /** Remplit un PreparedStatement avec les champs d'une Pénalité. */
    private int bind(PreparedStatement ps, Penalite p) throws SQLException {
        int i = 1;
        ps.setLong(i++, p.getAdherentId());
        ps.setLong(i++, p.getEmpruntId());
        ps.setString(i++, DEFAULT_TYPE);
        ps.setBigDecimal(i++, p.getMontant().amount());
        ps.setString(i++, p.getDateGeneration() != null ? p.getDateGeneration().toString() : null); // date_debut
        ps.setString(i++, p.getDatePaiement() != null ? p.getDatePaiement().toString() : null);   // date_fin
        ps.setString(i++, null); // description
        return i;
    }

    // =============================
    // ========== Mapping ==========
    // =============================
    
    /**
     * Mappeur unique et correct.
     * Reconstruit une {@link Penalite} depuis une ligne SQL.
     */
    private Penalite mapRowToPenalite(ResultSet rs) throws SQLException {
        Long id = rs.getLong("penalite_id");
        Long empruntId = rs.getLong("emprunt_id");
        Long adherentId = rs.getLong("adherent_id"); // Confirmé par DatabaseInitializer
        
        BigDecimal bd = rs.getBigDecimal("montant");
        double montantDouble = (bd != null ? bd : BigDecimal.ZERO).doubleValue();

        if (montantDouble <= 0) {
            System.err.println("Pénalité invalide (montant <= 0) ignorée : ID=" + id);
            return null; // Ignore la donnée corrompue
        }

        LocalDate dateGeneration = parseLocalDate(rs.getString("date_debut"));
        LocalDate datePaiement = parseLocalDate(rs.getString("date_fin"));
        
        if (dateGeneration == null) {
             System.err.println("Pénalité invalide (date_debut nulle) ignorée : ID=" + id);
            return null; // Donnée corrompue
        }

        // Utilise le constructeur de persistance de l'entité Penalite
        return new Penalite(
            id,
            empruntId,
            adherentId,
            Money.euros(montantDouble),
            dateGeneration,
            datePaiement
        );
    }
    
    private static LocalDate parseLocalDate(String dateString) {
        return (dateString == null || dateString.isBlank()) ? null : LocalDate.parse(dateString);
    }
}

