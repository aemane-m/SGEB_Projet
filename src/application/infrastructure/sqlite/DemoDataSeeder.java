package application.infrastructure.sqlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Outil pour remplir la base de données avec des données de test.
 * <p>
 * Il lit un fichier SQL (script) situé dans les ressources de l'application
 * et exécute les commandes une par une.
 * </p>
 */
public class DemoDataSeeder {

    private static final String SCRIPT_FILE = "demo-data.sql";

    /**
     * Lance l'insertion des données.
     * <p>
     * Cette méthode utilise une <strong>Transaction</strong> :
     * Soit toutes les données sont insérées correctement, soit rien du tout (en cas d'erreur).
     * </p>
     */
    public void installDemoData() {
        System.out.println(">> [DemoDataSeeder] Démarrage de l'injection des données...");

        // 1. Lecture du fichier SQL
        String sqlGlobal = lireFichierSql(SCRIPT_FILE);
        if (sqlGlobal == null || sqlGlobal.isBlank()) {
            System.err.println(">> [DemoDataSeeder] Fichier vide ou introuvable. Annulation.");
            return;
        }

        // 2. Connexion et Exécution
        try (Connection connection = SqliteDataSource.getConnection()) {
            
            // Démarrage de la transaction (Mode manuel)
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                
                // On découpe le fichier par point-virgule pour avoir chaque commande INSERT
                String[] requetes = sqlGlobal.split(";");
                int compteur = 0;

                for (String requete : requetes) {
                    String sqlPropre = requete.trim();
                    
                    // On ignore les lignes vides
                    if (!sqlPropre.isEmpty()) {
                        statement.executeUpdate(sqlPropre);
                        compteur++;
                    }
                }

                // Tout s'est bien passé, on valide !
                connection.commit();
                System.out.println(">> [DemoDataSeeder] Succès : " + compteur + " commandes exécutées.");

            } catch (SQLException e) {
                // En cas de pépin, on annule tout ce qu'on vient de faire
                connection.rollback();
                System.err.println(">> [DemoDataSeeder] Erreur SQL : " + e.getMessage());
                throw new RuntimeException("Échec de l'injection des données (Rollback effectué)", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion à la base de données", e);
        }
    }

    /**
     * Lit le contenu du fichier .sql situé dans le même dossier (package) que cette classe.
     */
    private String lireFichierSql(String nomFichier) {
        // On cherche le fichier dans le "Classpath" (les ressources compilées)
        try (InputStream flux = getClass().getResourceAsStream(nomFichier)) {
            
            if (flux == null) {
                System.err.println(">> [DemoDataSeeder] Fichier introuvable : " + nomFichier);
                System.err.println("   (Assurez-vous qu'il est dans : " + getClass().getPackageName().replace('.', '/') + ")");
                return null;
            }

            // Lecture et assemblage du texte
            try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux, StandardCharsets.UTF_8))) {
                return lecteur.lines().collect(Collectors.joining("\n"));
            }

        } catch (IOException e) {
            throw new RuntimeException("Erreur de lecture du fichier SQL : " + nomFichier, e);
        }
    }
}