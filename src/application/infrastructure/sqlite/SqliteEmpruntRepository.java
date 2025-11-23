package application.infrastructure.sqlite;

import application.domain.core.document.CD;
import application.domain.core.document.Document;
import application.domain.core.document.Livre;
import application.domain.core.document.Magazine;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.domain.core.personne.Adherent;
import application.domain.ports.repository.EmpruntRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation SQLite de {@link EmpruntRepository}.
 *
 * <p>Ce dépôt lit et écrit dans la table {@code EMPRUNT} et utilise des
 * LEFT JOIN avec {@code DOCUMENT} et {@code PERSONNE} pour récupérer
 * en une fois les infos utiles du document et de l’adhérent.</p>
 *
 * <p>Important : le statut métier de l’emprunt (en retard, en cours, etc.)
 * est calculé dans l’entité {@link Emprunt} à partir des dates,
 * il n’est pas stocké directement en base.</p>
 */
public class SqliteEmpruntRepository implements EmpruntRepository {

    /**
     * Requête de base : joint EMPRUNT, DOCUMENT et PERSONNE.
     * On pourra simplement rajouter un WHERE ou ORDER BY derrière.
     */
    private static final String SELECT_JOIN_SQL = """
        SELECT e.*,
               d.dtype,
               d.titre       AS docTitre,
               d.cover_path  AS docCoverRef,
               p.nom         AS adhNom,
               p.prenom      AS adhPrenom
          FROM EMPRUNT e
          LEFT JOIN DOCUMENT d ON e.document_id = d.document_id
          LEFT JOIN PERSONNE p ON e.emprunteur_id = p.personne_id
        """;

    public SqliteEmpruntRepository() {
        // Constructeur par défaut
    }

    // =============================
    // ========== CRUD =============
    // =============================

    @Override
    public Emprunt save(Emprunt emprunt) {
        if (emprunt == null) {
            throw new IllegalArgumentException("L'emprunt ne doit pas être null.");
        }
        if (emprunt.getDocument() == null || emprunt.getDocument().getId() == null) {
            throw new IllegalArgumentException("Le document de l'emprunt doit avoir un ID.");
        }
        if (emprunt.getEmprunteur() == null || emprunt.getEmprunteur().getId() == null) {
            throw new IllegalArgumentException("L'emprunteur de l'emprunt doit avoir un ID.");
        }

        return (emprunt.getId() == null) ? insert(emprunt) : update(emprunt);
    }

