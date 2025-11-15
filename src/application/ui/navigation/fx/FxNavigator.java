package application.ui.navigation.fx;

import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import application.domain.service.AccountService;
import application.domain.service.CatalogueService;
import application.domain.service.DashboardService;
import application.domain.service.EmpruntService;
import application.domain.service.PenaliteService;
import application.ui.navigation.Navigator;
import application.ui.viewmodel.AdherentsViewModel;
import application.ui.viewmodel.CatalogueViewModel;
import application.ui.viewmodel.DashboardViewModel;
import application.ui.viewmodel.EmpruntsViewModel;
import application.ui.viewmodel.PenalitesViewModel;
import application.ui.viewmodel.MesEmpruntsViewModel;
import application.ui.viewmodel.MesPenalitesViewModel;
import application.ui.viewmodel.controller.AdherentsController;
import application.ui.viewmodel.controller.CatalogueController;
import application.ui.viewmodel.controller.DashboardController;
import application.ui.viewmodel.controller.EmpruntsController;
import application.ui.viewmodel.controller.PenalitesController;
import application.ui.viewmodel.controller.MesEmpruntsController;
import application.ui.viewmodel.controller.MesPenalitesController;
import application.ui.viewmodel.controller.MainShellController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.lang.reflect.Method;        // NEW
import java.net.URL;
import java.util.Objects;

/**
 * Navigateur JavaFX : charge les vues FXML, injecte les ViewModels/Services,
 * et met à jour la zone centrale du Shell (MainShell.fxml).
 *
 * - stocke une {@link AccountService.UserSession}
 * - propage username/role vers le Shell
 * - gère la vue Pénalités + les vues adhérent ("Mes emprunts" / "Mes pénalités")
 */
public final class FxNavigator implements Navigator {

    // --- Dimensions minimales de la fenêtre ---
    public static final double MIN_W = 1350;
    public static final double MIN_H = 800;

    // --- État fenêtre/shell ---
    private final Stage stage;
    private MainShellController shell;
    private boolean shellShown = false;

    // --- Contexte utilisateur courant ---
    private AccountService.UserSession currentSession;

    // --- Dépendances (injectées) ---
    private final DashboardService dashboardService;
    private final CatalogueService catalogueService;
    private final EmpruntService empruntService;
    private final PenaliteService penaliteService;
    private final AdherentRepository adherentRepository;
    private final DocumentRepository documentRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteRepository penaliteRepository;
    private final BookInfoProvider bookInfoProvider;

    // NEW : référence à l'AccountService pour gérer login / changement mdp
    private final AccountService accountService;

    public FxNavigator(Stage stage,
                       DashboardService dashboardService,
                       CatalogueService catalogueService,
                       EmpruntService empruntService,
                       PenaliteService penaliteService,
                       AdherentRepository adherentRepository,
                       DocumentRepository documentRepository,
                       EmpruntRepository empruntRepository,
                       PenaliteRepository penaliteRepository,
                       BookInfoProvider bookInfoProvider,
                       AccountService accountService       // NEW
    ) {

        this.stage = Objects.requireNonNull(stage, "stage");
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
        this.catalogueService = Objects.requireNonNull(catalogueService, "catalogueService");
        this.empruntService = Objects.requireNonNull(empruntService, "empruntService");
        this.penaliteService = Objects.requireNonNull(penaliteService, "penaliteService");
        this.adherentRepository = Objects.requireNonNull(adherentRepository, "adherentRepository");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository");
        this.empruntRepository = Objects.requireNonNull(empruntRepository, "empruntRepository");
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository, "penaliteRepository");
        this.bookInfoProvider = Objects.requireNonNull(bookInfoProvider, "bookInfoProvider");
        this.accountService = Objects.requireNonNull(accountService, "accountService"); // NEW

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

            // Appliquer le contexte utilisateur si déjà connu
            applyUserContextToShell();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ouverture du Shell impossible : " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // Navigation de haut niveau
    // =====================================================================

    @Override
    public void goToDashboard(AccountService.UserSession session) {
        if (session != null) {
            this.currentSession = session;
        }

        showShell();
        applyUserContextToShell(); // met à jour le header et les menus

        boolean isAdherent = currentSession != null
                && currentSession.role() != null
                && "ADHERENT".equalsIgnoreCase(currentSession.role().getName());

        if (isAdherent) {
            setCenter("/application/ui/view/MesEmpruntsView.fxml");
        } else {
            setCenter("/application/ui/view/DashboardView.fxml");
        }
    }

    @Override
    public void goToCatalog() {
        showShell();
        setCenter("/application/ui/view/CatalogueView.fxml");
    }

    @Override
    public void goToAdherents() {
        showShell();
        setCenter("/application/ui/view/AdherentsView.fxml");
    }

