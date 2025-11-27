package application.infrastructure.sqlite;

import application.domain.core.document.CD;
import application.domain.core.document.Document;
import application.domain.core.document.Livre;
import application.domain.core.document.Magazine;
import application.domain.ports.repository.DocumentRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation SQLite de {@link DocumentRepository} pour la table {@code DOCUMENT}.
 *
 * <ul>
 *     <li>{@code dtype} : "Livre", "Magazine" ou "CD"</li>
 *     <li>Champs communs : {@code titre}, {@code annee_publication}, {@code editeur}, {@code genre}</li>
 *     <li>Livre : {@code auteur}, {@code isbn}, {@code nombre_pages}</li>
 *     <li>Magazine : {@code numero}, {@code periodicite}, {@code date_parution}</li>
 *     <li>CD : {@code artiste}, {@code duree_minutes}</li>
 *     <li>Cover : {@code cover_path}, {@code cover_mime}, {@code cover_updated_at}</li>
 * </ul>
 *
 * <p>Décision : pour un CD, {@code editeur} est utilisé comme label.</p>
 */
public class SqliteDocumentRepository implements DocumentRepository {

    // =============================
    // ========== CRUD =============
    // =============================

    @Override
    public Document save(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document ne doit pas être null");
        }
        return (document.getId() == null) ? insert(document) : update(document);
    }

    @Override
    public Optional<Document> findById(Long id) {
        final String sql = "SELECT * FROM DOCUMENT WHERE document_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.findById a échoué", e);
        }
    }

    @Override
    public List<Document> findAll() {
        final String sql = "SELECT * FROM DOCUMENT ORDER BY document_id";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Document> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;

        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.findAll a échoué", e);
        }
    }

    @Override
    public void delete(Long id) {
        final String sql = "DELETE FROM DOCUMENT WHERE document_id = ?";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            // Si le document est encore emprunté, une contrainte FK peut échouer
            throw new RuntimeException(
                    "DOCUMENT.delete : impossible de supprimer (document peut-être emprunté ?)", e
            );
        }
    }

    @Override
    public long count() {
        final String sql = "SELECT COUNT(*) FROM DOCUMENT";

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;

        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.count a échoué", e);
        }
    }

    // ==========================================
    // ========== Requêtes spécifiques ==========
    // ==========================================

    @Override
    public List<Document> findByTitre(String titre) {
        // Recherche "commence par"
        String value = (titre == null) ? "" : titre;
        return findByFieldLike("titre", value + "%", "findByTitre");
    }

    @Override
    public List<Document> findByAuteur(String auteur) {
        // Recherche "contient"
        String value = (auteur == null) ? "" : auteur;
        return findByFieldLike("auteur", "%" + value + "%", "findByAuteur");
    }

    @Override
    public List<Document> findByGenre(String genre) {
        String value = (genre == null) ? "" : genre;
        return findByFieldExact("genre", value, "findByGenre");
    }

    @Override
    public Optional<Document> findByIsbn(String isbn) {
        String value = (isbn == null) ? "" : isbn;
        List<Document> results = findByFieldExact("isbn", value, "findByIsbn");
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Collection<? extends Document> findByTitreContaining(String query) {
        String value = (query == null) ? "" : query;
        return findByFieldLike("titre", "%" + value + "%", "findByTitreContaining");
    }

    @Override
    public Collection<? extends Document> findByAuteurContaining(String query) {
        String value = (query == null) ? "" : query;
        return findByFieldLike("auteur", "%" + value + "%", "findByAuteurContaining");
    }

    // ==========================================
    // ========== Helpers de requête ============
    // ==========================================

    /** Recherche avec LIKE (pour les champs texte). */
    private List<Document> findByFieldLike(String column, String value, String context) {
        final String sql = "SELECT * FROM DOCUMENT WHERE " + column + " LIKE ? ORDER BY document_id";
        return executeFindQuery(sql, value, context);
    }

    /** Recherche avec égalité stricte (=). */
    private List<Document> findByFieldExact(String column, String value, String context) {
        final String sql = "SELECT * FROM DOCUMENT WHERE " + column + " = ? ORDER BY document_id";
        return executeFindQuery(sql, value, context);
    }

    /** Exécute une requête SELECT simple avec un seul paramètre. */
    private List<Document> executeFindQuery(String sql, String value, String context) {
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                List<Document> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }

        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT." + context + " a échoué", e);
        }
    }

    // =============================
    // ========== SQL I/O ==========
    // =============================

    /** Insère un nouveau document. */
    private Document insert(Document document) {
        final String sql = """
            INSERT INTO DOCUMENT(
              dtype, titre, annee_publication, editeur, genre,
              auteur, isbn, nombre_pages,
              numero, periodicite, date_parution,
              artiste, duree_minutes,
              cover_url, cover_path, cover_mime, cover_updated_at
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindAll(ps, document);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    document.setId(keys.getLong(1));
                }
            }
            return document;

        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.insert a échoué", e);
        }
    }

    /** Met à jour un document existant. */
    private Document update(Document document) {
        final String sql = """
            UPDATE DOCUMENT SET
              dtype=?, titre=?, annee_publication=?, editeur=?, genre=?,
              auteur=?, isbn=?, nombre_pages=?,
              numero=?, periodicite=?, date_parution=?,
              artiste=?, duree_minutes=?,
              cover_url=?, cover_path=?, cover_mime=?, cover_updated_at=?
            WHERE document_id = ?
        """;

        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int idx = bindAll(ps, document);
            ps.setLong(idx, document.getId());
            ps.executeUpdate();
            return document;

        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.update a échoué", e);
        }
    }

    /**
     * Renseigne tous les paramètres SQL communs + spécifiques en fonction du type de document.
     *
     * @return l'index du prochain paramètre libre (utilisé pour document_id dans l'UPDATE).
     */
    private int bindAll(PreparedStatement ps, Document d) throws SQLException {
        int i = 1;

        // Champs communs
        ps.setString(i++, d.getDtype());
        ps.setString(i++, d.getTitre());
        ps.setObject(i++, d.getAnneePublication(), Types.INTEGER);
        ps.setString(i++, d.getEditeur()); // Pour CD : editeur = label
        ps.setString(i++, d.getGenre());

        // Champs spécifiques selon le type
        if (d instanceof Livre l) {
            ps.setString(i++, l.getAuteur());
            ps.setString(i++, l.getIsbn());
            ps.setObject(i++, l.getNbPages(), Types.INTEGER);
            ps.setNull(i++, Types.INTEGER); // numero
            ps.setNull(i++, Types.VARCHAR); // periodicite
            ps.setNull(i++, Types.VARCHAR); // date_parution
            ps.setNull(i++, Types.VARCHAR); // artiste
            ps.setNull(i++, Types.INTEGER); // duree_minutes
        } else if (d instanceof Magazine m) {
            ps.setNull(i++, Types.VARCHAR); // auteur
            ps.setNull(i++, Types.VARCHAR); // isbn
            ps.setNull(i++, Types.INTEGER); // nombre_pages
            ps.setObject(i++, m.getNumero(), Types.INTEGER);
            ps.setString(i++, m.getPeriodicite());
            ps.setString(i++, m.getMoisParution());
            ps.setNull(i++, Types.VARCHAR); // artiste
            ps.setNull(i++, Types.INTEGER); // duree_minutes
        } else if (d instanceof CD cd) {
            ps.setNull(i++, Types.VARCHAR); // auteur
            ps.setNull(i++, Types.VARCHAR); // isbn
            ps.setNull(i++, Types.INTEGER); // nombre_pages
            ps.setNull(i++, Types.INTEGER); // numero
            ps.setNull(i++, Types.VARCHAR); // periodicite
            ps.setNull(i++, Types.VARCHAR); // date_parution
            ps.setString(i++, cd.getArtistePrincipal());
            ps.setObject(i++, cd.getDureeMinutes(), Types.INTEGER);
        } else {
            // Cas "Document générique" (normalement non utilisé)
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.INTEGER);
            ps.setNull(i++, Types.INTEGER);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.INTEGER);
        }

        // Champs cover
        ps.setString(i++, null); // cover_url
        ps.setString(i++, d.getCoverRef());
        ps.setString(i++, d.getCoverMime());
        ps.setString(i++, d.getCoverUpdatedAt() == null ? null : String.valueOf(d.getCoverUpdatedAt()));

        return i;
    }

    // =============================
    // ===== Mapping résultat ======
    // =============================

    /** Convertit une ligne de la table DOCUMENT en objet Document spécialisé. */
    private Document mapRow(ResultSet rs) throws SQLException {
        // Champs communs
        String dtype = rs.getString("dtype");
        Long id = rs.getLong("document_id");
        String titre = rs.getString("titre");
        Integer annee = (Integer) rs.getObject("annee_publication");
        String editeur = rs.getString("editeur");
        String genre = rs.getString("genre");
        String coverRef = rs.getString("cover_path");
        String coverMime = rs.getString("cover_mime");
        Long coverUpdated = readLongTolerant(rs, "cover_updated_at");

        // Spécifiques selon le type
        if ("Magazine".equalsIgnoreCase(dtype)) {
            Integer numero = (Integer) rs.getObject("numero");
            String moisParution = rs.getString("date_parution");
            String periodicite = rs.getString("periodicite");

            return new Magazine(
                    id, titre, annee, editeur, genre,
                    coverRef, coverMime, coverUpdated,
                    numero, moisParution, periodicite
            );
        }

        if ("CD".equalsIgnoreCase(dtype)) {
            Integer dureeMinutes = (Integer) rs.getObject("duree_minutes");
            String artiste = rs.getString("artiste");

            return new CD(
                    id, titre, annee, editeur, genre,
                    coverRef, coverMime, coverUpdated,
                    dureeMinutes, artiste,
                    editeur // label = editeur
            );
        }

        // Par défaut, on suppose un Livre
        String auteur = rs.getString("auteur");
        String isbn = rs.getString("isbn");
        Integer nbPages = (Integer) rs.getObject("nombre_pages");

        return new Livre(
                id, titre, annee, editeur, genre,
                coverRef, coverMime, coverUpdated,
                auteur, isbn, nbPages
        );
    }

    /** Lecture tolérante d'un Long (stocké en INTEGER ou TEXT). */
    private static Long readLongTolerant(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return s.isBlank() ? null : Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}