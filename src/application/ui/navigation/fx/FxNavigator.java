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
import application.ui.viewmodel.*;
import application.ui.viewmodel.controller.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Objects;

/**
 * Implémentation JavaFX de {@link Navigator}.
 *
 * - Charge les vues FXML.
 * - Injecte ViewModels / services.
 * - Gère la zone centrale du Shell (MainShell.fxml).
 * - Stocke et propage la {@link AccountService.UserSession}.
 */
public final class FxNavigator implements Navigator {

    // --- Dimensions minimales de la fenêtre ---
    public static final double MIN_W = 1350;
    public static final double MIN_H = 800;

    // --- FXML Paths ---
    private static final String FXML_SHELL           = "/application/ui/view/MainShell.fxml";
    private static final String FXML_DASHBOARD       = "/application/ui/view/DashboardView.fxml";
    private static final String FXML_CATALOGUE       = "/application/ui/view/CatalogueView.fxml";
    private static final String FXML_ADHERENTS       = "/application/ui/view/AdherentsView.fxml";
    private static final String FXML_EMPRUNTS        = "/application/ui/view/EmpruntsView.fxml";
    private static final String FXML_PENALITES       = "/application/ui/view/PenalitesView.fxml";
    private static final String FXML_ACCOUNTS        = "/application/ui/view/AccountsView.fxml";
    private static final String FXML_MES_EMPRUNTS    = "/application/ui/view/MesEmpruntsView.fxml";
    private static final String FXML_MES_PENALITES   = "/application/ui/view/MesPenalitesView.fxml";
    private static final String FXML_LOGIN           = "/application/ui/view/LoginView.fxml";
    private static final String FXML_REGISTER        = "/application/ui/view/SignupAdherentView.fxml";
    private static final String FXML_CHANGE_PASSWORD = "/application/ui/view/ChangePasswordView.fxml";
    private static final String FXML_LANDING         = "/application/ui/view/LandingView.fxml";

    // --- État fenêtre / shell ---
    private final Stage stage;
    private MainShellController shell;
    private boolean shellShown = false;

    // --- Contexte utilisateur courant ---
    private AccountService.UserSession currentSession;

