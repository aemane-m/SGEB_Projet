package application.infrastructure.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initialise le schéma SQLite du SGEB (Simplifié : Admin-Uniquement).
 *
 * <p>Tables :</p>
 * <ul>
 * <li>PERSONNE(personne_id, email, password_hash, nom, prenom, actif, isAdmin, created_at)</li>
 * <li>DOCUMENT(...)</li>
 * <li>EMPRUNT(...)</li>
 * <li>PENALITE(...)</li>
 * </ul>
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection c = SqliteDataSource.getConnection()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {

                // Toujours activer les contraintes FK
                st.execute("PRAGMA foreign_keys = ON;");

                // =================== NETTOYAGE (Ancien schéma) ===================
                // On supprime les anciennes tables de rôles
                st.execute("DROP TABLE IF EXISTS PERSONNE_ROLE;");
                st.execute("DROP TABLE IF EXISTS ROLE;");

                // =================== PERSONNE (Simplifié) ===================
                // On s'assure de recréer la table PERSONNE si elle a l'ancien format
                // (Note : c'est destructif, mais nécessaire pour le changement de schéma)
                 //st.execute("DROP TABLE IF EXISTS PERSONNE;"); // Décommentez si la table existe déjà

                st.execute("""
                    CREATE TABLE IF NOT EXISTS PERSONNE (
                        personne_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                        email         TEXT    NOT NULL UNIQUE,
                        password_hash TEXT,
                        nom           TEXT    NOT NULL,
                        prenom        TEXT    NOT NULL,
                        actif         INTEGER NOT NULL DEFAULT 1,
                        isAdmin       INTEGER NOT NULL DEFAULT 0,
                        created_at    TEXT    NOT NULL,
                        telephone     INTEGER
                    );
                """);
                
                st.execute("CREATE INDEX IF NOT EXISTS idx_personne_email ON PERSONNE(email);");
                // SUPPRIMÉ : Index pour 'ROLE'

                // =================== DOCUMENT (Inchangé) ===================
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

                st.execute("CREATE INDEX IF NOT EXISTS idx_document_dtype ON DOCUMENT(dtype);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_document_titre ON DOCUMENT(titre);");
                // ... (autres index inchangés)

                // =================== EMPRUNT (Inchangé) ===================
                // Cette table référence 'PERSONNE' pour 'emprunteur_id', 
                // ce qui est correct, car les Adhérents sont dans PERSONNE.
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
                
                st.execute("CREATE INDEX IF NOT EXISTS idx_emprunt_document ON EMPRUNT(document_id);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_emprunt_emprunteur ON EMPRUNT(emprunteur_id);");

                // =================== PENALITE (Inchangé) ===================
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
                        FOREIGN KEY (adherent_id) REFERENCES PERSONNE(personne_id)
                            ON UPDATE CASCADE ON DELETE RESTRICT,
                        FOREIGN KEY (emprunt_id) REFERENCES EMPRUNT(emprunt_id)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    );
                """);
                
                st.execute("CREATE INDEX IF NOT EXISTS idx_penalite_adherent ON PENALITE(adherent_id);");

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