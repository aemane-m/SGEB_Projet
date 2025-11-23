package application.infrastructure.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fournit des connexions JDBC vers la base SQLite du SGEB.
 *
 * <p>Caractéristiques :</p>
 * <ul>
 *   <li>Fichier DB par défaut : {@code data/sgeb.db}</li>
 *   <li>Chemin surchargeable via la variable d'environnement {@code SGEB_DB_PATH}</li>
 *   <li>Activation des clés étrangères : {@code PRAGMA foreign_keys = ON}</li>
 *   <li>Timeout sur les verrous : {@code PRAGMA busy_timeout = 5000}</li>
 *   <li>Journalisation WAL : {@code PRAGMA journal_mode = WAL}</li>
 * </ul>
 *
 * <p>Utilisation :</p>
 * <pre>
 * try (Connection c = SqliteDataSource.getConnection()) {
 *     // Requêtes SQL...
 * }
 * </pre>
 */
public final class SqliteDataSource {

    /** Dossier par défaut pour la base. */
    public static final String DB_DIR = "data";

    /** Nom de fichier par défaut. */
    public static final String DB_FILE = "sgeb.db";

    /** URL JDBC calculée une seule fois. */
    private static String JDBC_URL = null;

    /** Classe utilitaire : constructeur privé. */
    private SqliteDataSource() {
    }

    /**
     * Calcule (une fois) et retourne l'URL JDBC.
     * Si SGEB_DB_PATH est défini, on l'utilise. Sinon, on prend data/sgeb.db.
     * Le dossier parent est créé si besoin.
     */
    private static synchronized String getJdbcUrl() {
        if (JDBC_URL == null) {
            String override = System.getenv("SGEB_DB_PATH");
            Path dbPath;

            if (override != null && !override.isBlank()) {
                dbPath = Paths.get(override.trim()).toAbsolutePath();
            } else {
                dbPath = Paths.get(DB_DIR, DB_FILE).toAbsolutePath();
            }

            // Création du dossier parent si nécessaire
            Path parentDir = dbPath.getParent();
            if (parentDir != null && Files.notExists(parentDir)) {
                try {
                    Files.createDirectories(parentDir);
                    System.out.println("Répertoire de base créé : " + parentDir);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "Impossible de créer le répertoire de base : " + parentDir, e
                    );
                }
            }

            JDBC_URL = "jdbc:sqlite:" + dbPath;
            System.out.println("URL de base SQLite : " + JDBC_URL);
        }

        return JDBC_URL;
    }

    /**
     * Ouvre une connexion SQLite et applique les PRAGMA nécessaires.
     *
     * @return connexion prête à l'emploi (à fermer par l'appelant).
     * @throws SQLException en cas d'erreur d'ouverture ou de configuration.
     */
    public static Connection getConnection() throws SQLException {
        // Si besoin, on pourrait forcer le chargement du driver :
        // Class.forName("org.sqlite.JDBC");

        Connection cnx = DriverManager.getConnection(getJdbcUrl());

        // PRAGMA appliqués sur la connexion
        try (Statement st = cnx.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
            st.execute("PRAGMA busy_timeout = 5000;");   // 5 secondes d'attente si la base est verrouillée
            st.execute("PRAGMA journal_mode = WAL;");    // Meilleure concurrence lecture/écriture
            st.execute("PRAGMA synchronous = NORMAL;");  // Compromis sécurité / performance
        } catch (SQLException e) {
            // En cas d'échec, on ferme la connexion et on remonte l'erreur
            try {
                cnx.close();
            } catch (SQLException ignored) {
                // on ignore les erreurs de fermeture
            }
            throw new SQLException("Impossible de configurer la connexion SQLite (PRAGMA)", e);
        }

        return cnx;
    }
}