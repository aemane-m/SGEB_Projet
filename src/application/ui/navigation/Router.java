package application.ui.navigation;

import application.ui.viewmodel.controller.MainShellController;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

public final class Router {

    private final Stage stage;
    private MainShellController shellController;
    private final Map<Route, Parent> cache = new EnumMap<>(Route.class);

    public Router(Stage stage) {
        this.stage = stage;
    }

    /** Affiche la fenêtre shell (si pas déjà affichée). */
    public void showShell() {
        FXMLLoaderUtil.LoadedView lv = FXMLLoaderUtil.load("/application/ui/view/MainShell.fxml");
        Parent root = lv.root();

        // Récupère le contrôleur
        Object controller = lv.controller();
        if (!(controller instanceof MainShellController c)) {
            throw new IllegalStateException("MainShell.fxml n'utilise pas MainShellController.");
        }
        this.shellController = c;

        // Injection optionnelle du Router si le setter existe
        try {
            Method m = shellController.getClass().getMethod("setRouter", Router.class);
            m.invoke(shellController, this);
        } catch (NoSuchMethodException ignored) {
            // Pas de setRouter(Router) : on continue sans injection
        } catch (Exception e) {
            throw new RuntimeException("Injection du Router dans MainShellController impossible : " + e.getMessage(), e);
        }

        stage.setScene(new Scene(root, 1000, 680));
        stage.setTitle("SGEB — Tableau de bord");
        stage.show();
    }

    /** Navigue vers une route (charge, met en cache, affiche dans center). */
    public void go(Route route) {
        ensureShell();
        Parent view = cache.computeIfAbsent(route, r -> {
            FXMLLoaderUtil.LoadedView lv = FXMLLoaderUtil.load(r.fxml());
            return lv.root();
        });
        Platform.runLater(() -> shellController.setContent(view));
    }

    private void ensureShell() {
        if (shellController == null) showShell();
    }
}

