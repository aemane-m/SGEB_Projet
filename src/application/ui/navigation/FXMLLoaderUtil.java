package application.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Utilitaire pour charger des fichiers FXML.
 * Permet de récupérer :
 * - le root,
 * - le contrôleur,
 * - l'instance de {@link FXMLLoader}.
 */
public final class FXMLLoaderUtil {

    /** Classe utilitaire : constructeur privé. */
    private FXMLLoaderUtil() {
    }

    /**
     * Charge un FXML à partir du classpath, relatif à cette classe.
     *
     * @param fxmlPath chemin du fichier FXML (ex : "/application/ui/view/MyView.fxml"), non null
     * @return un {@link LoadedView} contenant le root, le contrôleur et le loader
     * @throws IllegalArgumentException si la ressource FXML est introuvable
     * @throws RuntimeException si une erreur I/O survient pendant le chargement
     */
    public static LoadedView load(String fxmlPath) {
        Objects.requireNonNull(fxmlPath, "fxmlPath ne peut pas être null");

        try {
            URL url = FXMLLoaderUtil.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalArgumentException("Ressource FXML introuvable : " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Object controller = loader.getController();

            return new LoadedView(root, controller, loader);

        } catch (IOException e) {
            throw new RuntimeException("Échec du chargement FXML : " + fxmlPath, e);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Erreur pendant le chargement FXML : " + fxmlPath, e);
        }
    }

    /**
     * Résultat d'un chargement FXML.
     *
     * @param root       root de la hiérarchie FXML
     * @param controller contrôleur associé (éventuellement null)
     * @param loader     {@link FXMLLoader} utilisé
     */
    public record LoadedView(Parent root, Object controller, FXMLLoader loader) {
    }
}