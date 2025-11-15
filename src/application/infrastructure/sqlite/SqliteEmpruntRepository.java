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
 * Utilise des LEFT JOIN pour charger des proxies (Document, Adherent)
 * et éviter les requêtes N+1 côté service/UI.
 *
 * Remarque : le statut métier de l'emprunt (EmpruntStatus) n'est pas stocké
 * en base, il est calculé dans l'entité à partir des dates.
 */
public class SqliteEmpruntRepository implements EmpruntRepository {

    /**
     * Requête SQL de base qui joint les tables EMPRUNT, DOCUMENT et PERSONNE
     * pour récupérer toutes les informations nécessaires en une seule fois.
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
    public Emprunt save(Emprunt e) {
        if (e.getDocument() == null || e.getDocument().getId() == null) {
            throw new IllegalArgumentException("Le document de l'emprunt doit avoir un ID.");
        }
        if (e.getEmprunteur() == null || e.getEmprunteur().getId() == null) {
            throw new IllegalArgumentException("L'emprunteur de l'emprunt doit avoir un ID.");
        }
        return (e.getId() == null) ? insert(e) : update(e);
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
                return rs.next() ? Optional.ofNullable(mapRowToEmprunt(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findById failed", e);
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
                Emprunt e = mapRowToEmprunt(rs);
                if (e != null) {
                    results.add(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findAll failed", e);
        }
        return results;
    }

    @Override
    public void delete(Long id) {
        if (id == null || id <= 0) {
            return;
        }
        final String sql = "DELETE FROM EMPRUNT WHERE emprunt_id = ?";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.delete failed", e);
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
            throw new RuntimeException("EMPRUNT.count failed", e);
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
                    Emprunt e = mapRowToEmprunt(rs);
                    if (e != null) {
                        results.add(e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findByAdherentId failed", e);
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
                    Emprunt e = mapRowToEmprunt(rs);
                    if (e != null) {
                        results.add(e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findByDocumentId failed", e);
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
                Emprunt e = mapRowToEmprunt(rs);
                if (e != null) {
                    results.add(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findActiveEmprunts failed", e);
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
                    Emprunt e = mapRowToEmprunt(rs);
                    if (e != null) {
                        results.add(e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("EMPRUNT.findLateEmprunts failed", e);
        }
        return results;
    }

    @Override
    public boolean isDocumentEmprunte(long documentId) {
        // Cette requête n'a pas besoin du JOIN : plus simple = plus performant
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
            throw new RuntimeException("EMPRUNT.isDocumentEmprunte failed", e);
        }
    }

    // =============================
    // ========== SQL IO ===========
    // =============================

    private Emprunt insert(Emprunt e) {
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

            bindParameters(ps, e);
            ps.executeUpdate();

            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    e.setId(k.getLong(1));
                }
            }
            return e;
        } catch (SQLException ex) {
            throw new RuntimeException("EMPRUNT.insert failed", ex);
        }
    }

    private Emprunt update(Emprunt e) {
        final String sql = """
            UPDATE EMPRUNT
               SET document_id          = ?,
                   emprunteur_id        = ?,
                   date_emprunt         = ?,
                   date_retour_prevue   = ?,
                   date_retour_effective= ?
             WHERE emprunt_id           = ?
            """;
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int i = bindParameters(ps, e);
            ps.setLong(i, e.getId());
            ps.executeUpdate();
            return e;
        } catch (SQLException ex) {
            throw new RuntimeException("EMPRUNT.update failed", ex);
        }
    }

    /**
     * Lie les paramètres communs INSERT/UPDATE (sans l'ID de l'emprunt).
     */
    private int bindParameters(PreparedStatement ps, Emprunt e) throws SQLException {
        int i = 1;

        if (e.getDocument() == null || e.getDocument().getId() == null) {
            throw new SQLException("Document proxy/ID est null dans bindParameters");
        }
        if (e.getEmprunteur() == null || e.getEmprunteur().getId() == null) {
            throw new SQLException("Emprunteur proxy/ID est null dans bindParameters");
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

    /**
     * Mappe une ligne de ResultSet (avec JOINs) en objet Emprunt.
     * Crée des objets "proxy" (Document, Adherent) peuplés
     * avec les informations suffisantes pour l'affichage (ID, titre, nom, coverRef).
     */
    private Emprunt mapRowToEmprunt(ResultSet rs) throws SQLException {
        Long id = rs.getLong("emprunt_id");
        if (rs.wasNull()) id = null;

        Long docId = rs.getLong("document_id");
        boolean docIdWasNull = rs.wasNull();

        Long adhId = rs.getLong("emprunteur_id");
        boolean adhIdWasNull = rs.wasNull();

        String dtype = rs.getString("dtype");

        // Données du JOIN
        String docTitre    = rs.getString("docTitre");
        String docCoverRef = rs.getString("docCoverRef");
        String adhNom      = rs.getString("adhNom");
        String adhPrenom   = rs.getString("adhPrenom");

        // Sécu : si FK manquante → on log et on skip
        if (docIdWasNull || adhIdWasNull) {
            System.err.println("Warning: Skipping orphan Emprunt record with ID=" + id);
            return null;
        }

        // --- Crée le bon type de Document proxy ---
        Document docProxy;
        if ("Magazine".equals(dtype)) {
            docProxy = new Magazine();
        } else if ("CD".equals(dtype)) {
            docProxy = new CD();
        } else {
            // Défaut pour "Livre" ou dtype null
            docProxy = new Livre();
        }
        docProxy.setId(docId);
        docProxy.setTitre(docTitre);
        docProxy.setCoverRef(docCoverRef);

        // --- Crée le proxy Adherent ---
        Adherent adherentProxy = new Adherent();
        adherentProxy.setId(adhId);
        adherentProxy.setNom(adhNom);
        adherentProxy.setPrenom(adhPrenom);

        // --- Dates ---
        LocalDate dateEmprunt        = parseLocalDate(rs.getString("date_emprunt"));
        LocalDate dateRetourPrevue   = parseLocalDate(rs.getString("date_retour_prevue"));
        LocalDate dateRetourEffective= parseLocalDate(rs.getString("date_retour_effective"));

        if (dateEmprunt == null || dateRetourPrevue == null) {
            System.err.println("Warning: Skipping Emprunt record with missing required dates, ID=" + id);
            return null;
        }

        // On ne charge pas la pénalité ici (évite de coupler/faire des JOIN supplémentaires)
        Penalite penalite = null;

        return new Emprunt(
                id,
                docProxy,
                adherentProxy,
                dateEmprunt,
                dateRetourPrevue,
                dateRetourEffective,
                penalite
        );
    }

    private static LocalDate parseLocalDate(String dateString) {
        return (dateString == null || dateString.isBlank())
                ? null
                : LocalDate.parse(dateString);
    }
}