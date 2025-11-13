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
 * Implémentation SQLite de {@link DocumentRepository} pour la table TPH {@code DOCUMENT}.
 *
 * <h2>Mapping TPH</h2>
 * <ul>
 * <li>{@code DOCUMENT.dtype} : "Livre" / "Magazine" / "CD"</li>
 * <li>Champs communs : {@code titre}, {@code annee_publication}, {@code editeur}, {@code genre}</li>
 * <li>Livre : {@code auteur}, {@code isbn}, {@code nombre_pages}</li>
 * <li>Magazine : {@code numero}, {@code periodicite}, {@code date_parution}</li>
 * <li>CD : {@code artiste}, {@code duree_minutes}</li>
 * <li>Cover : {@code cover_path}, {@code cover_mime}, {@code cover_updated_at}</li>
 * </ul>
 *
 * <p>Décision : {@code CD.label} est stocké dans la colonne commune {@code editeur}.</p>
 */
public class SqliteDocumentRepository implements DocumentRepository {

    // =============================
    // ========== CRUD =============
    // =============================

    /** {@inheritDoc} */
    @Override
    public Document save(Document d) {
        return (d.getId() == null) ? insert(d) : update(d);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Document> findById(Long id) {
        final String sql = "SELECT * FROM DOCUMENT WHERE document_id=?";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.findById", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Document> findAll() {
        final String sql = "SELECT * FROM DOCUMENT ORDER BY document_id";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Document> out = new ArrayList<>();
            while (rs.next()) out.add(mapRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.findAll", e);
        }
    }

    /** {@inheritDoc} – contrat générique Repository<T,ID>. */
    @Override
    public void delete(Long id) {
        final String sql = "DELETE FROM DOCUMENT WHERE document_id=?";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Si le document est emprunté, la contrainte FK peut échouer
            throw new RuntimeException("DOCUMENT.delete : impossible de supprimer (peut-être emprunté?)", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long count() {
        final String sql = "SELECT COUNT(*) FROM DOCUMENT";
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.count", e);
        }
    }

    // ==========================================
    // ========== Méthodes spécifiques ==========
    // ==========================================

    /** {@inheritDoc} */
    @Override
    public List<Document> findByTitre(String titre) {
        // Utilise LIKE pour une recherche flexible, % à la fin pour utiliser l'index
        return findByFieldLike("titre", titre + "%");
    }

    /** {@inheritDoc} */
    @Override
    public List<Document> findByAuteur(String auteur) {
        // Utilise LIKE pour une recherche flexible
        return findByFieldLike("auteur", "%" + auteur + "%");
    }

    /** {@inheritDoc} */
    @Override
    public List<Document> findByGenre(String genre) {
        // Correspondance exacte, utilise l'index
        return findByFieldExact("genre", genre);
    }

    /**
     * {@inheritDoc}
     * <p>Implémentation de l'exigence : "Rechercher un document par [...] ISBN."</p>
     */
    @Override
    public Optional<Document> findByIsbn(String isbn) {
        // Correspondance exacte, utilise l'index
        List<Document> result = findByFieldExact("isbn", isbn);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }


    private List<Document> findByFieldLike(String col, String value) {
        // CORRIGÉ : Utilise LIKE (insensible à la casse) qui peut utiliser l'index
        final String sql = "SELECT * FROM DOCUMENT WHERE " + col + " LIKE ? ORDER BY document_id";
        return executeFindQuery(sql, value, "findByFieldLike(" + col + ")");
    }

    private List<Document> findByFieldExact(String col, String value) {
        // CORRIGÉ : Utilise = (insensible à la casse) qui utilise l'index
        final String sql = "SELECT * FROM DOCUMENT WHERE " + col + " = ? ORDER BY document_id";
        return executeFindQuery(sql, value, "findByFieldExact(" + col + ")");
    }

    private List<Document> executeFindQuery(String sql, String value, String errorContext) {
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                List<Document> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT." + errorContext, e);
        }
    }


    // =============================
    // ========== SQL IO ===========
    // =============================

    private Document insert(Document d) {
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
            bindAll(ps, d);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) d.setId(k.getLong(1));
            }
            return d;
        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.insert", e);
        }
    }

    private Document update(Document d) {
        final String sql = """
            UPDATE DOCUMENT SET
              dtype=?, titre=?, annee_publication=?, editeur=?, genre=?,
              auteur=?, isbn=?, nombre_pages=?,
              numero=?, periodicite=?, date_parution=?,
              artiste=?, duree_minutes=?,
              cover_url=?, cover_path=?, cover_mime=?, cover_updated_at=?
            WHERE document_id=?
        """;
        try (Connection c = SqliteDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = bindAll(ps, d);
            ps.setLong(idx, d.getId());
            ps.executeUpdate();
            return d;
        } catch (SQLException e) {
            throw new RuntimeException("DOCUMENT.update", e);
        }
    }

    /** Renseigne tous les paramètres SQL pour un {@link Document}. */
    private int bindAll(PreparedStatement ps, Document d) throws SQLException {
        int i = 1;
        ps.setString(i++, d.getDtype());
        ps.setString(i++, d.getTitre());
        ps.setObject(i++, d.getAnneePublication(), Types.INTEGER);
        ps.setString(i++, d.getEditeur()); // pour CD : editeur == label
        ps.setString(i++, d.getGenre());

        if (d instanceof Livre l) {
            // CORRIGÉ : Gère 'auteur'
            ps.setString(i++, l.getAuteur());
            ps.setString(i++, l.getIsbn());
            ps.setObject(i++, l.getNbPages(), Types.INTEGER);
            ps.setNull(i++, Types.INTEGER);    // numero
            ps.setNull(i++, Types.VARCHAR);   // periodicite
            ps.setNull(i++, Types.VARCHAR);   // date_parution
            ps.setNull(i++, Types.VARCHAR);   // artiste
            ps.setNull(i++, Types.INTEGER);   // duree_minutes
        } else if (d instanceof Magazine m) {
            ps.setNull(i++, Types.VARCHAR);   // auteur
            ps.setNull(i++, Types.VARCHAR);   // isbn
            ps.setNull(i++, Types.INTEGER);   // nombre_pages
            ps.setObject(i++, m.getNumero(), Types.INTEGER);
            // CORRIGÉ : Gère 'periodicite'
            ps.setString(i++, m.getPeriodicite());
            ps.setString(i++, m.getMoisParution()); // date_parution
            ps.setNull(i++, Types.VARCHAR);   // artiste
            ps.setNull(i++, Types.INTEGER);   // duree_minutes
        } else if (d instanceof CD cd) {
            ps.setNull(i++, Types.VARCHAR);   // auteur
            ps.setNull(i++, Types.VARCHAR);   // isbn
            ps.setNull(i++, Types.INTEGER);   // nombre_pages
            ps.setNull(i++, Types.INTEGER);   // numero
            ps.setNull(i++, Types.VARCHAR);   // periodicite
            ps.setNull(i++, Types.VARCHAR);   // date_parution
            ps.setString(i++, cd.getArtistePrincipal()); // artiste
            ps.setObject(i++, cd.getDureeMinutes(), Types.INTEGER); // duree_minutes
        } else {
            // Document générique (ne devrait pas arriver si dtype est géré)
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.INTEGER);
            ps.setNull(i++, Types.INTEGER);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.VARCHAR);
            ps.setNull(i++, Types.INTEGER);
        }

        ps.setString(i++, null); // cover_url (non utilisée)
        ps.setString(i++, d.getCoverRef()); // cover_path
        ps.setString(i++, d.getCoverMime());
        ps.setString(i++, d.getCoverUpdatedAt() == null ? null : String.valueOf(d.getCoverUpdatedAt()));
        return i;
    }

    /** Convertit une ligne SQL en entité {@link Document} spécialisée. */
    private Document mapRow(ResultSet rs) throws SQLException {
        // --- 1. Champs Communs
        final String dtype = rs.getString("dtype");
        Long id = rs.getLong("document_id");
        String titre = rs.getString("titre");
        Integer annee = (Integer) rs.getObject("annee_publication");
        String editeur = rs.getString("editeur");
        String genre = rs.getString("genre");
        String coverRef = rs.getString("cover_path");
        String coverMime = rs.getString("cover_mime");
        Long coverUpdated = readLongTolerant(rs, "cover_updated_at");

        // --- 2. Champs Spécifiques
        if ("Magazine".equalsIgnoreCase(dtype)) {
            Integer numero = (Integer) rs.getObject("numero");
            String moisParution = rs.getString("date_parution");
            // CORRIGÉ : Lecture de 'periodicite'
            String periodicite = rs.getString("periodicite");
            return new Magazine(id, titre, annee, editeur, genre,
                coverRef, coverMime, coverUpdated, numero, moisParution, periodicite);
        } else if ("CD".equalsIgnoreCase(dtype)) {
            return new CD(id, titre, annee, editeur, genre,
                coverRef, coverMime, coverUpdated,
                (Integer) rs.getObject("duree_minutes"),
                rs.getString("artiste"),
                editeur /* label = editeur */);
        } else {
            // CORRIGÉ : Lecture de 'auteur'
            String auteur = rs.getString("auteur");
            String isbn = rs.getString("isbn");
            Integer nbPages = (Integer) rs.getObject("nombre_pages");
            return new Livre(id, titre, annee, editeur, genre,
                coverRef, coverMime, coverUpdated,
                auteur, isbn, nbPages);
        }
    }

    /** Lecture tolérante d'un Long stocké en TEXT ou en INTEGER. */
    private static Long readLongTolerant(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try {
                return s.isBlank() ? null : Long.parseLong(s.trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Collection<? extends Document> findByTitreContaining(String query) {
        // Utilise LIKE pour une recherche "contient"
        // (votre méthode findByTitre utilisait "commence par")
        return findByFieldLike("titre", "%" + query + "%");
    }

    @Override
    public Collection<? extends Document> findByAuteurContaining(String query) {
        // Cette méthode est fonctionnellement identique à votre findByAuteur existante
        return findByFieldLike("auteur", "%" + query + "%");
    }
}