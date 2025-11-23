package application.infrastructure.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initialise le schéma SQLite du SGEB.
 *
 * <p>Points importants :</p>
 * <ul>
 *     <li>Table {@code PERSONNE} alignée avec {@link AccountServiceSqlite} :
 *         <ul>
 *             <li>{@code password_hash} NOT NULL</li>
 *             <li>{@code status} TEXT NOT NULL DEFAULT 'ACTIVE'</li>
 *             <li>{@code password_must_change} INTEGER NOT NULL DEFAULT 0</li>
 *         </ul>
 *     </li>
 *     <li>Table {@code PENALITE} contenant notamment {@code date_fin},
 *         compatible avec le repository qui utilise cette colonne.</li>
 * </ul>
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
        // Classe utilitaire : pas d'instance
    }

    /**
     * Initialise le schéma de la base SQLite si nécessaire.
     * <p>
     * Toutes les opérations sont exécutées dans une transaction :
     * en cas d'erreur, un rollback est effectué.
     * </p>
     *
     * @throws RuntimeException en cas d'échec d'initialisation
     */
    public static void initialize() {
        try (Connection c = SqliteDataSource.getConnection()) {
            c.setAutoCommit(false);

            try (Statement st = c.createStatement()) {

                // Toujours activer les contraintes de clés étrangères
                st.execute("PRAGMA foreign_keys = ON;");

                // =========================================================
                // ================ TABLE PERSONNE (corrigée) ===============
                // =========================================================
                st.execute("""
                    CREATE TABLE IF NOT EXISTS PERSONNE (
                        personne_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        email                TEXT    NOT NULL UNIQUE,
                        password_hash        TEXT    NOT NULL,
                        nom                  TEXT    NOT NULL,
                        prenom               TEXT    NOT NULL,
                        actif                INTEGER NOT NULL DEFAULT 1,
                        isAdmin              INTEGER NOT NULL DEFAULT 0,
                        created_at           TEXT    NOT NULL,
                        telephone            TEXT,
                        
                        -- Statut du compte : ACTIVE, PENDING, SUSPENDED, DISABLED, etc.
                        status               TEXT    NOT NULL DEFAULT 'ACTIVE',
                        
                        -- Flag : forcer le changement de mot de passe à la prochaine connexion
                        password_must_change INTEGER NOT NULL DEFAULT 0
                    );
                    """);

                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_personne_email
                        ON PERSONNE(email);
                    """);
                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_personne_status
                        ON PERSONNE(status);
                    """);

                // =========================================================
                // ================= TABLE DOCUMENT (inchangée) ============
                // =========================================================
                st.execute("""
                    CREATE TABLE IF NOT EXISTS DOCUMENT (
                        document_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                        dtype             TEXT    NOT NULL CHECK (dtype IN ('Livre', 'Magazine', 'CD')),
                        titre             TEXT    NOT NULL,
                        annee_publication INTEGER,
                        editeur           TEXT,
                        genre             TEXT,
                        auteur            TEXT,
                        isbn              TEXT,
                        nombre_pages      INTEGER,
                        numero            INTEGER,
                        periodicite       TEXT,
                        date_parution     TEXT,
                        artiste           TEXT,
                        duree_minutes     INTEGER,
                        cover_url         TEXT,
                        cover_path        TEXT,
                        cover_mime        TEXT,
                        cover_updated_at  TEXT
                    );
                    """);

                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_document_dtype
                        ON DOCUMENT(dtype);
                    """);

                // =========================================================
                // ================== TABLE EMPRUNT =========================
                // =========================================================
                st.execute("""
                    CREATE TABLE IF NOT EXISTS EMPRUNT (
                        emprunt_id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        document_id           INTEGER NOT NULL,
                        emprunteur_id         INTEGER NOT NULL,
                        date_emprunt          TEXT    NOT NULL,
                        date_retour_prevue    TEXT    NOT NULL,
                        date_retour_effective TEXT,
                        
                        FOREIGN KEY (document_id) REFERENCES DOCUMENT(document_id)
                            ON UPDATE CASCADE ON DELETE RESTRICT,
                        FOREIGN KEY (emprunteur_id) REFERENCES PERSONNE(personne_id)
                            ON UPDATE CASCADE ON DELETE RESTRICT
                    );
                    """);

                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_emprunt_document
                        ON EMPRUNT(document_id);
                    """);
                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_emprunt_emprunteur
                        ON EMPRUNT(emprunteur_id);
                    """);

                // =========================================================
                // ================== TABLE PENALITE ========================
                // =========================================================
                st.execute("""
                    CREATE TABLE IF NOT EXISTS PENALITE (
                        penalite_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        adherent_id INTEGER NOT NULL,
                        emprunt_id  INTEGER NOT NULL,
                        type        TEXT,
                        montant     REAL    NOT NULL,
                        date_debut  TEXT,
                        date_fin    TEXT,
                        description TEXT,
                        
                        -- Statut du workflow de paiement
                        -- ex : ACTIVE, DEMANDE, REGLEE, ANNULEE
                        status      TEXT NOT NULL DEFAULT 'ACTIVE',
                        
                        FOREIGN KEY (adherent_id) REFERENCES PERSONNE(personne_id)
                            ON UPDATE CASCADE ON DELETE RESTRICT,
                        FOREIGN KEY (emprunt_id) REFERENCES EMPRUNT(emprunt_id)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    );
                    """);

                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_penalite_adherent
                        ON PENALITE(adherent_id);
                    """);
                st.execute("""
                    CREATE INDEX IF NOT EXISTS idx_penalite_status
                        ON PENALITE(status);
                    """);

                // Si tout va bien : commit
                c.commit();

            } catch (SQLException ddlError) {
                c.rollback();
                throw ddlError;
            } finally {
                c.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur d'initialisation du schéma SQLite", e);
        }
    }
}