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
 * Rappels :
 * - le statut métier (DUE / PAID / CANCELLED / PAYMENT_REQUESTED / DEMANDE)
 *   est stocké dans la colonne "status".
 * - la date_fin marque la date de paiement (si payé).
 */
public class SqlitePenaliteRepository implements PenaliteRepository {

    /** Type par défaut de pénalité (retard d’emprunt). */
    private static final String DEFAULT_TYPE = "RETARD";

    public SqlitePenaliteRepository() {
        // Constructeur par défaut
    }

    // ==========================================
    // ========== CRUD (Repository<T,ID>) =======
    // ==========================================

    @Override
    public Penalite save(Penalite penalite) {
        Objects.requireNonNull(penalite, "La pénalité ne peut pas être nulle.");

        if (penalite.getMontant() == null || !penalite.getMontant().isPositive()) {
            throw new IllegalArgumentException("Le montant de la pénalité doit être strictement positif.");
        }

        return (penalite.getId() == null) ? insert(penalite) : update(penalite);
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
            throw new RuntimeException("PENALITE.findById a échoué", e);
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
                Penalite penalite = mapRowToPenalite(rs);
                if (penalite != null) {
                    results.add(penalite);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findAll a échoué", e);
        }

        return results;
    }

    @Override
    public void delete(Long id) {
        if (id == null || id <= 0) {
            return; // Rien à faire
        }

        final String sql = "DELETE FROM PENALITE WHERE penalite_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.delete a échoué", e);
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
            throw new RuntimeException("PENALITE.count a échoué", e);
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
                    Penalite penalite = mapRowToPenalite(rs);
                    if (penalite != null) {
                        results.add(penalite);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findByAdherentId a échoué", e);
        }