    @Override
    public Optional<Emprunt> findById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        final String sql = SELECT_JOIN_SQL + " WHERE e.emprunt_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.ofNullable(mapRowToEmprunt(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findById a échoué", e);
        }
    }

    @Override
    public List<Emprunt> findAll() {
        final String sql = SELECT_JOIN_SQL + " ORDER BY e.date_emprunt DESC";
        List<Emprunt> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Emprunt emprunt = mapRowToEmprunt(rs);
                if (emprunt != null) {
                    results.add(emprunt);
                }
            }
            return results;

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findAll a échoué", e);
        }
    }

    @Override
    public void delete(Long id) {
        if (id == null || id <= 0) {
            return; // Rien à faire
        }

        final String sql = "DELETE FROM EMPRUNT WHERE emprunt_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.delete a échoué", e);
        }
    }

    @Override
    public long count() {
        final String sql = "SELECT COUNT(*) FROM EMPRUNT";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.count a échoué", e);
        }
    }

    // ==========================================
    // ========== Requêtes spécifiques ==========
    // ==========================================

    @Override
    public List<Emprunt> findByAdherentId(Long adherentId) {
        if (adherentId == null || adherentId <= 0) {
            return List.of();
        }

        final String sql = SELECT_JOIN_SQL
                + " WHERE e.emprunteur_id = ?"
                + " ORDER BY e.date_emprunt DESC";

        List<Emprunt> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, adherentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Emprunt emprunt = mapRowToEmprunt(rs);
                    if (emprunt != null) {
                        results.add(emprunt);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findByAdherentId a échoué", e);
        }

        return results;
    }

    @Override
    public List<Emprunt> findByDocumentId(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return List.of();
        }

        final String sql = SELECT_JOIN_SQL
                + " WHERE e.document_id = ?"
                + " ORDER BY e.date_emprunt DESC";

        List<Emprunt> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, documentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Emprunt emprunt = mapRowToEmprunt(rs);
                    if (emprunt != null) {
                        results.add(emprunt);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findByDocumentId a échoué", e);
        }

        return results;
    }

    @Override
    public List<Emprunt> findActiveEmprunts() {
        final String sql = SELECT_JOIN_SQL
                + " WHERE e.date_retour_effective IS NULL"
                + " ORDER BY e.date_emprunt";

        List<Emprunt> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Emprunt emprunt = mapRowToEmprunt(rs);
                if (emprunt != null) {
                    results.add(emprunt);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findActiveEmprunts a échoué", e);
        }

        return results;
    }

    @Override
    public List<Emprunt> findLateEmprunts(LocalDate dateLimite) {
        if (dateLimite == null) {
            return List.of();
        }

        final String sql = SELECT_JOIN_SQL + """
            WHERE e.date_retour_effective IS NULL
              AND e.date_retour_prevue < ?
            ORDER BY e.date_retour_prevue ASC
            """;

        List<Emprunt> results = new ArrayList<>();

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, dateLimite.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Emprunt emprunt = mapRowToEmprunt(rs);
                    if (emprunt != null) {
                        results.add(emprunt);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findLateEmprunts a échoué", e);
        }

        return results;
    }

    @Override
    public boolean isDocumentEmprunte(long documentId) {
        // Ici, on n’a pas besoin de JOIN : on veut juste savoir si un prêt actif existe.
        final String sql = """
            SELECT COUNT(*)
              FROM EMPRUNT
             WHERE document_id = ?
               AND date_retour_effective IS NULL
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, documentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.isDocumentEmprunte a échoué", e);
        }
    }

    // =============================
    // ========== SQL I/O ==========
    // =============================

    /** Insertion d’un nouvel emprunt. */
    private Emprunt insert(Emprunt emprunt) {
        final String sql = """
            INSERT INTO EMPRUNT(
                document_id,
                emprunteur_id,
                date_emprunt,
                date_retour_prevue,
                date_retour_effective
            ) VALUES (?,?,?,?,?)
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindParameters(ps, emprunt);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    emprunt.setId(keys.getLong(1));
                }
            }
            return emprunt;

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.insert a échoué", e);
        }
    }

    /** Mise à jour d’un emprunt existant. */
    private Emprunt update(Emprunt emprunt) {
        final String sql = """
            UPDATE EMPRUNT
               SET document_id           = ?,
                   emprunteur_id         = ?,
                   date_emprunt          = ?,
                   date_retour_prevue    = ?,
                   date_retour_effective = ?
             WHERE emprunt_id            = ?
            """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int i = bindParameters(ps, emprunt);
            ps.setLong(i, emprunt.getId());
            ps.executeUpdate();
            return emprunt;

        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.update a échoué", e);
        }
    }

    /**
     * Lie les paramètres communs pour INSERT et UPDATE
     * (sans l’ID de l’emprunt).
     *
     * @return l’index du prochain paramètre libre.
     */
    private int bindParameters(PreparedStatement ps, Emprunt e) throws SQLException {
        int i = 1;

        if (e.getDocument() == null || e.getDocument().getId() == null) {
            throw new SQLException("Document (ou son ID) est null dans bindParameters.");
        }
        if (e.getEmprunteur() == null || e.getEmprunteur().getId() == null) {
            throw new SQLException("Emprunteur (ou son ID) est null dans bindParameters.");
        }

        ps.setLong(i++, e.getDocument().getId());
        ps.setLong(i++, e.getEmprunteur().getId());
        ps.setString(i++, e.getDateEmprunt().toString());
        ps.setString(i++, e.getDateRetourPrevue().toString());

        if (e.getDateRetourEffective() != null) {
            ps.setString(i++, e.getDateRetourEffective().toString());
        } else {
            ps.setNull(i++, Types.VARCHAR);
        }

        return i;
    }

    // =============================
    // ===== Mapping résultat ======
    // =============================

    /**
     * Transforme une ligne SQL (avec JOIN) en objet {@link Emprunt}.
     *
     * <p>On crée des objets simples pour le document et l’adhérent :
     * juste assez remplis pour l’affichage (id, titre, nom/prénom, coverRef).</p>
     */
    private Emprunt mapRowToEmprunt(ResultSet rs) throws SQLException {
        Long empruntId = rs.getLong("emprunt_id");
        if (rs.wasNull()) {
            empruntId = null;
        }

        Long docId = rs.getLong("document_id");
        boolean docIdWasNull = rs.wasNull();

        Long adhId = rs.getLong("emprunteur_id");
        boolean adhIdWasNull = rs.wasNull();

        String dtype      = rs.getString("dtype");
        String docTitre   = rs.getString("docTitre");
        String docCoverRef= rs.getString("docCoverRef");
        String adhNom     = rs.getString("adhNom");
        String adhPrenom  = rs.getString("adhPrenom");

        // Sécurité : si les clés étrangères sont manquantes
        if (docIdWasNull || adhIdWasNull) {
            System.err.println("Attention : Emprunt orphelin ignoré (ID=" + empruntId + ")");
            return null;
        }

        // --- Création d’un Document "léger" selon le dtype ---
        Document docProxy;
        if ("Magazine".equalsIgnoreCase(dtype)) {
            docProxy = new Magazine();
        } else if ("CD".equalsIgnoreCase(dtype)) {
            docProxy = new CD();
        } else {
            // Par défaut : livre (ou dtype null)
            docProxy = new Livre();
        }
        docProxy.setId(docId);
        docProxy.setTitre(docTitre);
        docProxy.setCoverRef(docCoverRef);

        // --- Création d’un Adherent "léger" ---
        Adherent adherentProxy = new Adherent();
        adherentProxy.setId(adhId);
        adherentProxy.setNom(adhNom);
        adherentProxy.setPrenom(adhPrenom);

        // --- Dates ---
        LocalDate dateEmprunt         = parseLocalDate(rs.getString("date_emprunt"));
        LocalDate dateRetourPrevue    = parseLocalDate(rs.getString("date_retour_prevue"));
        LocalDate dateRetourEffective = parseLocalDate(rs.getString("date_retour_effective"));

        if (dateEmprunt == null || dateRetourPrevue == null) {
            System.err.println("Attention : Emprunt avec dates invalides ignoré (ID=" + empruntId + ")");
            return null;
        }

        // On ne charge pas de pénalité ici (pour éviter de multiplier les JOIN).
        Penalite penalite = null;

        return new Emprunt(
                empruntId,
                docProxy,
                adherentProxy,
                dateEmprunt,
                dateRetourPrevue,
                dateRetourEffective,
                penalite
        );
    }

    /** Convertit une chaîne "YYYY-MM-DD" (ou vide) en LocalDate. */
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