    @Override
    public void goToLoans() {
        showShell();
        setCenter("/application/ui/view/EmpruntsView.fxml");
    }

    @Override
    public void goToPenalites() {
        showShell();
        setCenter("/application/ui/view/PenalitesView.fxml");
    }

    @Override
    public void goToMesEmprunts() {
        showShell();
        setCenter("/application/ui/view/MesEmpruntsView.fxml");
    }

    @Override
    public void goToMesPenalites() {
        showShell();
        setCenter("/application/ui/view/MesPenalitesView.fxml");
    }

    @Override
    public void goToLogin() {
        try {
            URL fxml = getClass().getResource("/application/ui/view/LoginView.fxml");
            if (fxml == null) {
                throw new IllegalStateException("FXML introuvable : /application/ui/view/LoginView.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            // NEW : injection de AccountService + Navigator dans LoginController
            injectControllerDependencies(loader);

            Scene scene = new Scene(root, MIN_W, MIN_H);
            stage.setScene(scene);
            stage.setTitle("SGEB – Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger LoginView.fxml", e);
        }
    }

    @Override
    public void goToRegister() {
        // Pour l’instant, on peut réutiliser la même vue que Login
        goToLogin();
    }

    // =====================================================================
    // Utilitaire : appliquer la session au Shell
    // =====================================================================

    private void applyUserContextToShell() {
        if (shell == null) return;

        if (currentSession != null) {
            String username = currentSession.email(); // ou displayName
            String roleName = (currentSession.role() != null)
                    ? currentSession.role().getName()
                    : null;

            shell.setUserContext(username, roleName);
        } else {
            shell.setUserContext(null, null);
        }
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

            // Controller Factory (injection de dépendances dans les contrôleurs)
            loader.setControllerFactory(type -> {
                try {
                    if (type == DashboardController.class) {
                        String username = (currentSession != null) ? currentSession.email() : null;
                        String roleName = (currentSession != null && currentSession.role() != null)
                                ? currentSession.role().getName()
                                : null;

                        var vm = new DashboardViewModel(
                                dashboardService,
                                documentRepository,
                                adherentRepository,
                                empruntRepository,
                                penaliteRepository,
                                username,
                                roleName
                        );
                        return new DashboardController(vm);

                    } else if (type == CatalogueController.class) {
                        var vm = new CatalogueViewModel(catalogueService, bookInfoProvider);

                        // 🔐 Admin = catalogue complet, Adhérent = lecture seule
                        boolean adminMode = currentSession != null
                                && currentSession.role() != null
                                && "ADMIN".equalsIgnoreCase(currentSession.role().getName());

                        return new CatalogueController(vm, adminMode);

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
                        var vm = new PenalitesViewModel(
                                penaliteRepository,
                                empruntRepository,
                                penaliteService
                        );
                        return new PenalitesController(vm);

                    } else if (type == MesEmpruntsController.class) {
                        Long adherentId = (currentSession != null) ? currentSession.adherentId() : null;
                        var vm = new MesEmpruntsViewModel(
                                empruntService,
                                adherentRepository,
                                documentRepository,
                                adherentId
                        );
                        return new MesEmpruntsController(vm);

                    } else if (type == MesPenalitesController.class) {
                        Long adherentId = (currentSession != null) ? currentSession.adherentId() : null;
                        var vm = new MesPenalitesViewModel(
                                penaliteService,
                                empruntRepository,
                                penaliteRepository,
                                adherentId
                        );
                        return new MesPenalitesController(vm);
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

    // =====================================================================
    // Injection générique pour Login / écrans hors Shell
    // =====================================================================

    /**
     * Injection de AccountService + Navigator dans un contrôleur chargé
     * via FXMLLoader (ex : LoginView.fxml).
     */
    private void injectControllerDependencies(FXMLLoader loader) {
        Object controller = loader.getController();
        if (controller == null) return;

        // AccountService
        if (accountService != null) {
            try {
                Method m = controller.getClass().getMethod("setAccountService", AccountService.class);
                m.invoke(controller, accountService);
            } catch (NoSuchMethodException e) {
                // pas de setAccountService -> OK
            } catch (Exception e) {
                throw new RuntimeException("Injection AccountService impossible dans "
                        + controller.getClass().getName() + " : " + e.getMessage(), e);
            }
        }

        // Navigator
        try {
            Method m = controller.getClass().getMethod("setNavigator", Navigator.class);
            m.invoke(controller, this);
        } catch (NoSuchMethodException e) {
            // pas de setNavigator -> OK
        } catch (Exception e) {
            throw new RuntimeException("Injection Navigator impossible dans "
                    + controller.getClass().getName() + " : " + e.getMessage(), e);
        }
    }
}