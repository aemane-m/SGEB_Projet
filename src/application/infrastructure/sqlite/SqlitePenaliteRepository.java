package application.infrastructure.sqlite;

import application.domain.core.emprunt.Money;
import application.domain.core.emprunt.Penalite;
import application.domain.core.emprunt.PenaliteStatus;
import application.domain.ports.repository.PenaliteRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Implémentation SQLite de {@link PenaliteRepository}.
 *
 * Schéma SQL (cf. DatabaseInitializer) :
 *   PENALITE(
 *      penalite_id INTEGER PK AUTOINCREMENT,
 *      adherent_id INTEGER NOT NULL,
 *      emprunt_id  INTEGER NOT NULL,
 *      type        TEXT,
 *      montant     REAL NOT NULL,
 *      date_debut  TEXT,
 *      date_fin    TEXT,
 *      description TEXT,
 *      status      TEXT NOT NULL DEFAULT 'DUE'
 *   )
 *
 * Désormais :
 * - le statut métier (DUE / PAID / CANCELLED / PAYMENT_REQUESTED / DEMANDE)
 *   est lu/écrit dans la colonne "status".
 * - la date_fin continue à marquer la date de paiement.
 */
public class SqlitePenaliteRepository implements PenaliteRepository {

    private static final String DEFAULT_TYPE = "RETARD";

    public SqlitePenaliteRepository() {
        // Constructeur par défaut
    }

    // ==========================================
    // ========== CRUD (Interface Repository) ===
    // ==========================================

    @Override
    public Penalite save(Penalite p) {
        Objects.requireNonNull(p, "La pénalité ne peut être nulle.");
        if (p.getMontant() == null || !p.getMontant().isPositive()) {
            throw new IllegalArgumentException("Le montant de la pénalité doit être positif.");
        }
        return (p.getId() == null) ? insert(p) : update(p);
    }

