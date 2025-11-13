package application.ui.navigation.fx;

import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import application.domain.service.CatalogueService;
import application.domain.service.DashboardService;
import application.domain.service.EmpruntService;
import application.domain.service.PenaliteService; // <-- AJOUTÉ

import application.ui.navigation.Navigator;
import application.ui.viewmodel.AdherentsViewModel;
import application.ui.viewmodel.CatalogueViewModel;
import application.ui.viewmodel.DashboardViewModel;
import application.ui.viewmodel.EmpruntsViewModel;
import application.ui.viewmodel.PenalitesViewModel; // <-- AJOUTÉ
import application.ui.viewmodel.controller.AdherentsController;
import application.ui.viewmodel.controller.CatalogueController;
import application.ui.viewmodel.controller.DashboardController;
import application.ui.viewmodel.controller.EmpruntsController;
import application.ui.viewmodel.controller.PenalitesController; // <-- AJOUTÉ
import application.ui.viewmodel.controller.MainShellController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
/**
 * Navigateur JavaFX : charge les vues FXML, injecte les ViewModels/Services,
 * et met à jour la zone centrale du Shell (MainShell.fxml).
 *
 * CORRIGÉ : Accepte PenaliteService et gère la navigation vers PenalitesView.
 */
public final class FxNavigator implements Navigator {

    // --- Dimensions minimales de la fenêtre ---
    public static final double MIN_W = 1350; // Aligné sur votre code
    public static final double MIN_H = 800;

    // --- État fenêtre/shell ---
    private final Stage stage;
    private MainShellController shell;
    private boolean shellShown = false;

    // --- Contexte utilisateur courant (mémorisé par le Navigator) ---
    private String currentUsername;
    private String currentRole;

    // --- Dépendances (injectées) ---
    private final DashboardService dashboardService;
    private final CatalogueService catalogueService;
    private final EmpruntService empruntService;
    private final PenaliteService penaliteService; // <-- AJOUTÉ
    private final AdherentRepository adherentRepository;
    private final DocumentRepository documentRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteRepository penaliteRepository;
    private final BookInfoProvider bookInfoProvider;

    /**
     * CORRIGÉ : Constructeur complet qui initialise tous les champs final.
     */
    public FxNavigator(Stage stage,
                       DashboardService dashboardService,
                       CatalogueService catalogueService,
                       EmpruntService empruntService,
                       PenaliteService penaliteService, // <-- AJOUTÉ
                       AdherentRepository adherentRepository,
                       DocumentRepository documentRepository,
                       EmpruntRepository empruntRepository,
                       PenaliteRepository penaliteRepository,
                       BookInfoProvider bookInfoProvider) {
        
        this.stage = Objects.requireNonNull(stage, "stage");
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
        this.catalogueService = Objects.requireNonNull(catalogueService, "catalogueService");
        this.empruntService = Objects.requireNonNull(empruntService, "empruntService");
        this.penaliteService = Objects.requireNonNull(penaliteService, "penaliteService"); // <-- AJOUTÉ
        this.adherentRepository = Objects.requireNonNull(adherentRepository, "adherentRepository");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository");
        this.empruntRepository = Objects.requireNonNull(empruntRepository, "empruntRepository");
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository, "penaliteRepository");
        this.bookInfoProvider = bookInfoProvider;

