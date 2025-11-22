package application.infrastructure.sqlite;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DemoDataSeeder {

    public void installDemoData() {
        System.out.println("[DemoDataSeeder] Installation des données de démo...");

        try (Connection c = SqliteDataSource.getConnection();
             Statement st = c.createStatement()) {

            String sql = loadSqlScript("demo-data.sql"); // même package
            if (sql == null || sql.isBlank()) {
                throw new IllegalStateException(
                        "[DemoDataSeeder] Le script demo-data.sql est vide ou introuvable " +
                        "(package: " + getClass().getPackageName() + ")"
                );
            }

            int count = 0;
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    st.executeUpdate(trimmed);
                    count++;
                }
            }

            System.out.println("[DemoDataSeeder] Données de démo installées avec succès. " +
                               count + " statements exécutés.");

        } catch (Exception e) {
            System.err.println("[DemoDataSeeder] Échec lors de l'installation des données de démo.");
            e.printStackTrace();
            // On laisse remonter pour que le contrôleur affiche l'erreur
            throw new RuntimeException("Erreur lors de l'installation des données de démonstration : " +
                                       e.getMessage(), e);
        }
    }

    /**
     * Charge un fichier SQL placé dans le même package que cette classe.
     */
    private String loadSqlScript(String resourceName) {
        try {
            InputStream is = getClass().getResourceAsStream(resourceName);
            if (is == null) {
                System.err.println("[DemoDataSeeder] Resource not found: " + resourceName +
                                   " in package " + getClass().getPackageName());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