        return results;
    }

    // ==========================================
    // ========== Requêtes spécifiques ==========
    // ==========================================

    /**
     * Pénalités "actives" = non payées (date_fin IS NULL).
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
                    Penalite penalite = mapRowToPenalite(rs);
                    if (penalite != null) {
                        results.add(penalite);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findActivesByAdherentId a échoué", e);
        }

        return results;
    }

    /**
     * Retourne toutes les pénalités actives de la base (date_fin IS NULL).
     */
    public List<Penalite> findAllActives() {
        final String sql = "SELECT * FROM PENALITE WHERE date_fin IS NULL";
        List<Penalite> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Penalite penalite = mapRowToPenalite(rs);
                if (penalite != null) {
                    results.add(penalite);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.findAllActives a échoué", e);
        }

        return results;
    }

    /**
     * Carte {adherent_id -> nombre de pénalités actives}.
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
            throw new RuntimeException("PENALITE.findActivePenaltyCountsMap a échoué", e);
        }

        return counts;
    }

    /**
     * Marque toutes les pénalités actives d'un adhérent comme payées
     * (date_fin = datePaiement, status = 'PAID').
     */
    @Override
    public int reglerToutesPourAdherent(long adherentId, LocalDate datePaiement) {
        Objects.requireNonNull(datePaiement, "datePaiement ne doit pas être null.");

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

            return ps.executeUpdate(); // nombre de lignes mises à jour

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.reglerToutesPourAdherent a échoué", e);
        }
    }

    // =============================
    // ========== SQL I/O ==========
    // =============================

    /** Insertion d’une nouvelle pénalité. */
    private Penalite insert(Penalite penalite) {
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

            bind(ps, penalite);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    penalite.setId(keys.getLong(1));
                }
            }

            return penalite;

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.insert a échoué", e);
        }
    }

    /** Mise à jour d’une pénalité existante. */
    private Penalite update(Penalite penalite) {
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

            int i = bind(ps, penalite);
            ps.setLong(i, penalite.getId());
            ps.executeUpdate();
            return penalite;

        } catch (SQLException e) {
            throw new RuntimeException("PENALITE.update a échoué", e);
        }
    }

    /**
     * Renseigne tous les paramètres du PreparedStatement avec les champs d’une pénalité.
     *
     * @return l’index du prochain paramètre libre.
     */
    private int bind(PreparedStatement ps, Penalite penalite) throws SQLException {
        int i = 1;

        if (penalite.getAdherentId() == null) {
            throw new SQLException("adherentId null dans Penalite.bind");
        }
        if (penalite.getEmpruntId() == null) {
            throw new SQLException("empruntId null dans Penalite.bind");
        }

        // Identifiants
        ps.setLong(i++, penalite.getAdherentId());
        ps.setLong(i++, penalite.getEmpruntId());

        // Type (ici un seul type : RETARD, mais extensible)
        ps.setString(i++, DEFAULT_TYPE);

        // Montant
        BigDecimal amount = penalite.getMontant().amount();
        ps.setBigDecimal(i++, amount);

        // date_debut (dateGeneration)
        LocalDate dateGeneration = penalite.getDateGeneration();
        if (dateGeneration != null) {
            ps.setString(i++, dateGeneration.toString());
        } else {
            ps.setNull(i++, Types.VARCHAR);
        }

        // date_fin (datePaiement)
        LocalDate datePaiement = penalite.getDatePaiement();
        if (datePaiement != null) {
            ps.setString(i++, datePaiement.toString());
        } else {
            ps.setNull(i++, Types.VARCHAR);
        }

        // description (non utilisée pour l’instant)
        ps.setString(i++, null);

        // status (DUE / PAYMENT_REQUESTED / PAID / CANCELLED / DEMANDE)
        ps.setString(i++, toDbStatus(penalite));

        return i;
    }

    /**
     * Convertit le statut métier en string pour la DB.
     */
    private String toDbStatus(Penalite penalite) {
        PenaliteStatus status = penalite.getStatus();

        if (status != null) {
            return status.name();
        }

        // Si aucun statut explicite : PAID si datePaiement non null, sinon DUE.
        return (penalite.getDatePaiement() != null)
                ? PenaliteStatus.PAID.name()
                : PenaliteStatus.DUE.name();
    }

    // =============================
    // ========== Mapping ==========
    // =============================

    /**
     * Reconstruit une {@link Penalite} depuis une ligne SQL.
     *
     * Utilise le constructeur de persistance :
     *   (id, empruntId, adherentId, Money, dateGeneration, datePaiement),
     * puis applique le statut lu en base si possible.
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
            System.err.println("Pénalité orpheline ignorée (emprunt/adherent manquant), ID = " + id);
            return null;
        }

        BigDecimal montantBd = rs.getBigDecimal("montant");
        if (montantBd == null || montantBd.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Pénalité invalide (montant <= 0) ignorée : ID = " + id);
            return null;
        }

        LocalDate dateGeneration = parseLocalDate(rs.getString("date_debut"));
        LocalDate datePaiement   = parseLocalDate(rs.getString("date_fin"));

        if (dateGeneration == null) {
            System.err.println("Pénalité invalide (date_debut nulle) ignorée : ID = " + id);
            return null;
        }

        // Création "standard" (statut dérivé de la datePaiement : DUE / PAID)
        Penalite penalite = new Penalite(
                id,
                empruntId,
                adherentId,
                Money.euros(montantBd),
                dateGeneration,
                datePaiement
        );

        // Lecture du statut explicite en base (colonne status)
        String statusStr = rs.getString("status");
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                PenaliteStatus status = PenaliteStatus.valueOf(statusStr.toUpperCase());
                // Méthode à prévoir dans Penalite (ex. `overrideStatusFromPersistence`)
                penalite.overrideStatusFromPersistence(status);
            } catch (IllegalArgumentException ex) {
                // Valeur inconnue en DB -> on garde le statut déduit (DUE/PAID)
                System.err.println("Statut de pénalité inconnu '" + statusStr + "' pour ID = " + id);
            }
        }

        return penalite;
    }

    /** Parse une date ISO (YYYY-MM-DD) en LocalDate, ou null si vide. */
    private static LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // Format simple (YYYY-MM-DD)
            if (value.length() == 10) {
                return LocalDate.parse(value);
            }

            // Format ISO complet avec Z ou offset
            return java.time.Instant.parse(value)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate();

        } catch (Exception e1) {
            try {
                // Format ISO local (sans Z)
                return java.time.LocalDateTime.parse(value)
                        .toLocalDate();
            } catch (Exception e2) {
                System.err.println("[WARN] Impossible de parser date: '" + value + "'");
                return null;
            }
        }
    }
}