        this.stage.setMinWidth(MIN_W);
        this.stage.setMinHeight(MIN_H);
    }


    // =====================================================================
    // Shell
    // =====================================================================

    @Override
    public void showShell() {
        if (shellShown) return;

        try {
            URL fxml = getClass().getResource("/application/ui/view/MainShell.fxml");
            if (fxml == null) {
                throw new IllegalStateException("FXML introuvable : /application/ui/view/MainShell.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            shell = loader.getController();
            if (shell == null) {
                throw new IllegalStateException("Controller MainShellController non résolu.");
            }
            shell.setNavigator(this); // injection du Navigator dans le shell

            double initW = Math.max(stage.getWidth() > 0 ? stage.getWidth() : MIN_W, MIN_W);
            double initH = Math.max(stage.getHeight() > 0 ? stage.getHeight() : MIN_H, MIN_H);

            Scene scene = new Scene(root, initW, initH);
            stage.setScene(scene);
            stage.setTitle("SGEB");
            stage.setMinWidth(MIN_W);
            stage.setMinHeight(MIN_H);
            stage.centerOnScreen();
            stage.show();
            shellShown = true;

            root.minWidth(MIN_W);
            root.minHeight(MIN_H);

            if (currentUsername != null || currentRole != null) {
                shell.setUserContext(currentUsername, currentRole);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ouverture du Shell impossible : " + e.getMessage(), e);
        }
    }


    // =====================================================================
    // Navigation de haut niveau
    // =====================================================================


    @Override
    public void goToDashboard(String username, String role) {
        if (username != null) this.currentUsername = username;
        if (role != null) this.currentRole = role;
        showShell();
        if (shell != null) {
            shell.setUserContext(this.currentUsername, this.currentRole);
        }
        setCenter("/application/ui/view/DashboardView.fxml");
    }

    @Override
    public void goToDashboard() {
        showShell();
        if (shell != null) {
            shell.setUserContext(this.currentUsername, this.currentRole);
        }
        setCenter("/application/ui/view/DashboardView.fxml");
    }

    @Override
    public void goToCatalog() {
        showShell();
        setCenter("/application/ui/view/CatalogueView.fxml");
    }

    @Override
    public void goToAdherents() {
        showShell();
        // CORRIGÉ : Chemin FXML corrigé (il y avait un 'V' en trop)
        setCenter("/application/ui/view/AdherentsView.fxml");
    }

    @Override
    public void goToLoans() {
        showShell();
        setCenter("/application/ui/view/EmpruntsView.fxml");
    }
    
    /**
     * NOUVEAU : Navigation vers la vue des Pénalités
     */
    public void goToPenalites() {
        showShell();
        setCenter("/application/ui/view/PenalitesView.fxml");
    }

    // =====================================================================
    // Chargement central (Controller Factory)
    // =====================================================================

    private void setCenter(String fxmlPath) {
        if (!shellShown || shell == null) {
            showShell();
        }

        try {
            URL fxml = getClass().getResource(fxmlPath);
            if (fxml == null) {
                throw new IllegalStateException("FXML introuvable : " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxml);

            // ---- CORRIGÉ : Controller Factory (injection de dépendances) ----
            loader.setControllerFactory(type -> {
                try {
                    if (type == DashboardController.class) {
                        var vm = new DashboardViewModel(
                            dashboardService, documentRepository, adherentRepository,
                            empruntRepository, penaliteRepository,
                            currentUsername, currentRole
                        );
                        return new DashboardController(vm);
                    
                    } else if (type == CatalogueController.class) {
                        var vm = new CatalogueViewModel(catalogueService, bookInfoProvider);
                        return new CatalogueController(vm);
                    
                    } else if (type == AdherentsController.class) {
                        var vm = new AdherentsViewModel(
                            adherentRepository,
                            empruntRepository,
                            penaliteRepository
                        );
                        return new AdherentsController(vm);
                    
                    } else if (type == EmpruntsController.class) {
                        var vm = new EmpruntsViewModel(
                            empruntService,
                            adherentRepository,
                            documentRepository
                        );
                        return new EmpruntsController(vm);
                    
                    } else if (type == PenalitesController.class) {
                        // NOUVEAU : Injection pour la vue Pénalités
                        var vm = new PenalitesViewModel(
                            penaliteRepository,
                            empruntRepository,
                            penaliteService
                        );
                        return new PenalitesController(vm);
                    }

                    // Contrôleurs sans dépendances explicites
                    return type.getDeclaredConstructor().newInstance();

                } catch (Exception e) {
                    throw new RuntimeException("Factory: création contrôleur impossible : " + type.getName(), e);
                }
            });

            Parent view = loader.load();
            view.minWidth(MIN_W);
            view.minHeight(MIN_H);
            shell.setContent(view);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Chargement vue impossible (" + fxmlPath + ") : " + e.getMessage(), e);
        }
    }
}