    @Override
    public Optional<Penalite> findById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        final String sql = "SELECT * FROM PENALITE WHERE penalite_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.ofNullable(mapRowToPenalite(rs))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findById failed", e);
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
                if (p != null) {
                    results.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findAll failed", e);
        }
        return results;
    }

    @Override
    public void delete(Long id) {
        if (id == null || id <= 0) {
            return;
        }
        final String sql = "DELETE FROM PENALITE WHERE penalite_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.delete failed", e);
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
            throw new RuntimeException("PENALITE.count failed", e);
        }
    }

    @Override
    public List<Penalite> findByAdherentId(long adherentId) {
        if (adherentId <= 0) {
            return List.of();
        }

        final String sql = "SELECT * FROM PENALITE WHERE adherent_id = ?";

        List<Penalite> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, adherentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Penalite p = mapRowToPenalite(rs);
                    if (p != null) {
                        results.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findByAdherentId failed", e);
        }

        return results;
    }

    // ==========================================
    // ========== Requêtes spécifiques ==========
    // ==========================================

    /**
     * Active = non payée en base, i.e. date_fin IS NULL.
     */
    @Override
    public List<Penalite> findActivesByAdherentId(Long adherentId) {
        if (adherentId == null || adherentId <= 0) {
            return List.of();
        }
        final String sql = "SELECT * FROM PENALITE WHERE adherent_id = ? AND date_fin IS NULL";

        List<Penalite> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, adherentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Penalite p = mapRowToPenalite(rs);
                    if (p != null) {
                        results.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findActivesByAdherentId failed", e);
        }
        return results;
    }

    /**
     * Retourne toutes les pénalités actives (date_fin IS NULL).
     */
    public List<Penalite> findAllActives() {
        final String sql = "SELECT * FROM PENALITE WHERE date_fin IS NULL";
        List<Penalite> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Penalite p = mapRowToPenalite(rs);
                if (p != null) {
                    results.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findAllActives failed", e);
        }
        return results;
    }

    /**
     * Carte {adherent_id -> nb de pénalités actives (date_fin IS NULL)}.
     */
    @Override
    public Map<Long, Integer> findActivePenaltyCountsMap() {
        final String sql = """
            SELECT adherent_id, COUNT(penalite_id) AS count
              FROM PENALITE
             WHERE date_fin IS NULL
             GROUP BY adherent_id
            """;

        Map<Long, Integer> counts = new HashMap<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long adherentId = rs.getLong("adherent_id");
                int count = rs.getInt("count");
                counts.put(adherentId, count);
            }
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findActivePenaltyCountsMap failed", e);
        }
        return counts;
    }

    /**
     * Marque toutes les pénalités actives d'un adhérent comme payées (date_fin = datePaiement).
     */
    @Override
    public int reglerToutesPourAdherent(long adherentId, LocalDate datePaiement) {
        Objects.requireNonNull(datePaiement, "datePaiement");
        final String sql = """
            UPDATE PENALITE
               SET date_fin = ?,
                   status   = 'PAID'
             WHERE adherent_id = ?
               AND date_fin IS NULL
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, datePaiement.toString());
            ps.setLong(2, adherentId);

            return ps.executeUpdate(); // Nombre de lignes modifiées
        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.reglerToutesPourAdherent failed", e);
        }
    }

    // =============================
    // ========== SQL IO ===========
    // =============================

    private Penalite insert(Penalite p) {
        // Schéma : adherent_id, emprunt_id, type, montant, date_debut, date_fin, description, status
        final String sql = """
            INSERT INTO PENALITE(
                adherent_id,
                emprunt_id,
                type,
                montant,
                date_debut,
                date_fin,
                description,
                status
            ) VALUES (?,?,?,?,?,?,?,?)
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(ps, p);
            ps.executeUpdate();

            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    p.setId(k.getLong(1));
                }
            }
            return p;

        } catch (SQLException ex) {
            throw new RuntimeException("PENALITE.insert failed", ex);
        }
    }

    private Penalite update(Penalite p) {
        final String sql = """
            UPDATE PENALITE
               SET adherent_id = ?,
                   emprunt_id  = ?,
                   type        = ?,
                   montant     = ?,
                   date_debut  = ?,
                   date_fin    = ?,
                   description = ?,
                   status      = ?
             WHERE penalite_id = ?
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int i = bind(ps, p);
            ps.setLong(i, p.getId());
            ps.executeUpdate();
            return p;
        } catch (SQLException ex) {
            throw new RuntimeException("PENALITE.update failed", ex);
        }
    }

    /**
     * Remplit un PreparedStatement avec les champs d'une Pénalité.
     * @return l'indice suivant à utiliser dans le PreparedStatement.
     */
    private int bind(PreparedStatement ps, Penalite p) throws SQLException {
        int i = 1;

        if (p.getAdherentId() == null) {
            throw new SQLException("adherentId null dans Penalite.bind");
        }
        if (p.getEmpruntId() == null) {
            throw new SQLException("empruntId null dans Penalite.bind");
        }

        ps.setLong(i++, p.getAdherentId());
        ps.setLong(i++, p.getEmpruntId());
        ps.setString(i++, DEFAULT_TYPE);

        // Montant
        BigDecimal amount = p.getMontant().amount();
        ps.setBigDecimal(i++, amount);

        // date_debut (dateGeneration)
        LocalDate dateGeneration = p.getDateGeneration();
        if (dateGeneration != null) {
            ps.setString(i++, dateGeneration.toString());
        } else {
            ps.setNull(i++, Types.VARCHAR);
        }

        // date_fin (datePaiement)
        LocalDate datePaiement = p.getDatePaiement();
        if (datePaiement != null) {
            ps.setString(i++, datePaiement.toString());
        } else {
            ps.setNull(i++, Types.VARCHAR);
        }

        // description (null par défaut pour l'instant)
        ps.setString(i++, null);

        // status (DUE / PAYMENT_REQUESTED / PAID / CANCELLED / DEMANDE)
        ps.setString(i++, toDbStatus(p));

        return i;
    }

    private String toDbStatus(Penalite p) {
        PenaliteStatus st = p.getStatus();
        if (st != null) {
            return st.name();
        }
        // Fallback : si pas de statut explicite, on déduit depuis datePaiement
        return (p.getDatePaiement() != null) ? PenaliteStatus.PAID.name() : PenaliteStatus.DUE.name();
    }

    // =============================
    // ========== Mapping ==========
    // =============================

    /**
     * Reconstruit une {@link Penalite} depuis une ligne SQL.
     * Utilise le constructeur de persistance :
     * (id, empruntId, adherentId, Money, dateGeneration, datePaiement)
     * puis applique, si possible, le statut lu en base.
     */
    private Penalite mapRowToPenalite(ResultSet rs) throws SQLException {
        Long id = rs.getLong("penalite_id");
        if (rs.wasNull()) {
            id = null;
        }

        Long empruntId = rs.getLong("emprunt_id");
        boolean empruntIdWasNull = rs.wasNull();

        Long adherentId = rs.getLong("adherent_id");
        boolean adherentIdWasNull = rs.wasNull();

        if (empruntIdWasNull || adherentIdWasNull) {
            System.err.println("Pénalité orpheline ignorée (emprunt/adherent manquant), ID=" + id);
            return null;
        }

        BigDecimal montantBd = rs.getBigDecimal("montant");
        if (montantBd == null || montantBd.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Pénalité invalide (montant <= 0) ignorée : ID=" + id);
            return null;
        }

        LocalDate dateGeneration = parseLocalDate(rs.getString("date_debut"));
        LocalDate datePaiement   = parseLocalDate(rs.getString("date_fin"));

        if (dateGeneration == null) {
            System.err.println("Pénalité invalide (date_debut nulle) ignorée : ID=" + id);
            return null;
        }

        // Création "standard" (déduira DUE / PAID depuis datePaiement)
        Penalite p = new Penalite(
                id,
                empruntId,
                adherentId,
                Money.euros(montantBd),
                dateGeneration,
                datePaiement
        );

        // Lecture éventuelle du status en base
        String statusStr = rs.getString("status");
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                PenaliteStatus status = PenaliteStatus.valueOf(statusStr.toUpperCase());
                // Méthode à ajouter dans Penalite (voir plus bas)
                p.overrideStatusFromPersistence(status);
            } catch (IllegalArgumentException ex) {
                // Valeur inconnue en base : on garde le statut déduit (DUE/PAID)
                System.err.println("Statut de pénalité inconnu '" + statusStr + "' pour ID=" + id);
            }
        }

        return p;
    }

    private static LocalDate parseLocalDate(String dateString) {
        return (dateString == null || dateString.isBlank())
                ? null
                : LocalDate.parse(dateString);
    }
}