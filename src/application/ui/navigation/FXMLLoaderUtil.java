package application.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException; // Be more specific on exception
import java.net.URL;
import java.util.Objects;

/**
 * Utility class for loading FXML files and accessing their components.
 * Provides a convenient way to get the root node, controller, and loader instance.
 */
public final class FXMLLoaderUtil {

    /** Private constructor to prevent instantiation. */
    private FXMLLoaderUtil() {}

    /**
     * Loads an FXML file from the classpath relative to this utility class.
     *
     * @param fxmlPath The path to the FXML file (e.g., "/application/ui/view/MyView.fxml"). Must not be null.
     * @return A {@link LoadedView} record containing the loaded root Parent, its controller, and the FXMLLoader instance.
     * @throws IllegalArgumentException if the fxmlPath is null or the FXML resource cannot be found.
     * @throws RuntimeException if an IOException occurs during loading.
     */
    public static LoadedView load(String fxmlPath) {
        Objects.requireNonNull(fxmlPath, "fxmlPath cannot be null");
        try {
            // Get resource relative to the utility class's location
            URL url = FXMLLoaderUtil.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalArgumentException("FXML resource not found: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load(); // Can throw IOException
            Object controller = loader.getController();

            return new LoadedView(root, controller, loader);

        } catch (IOException e) {
            // Wrap IOException in a RuntimeException for convenience, providing context
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        } catch (IllegalStateException e) {
            // Catch potential issues if controller factory is needed but not set, etc.
             throw new RuntimeException("Failed during FXML loading process: " + fxmlPath, e);
        }
    }

    /**
     * A record holding the results of loading an FXML file.
     *
     * @param root       The root {@link Parent} node of the loaded FXML hierarchy.
     * @param controller The controller instance associated with the FXML file (can be null if no controller is specified).
     * @param loader     The {@link FXMLLoader} instance used for loading.
     */
    public record LoadedView(Parent root, Object controller, FXMLLoader loader) {}
}