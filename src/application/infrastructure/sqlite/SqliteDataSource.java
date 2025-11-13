package application.infrastructure.sqlite;

import java.io.IOException; // Import IOException
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths; // Use Paths for creating Path objects
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fournit des connexions JDBC vers la base SQLite embarquée du SGEB.
 *
 * <p>Caractéristiques :</p>
 * <ul>
 * <li>Fichier DB local par défaut : {@code data/sgeb.db}</li>
 * <li>Chemin surchargable via variable d'env {@code SGEB_DB_PATH}</li>
 * <li>Activation des clés étrangères : {@code PRAGMA foreign_keys=ON}</li>
 * <li>Timeout d'attente de verrous : {@code PRAGMA busy_timeout=5000}</li>
 * <li>Journalisation WAL pour de meilleures perfs : {@code PRAGMA journal_mode=WAL}</li>
 * </ul>
 *
 * <p>Utilisation typique : {@code try (Connection c = SqliteDataSource.getConnection()) { ... }}</p>
 */
public final class SqliteDataSource {

    /** Dossier runtime pour la DB (si SGEB_DB_PATH non défini). */
    public static final String DB_DIR = "data";
    /** Nom du fichier DB (si SGEB_DB_PATH non défini). */
    public static final String DB_FILE = "sgeb.db";

    private static String JDBC_URL = null; // Store the calculated URL

    private SqliteDataSource() {} // Private constructor for utility class

    /**
     * Calcule et retourne l'URL JDBC une seule fois.
     * Crée le dossier parent si nécessaire pour le chemin par défaut.
     * @return L'URL JDBC pour la connexion SQLite.
     * @throws RuntimeException si le dossier parent ne peut pas être créé.
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

            // Ensure parent directory exists
            Path parentDir = dbPath.getParent();
            if (parentDir != null && Files.notExists(parentDir)) {
                try {
                    Files.createDirectories(parentDir);
                    System.out.println("Created database directory: " + parentDir);
                } catch (IOException e) {
                    // Throw a runtime exception if directory creation fails, as it's critical
                    throw new RuntimeException("Failed to create database directory: " + parentDir, e);
                }
            }
            JDBC_URL = "jdbc:sqlite:" + dbPath;
            System.out.println("Using database URL: " + JDBC_URL);
        }
        return JDBC_URL;
    }

    /**
     * Ouvre une connexion SQLite avec les PRAGMA nécessaires.
     *
     * @return connexion ouverte (à fermer par l'appelant)
     * @throws SQLException en cas d'échec d'ouverture ou de configuration.
     */
    public static Connection getConnection() throws SQLException {
        // Ensure the class is loaded (optional, but good practice for drivers)
        // try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException e) { throw new SQLException("SQLite JDBC driver not found", e); }

        final Connection cnx = DriverManager.getConnection(getJdbcUrl());
        // Set PRAGMAs which are recommended per-connection
        try (Statement st = cnx.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
            st.execute("PRAGMA busy_timeout = 5000;"); // 5s timeout if DB is locked
            st.execute("PRAGMA journal_mode = WAL;");  // Better read/write concurrency
            st.execute("PRAGMA synchronous = NORMAL;"); // Good balance of safety/performance
        } catch (SQLException e) {
            // Close connection if PRAGMA setting fails
            try { cnx.close(); } catch (SQLException closeEx) { /* Ignore closing error */ }
            throw new SQLException("Failed to set PRAGMAs on SQLite connection", e);
        }
        return cnx;
    }

    // Removed ensureDbFolder as its logic is now in getJdbcUrl()
    // Removed duplicate closing brace
}