    // --- Services et ports ---
    private final DashboardService dashboardService;
    private final CatalogueService catalogueService;
    private final EmpruntService empruntService;
    private final PenaliteService penaliteService;
    private final AdherentRepository adherentRepository;
    private final DocumentRepository documentRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteRepository penaliteRepository;
    private final BookInfoProvider bookInfoProvider;
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
                       AccountService accountService) {

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
        this.accountService = Objects.requireNonNull(accountService, "accountService");

        this.stage.setMinWidth(MIN_W);
        this.stage.setMinHeight(MIN_H);
    }

    // =====================================================================
    // Shell
    // =====================================================================

    @Override
    public void showShell() {
        if (shellShown) {
            return;
        }

        try {
            URL fxml = getResourceOrThrow(FXML_SHELL);
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            shell = loader.getController();
            if (shell == null) {
                throw new IllegalStateException("Controller MainShellController non résolu.");
            }

            shell.setNavigator(this);

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
        applyUserContextToShell();

        boolean isAdherent = currentSession != null
                && currentSession.role() != null
                && "ADHERENT".equalsIgnoreCase(currentSession.role().getName());

        if (isAdherent) {
            setCenter(FXML_MES_EMPRUNTS);
        } else {
            setCenter(FXML_DASHBOARD);
        }
    }

    @Override
    public void goToCatalog() {
        showShell();
        setCenter(FXML_CATALOGUE);
    }

    @Override
    public void goToAdherents() {
        showShell();
        setCenter(FXML_ADHERENTS);
    }

    @Override
    public void goToLoans() {
        showShell();
        setCenter(FXML_EMPRUNTS);
    }

    @Override
    public void goToPenalites() {
        showShell();
        setCenter(FXML_PENALITES);
    }

    @Override
    public void goToAccounts() {
        showShell();
        setCenter(FXML_ACCOUNTS);
    }

    @Override
    public void goToMesEmprunts() {
        showShell();
        setCenter(FXML_MES_EMPRUNTS);
    }

    @Override
    public void goToMesPenalites() {
        showShell();
        setCenter(FXML_MES_PENALITES);
    }

    @Override
    public void goToLogin() {
        try {
            URL fxml = getResourceOrThrow(FXML_LOGIN);
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

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
        try {
            URL fxml = getResourceOrThrow(FXML_REGISTER);
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            injectControllerDependencies(loader);

            Scene scene = new Scene(root, MIN_W, MIN_H);
            stage.setScene(scene);
            stage.setTitle("SGEB – Inscription");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger la vue d'inscription", e);
        }
    }

    @Override
    public void goToChangePassword(AccountService.UserSession session) {
        Objects.requireNonNull(session, "session ne peut pas être null pour goToChangePassword");

        try {
            URL fxml = getResourceOrThrow(FXML_CHANGE_PASSWORD);
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            ChangePasswordController controller = loader.getController();
            if (controller == null) {
                throw new IllegalStateException("Controller ChangePasswordController non résolu.");
            }

            controller.setAccountService(accountService);
            controller.setNavigator(this);
            controller.setSession(session);

            Scene scene = new Scene(root, MIN_W, MIN_H);
            stage.setScene(scene);
            stage.setTitle("SGEB – Changement de mot de passe");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Impossible de charger ChangePasswordView.fxml : " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // Application du contexte utilisateur au Shell
    // =====================================================================

    private void applyUserContextToShell() {
        if (shell == null) {
            return;
        }

        if (currentSession != null) {
            String username = currentSession.email();
            String roleName = (currentSession.role() != null)
                    ? currentSession.role().getName()
                    : null;
            shell.setUserContext(username, roleName);
        } else {
            shell.setUserContext(null, null);
        }
    }

    // =====================================================================
    // Chargement central des vues du Shell
    // =====================================================================

    private void setCenter(String fxmlPath) {
        if (!shellShown || shell == null) {
            showShell();
        }

        try {
            URL fxml = getResourceOrThrow(fxmlPath);
            FXMLLoader loader = new FXMLLoader(fxml);

            // Controller Factory : injection de dépendances / ViewModels
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
                                empruntRepository,
                                username,
                                roleName
                        );
                        return new DashboardController(vm);

                    } else if (type == CatalogueController.class) {
                        var vm = new CatalogueViewModel(catalogueService, bookInfoProvider);

                        boolean adminMode = currentSession != null
                                && currentSession.role() != null
                                && "ADMIN".equalsIgnoreCase(currentSession.role().getName());

                        return new CatalogueController(vm, adminMode);

                    } else if (type == AdherentsController.class) {
                        var vm = new AdherentsViewModel(
                                adherentRepository,
                                empruntRepository,
                                penaliteRepository,
                                accountService
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
                                documentRepository,
                                adherentId
                        );
                        return new MesEmpruntsController(vm);

                    } else if (type == MesPenalitesController.class) {
                        Long adherentId = (currentSession != null) ? currentSession.adherentId() : null;
                        var vm = new MesPenalitesViewModel(
                                penaliteService,
                                empruntRepository,
                                adherentId
                        );
                        return new MesPenalitesController(vm);

                    } else if (type == AccountsController.class) {
                        long actingUserId = (currentSession != null) ? currentSession.userId() : -1L;
                        var vm = new AccountsViewModel(accountService, actingUserId);
                        return new AccountsController(vm);
                    }

                    // Contrôleurs sans dépendances explicites : constructeur par défaut
                    return type.getDeclaredConstructor().newInstance();

                } catch (Exception e) {
                    throw new RuntimeException("Factory : création contrôleur impossible : " + type.getName(), e);
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
    // Injection générique pour Login / Landing / etc.
    // =====================================================================

    /**
     * Injection de {@link AccountService} et {@link Navigator} dans les contrôleurs
     * possédant les méthodes appropriées (setAccountService, setNavigator).
     */
    private void injectControllerDependencies(FXMLLoader loader) {
        Object controller = loader.getController();
        if (controller == null) {
            return;
        }

        // AccountService
        if (accountService != null) {
            try {
                Method m = controller.getClass().getMethod("setAccountService", AccountService.class);
                m.invoke(controller, accountService);
            } catch (NoSuchMethodException e) {
                // Pas de setAccountService : OK
            } catch (Exception e) {
                throw new RuntimeException(
                        "Injection AccountService impossible dans " + controller.getClass().getName() + " : " + e.getMessage(), e
                );
            }
        }

        // Navigator
        try {
            Method m = controller.getClass().getMethod("setNavigator", Navigator.class);
            m.invoke(controller, this);
        } catch (NoSuchMethodException e) {
            // Pas de setNavigator : OK
        } catch (Exception e) {
            throw new RuntimeException(
                    "Injection Navigator impossible dans " + controller.getClass().getName() + " : " + e.getMessage(), e
            );
        }
    }

    // =====================================================================
    // Déconnexion
    // =====================================================================

    @Override
    public void logout() {
        // On oublie la session courante
        this.currentSession = null;

        try {
            URL fxml = getResourceOrThrow(FXML_LANDING);
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            injectControllerDependencies(loader);

            Scene scene = new Scene(root, 900, 600);
            stage.setTitle("SGEB – Bibliothèque");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();
            stage.show();

            this.shell = null;
            this.shellShown = false;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Impossible de charger LandingView.fxml : " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Récupère une ressource FXML ou lève une IllegalStateException claire. */
    private URL getResourceOrThrow(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            throw new IllegalStateException("FXML introuvable : " + path);
        }
        return url;
    }
}