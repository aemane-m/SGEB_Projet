package application;

// --- JavaFX Imports ---
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

// --- Application Specific Imports ---
import application.domain.core.emprunt.Penalite;
import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.repository.*;
import application.domain.ports.storage.CoverStoragePort;
import application.domain.service.*;
import application.infrastructure.external.LocalCoverStorageAdapter;
import application.infrastructure.external.OpenLibraryProvider;
import application.infrastructure.sqlite.*; // inclut DemoDataSeeder, DatabaseInitializer, SqliteDataSource...
import application.ui.navigation.Navigator;
import application.ui.navigation.fx.FxNavigator;

// --- Standard Java Imports ---
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDate;

public class Main extends Application {

    /** Référence au Navigator (shell principal) */
    private Navigator navigator;

    /** Référence à l'AccountService (auth, comptes) */
    private AccountService accountService;

    /** Seeder pour les données de démonstration (optionnel) */
    private DemoDataSeeder demoDataSeeder;

    @Override
    public void start(Stage primaryStage) {
        // Handler global des exceptions non catchées
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread " + t.getName());
            e.printStackTrace();
            Platform.runLater(() ->
                    showStartupError(new RuntimeException("An unexpected error occurred.", e))
            );
        });

        try {
            // 1) Initialisation de la base
            System.out.println("Initializing database schema...");
            DatabaseInitializer.initialize();
            System.out.println("Database schema initialized successfully.");

            // 2) Assemblage des dépendances (repositories + services)

            // Repositories (SQLite)
            final AdherentRepository adherentRepository   = new SqliteAdherentRepository();
            final DocumentRepository documentRepository   = new SqliteDocumentRepository();
            final EmpruntRepository empruntRepository     = new SqliteEmpruntRepository();
            final PenaliteRepository penaliteRepository   = new SqlitePenaliteRepository();

            // Services externes
            final BookInfoProvider bookInfoProvider = new OpenLibraryProvider();
            final CoverStoragePort coverStorage     = new LocalCoverStorageAdapter(); // ./covers

            // Services métier
            final DashboardService dashboardService = createSimpleDashboardService(
                    documentRepository,
                    adherentRepository,
                    empruntRepository,
                    penaliteRepository
            );

            final CatalogueService catalogueService = new DefaultCatalogueService(
                    documentRepository,
                    empruntRepository,
                    coverStorage
            );

            final EmpruntService empruntService = new DefaultEmpruntService(
                    empruntRepository,
                    documentRepository,
                    adherentRepository,
                    penaliteRepository
            );

            final PenaliteService penaliteService = new DefaultPenaliteService(penaliteRepository);

            // 2bis) AccountService (authentification / comptes)
            // ⚠️ On le crée AVANT le Navigator, car le Navigator en a besoin.
            this.accountService = new AccountServiceSqlite();

            // 2ter) DemoDataSeeder (données de démonstration)
            // ✅ C'est ici qu'on instancie vraiment le seeder :
            this.demoDataSeeder = new DemoDataSeeder();

            // 3) Navigator (shell JavaFX) – on garde la référence dans le champ
            this.navigator = new FxNavigator(
                    primaryStage,
                    dashboardService,
                    catalogueService,
                    empruntService,
                    penaliteService,
                    adherentRepository,
                    documentRepository,
                    empruntRepository,
                    penaliteRepository,
                    bookInfoProvider,
                    this.accountService
            );

            // 4) Vérification du premier démarrage : y a-t-il au moins un compte ?
            boolean hasAnyUser = this.accountService.countUsers() > 0;

            // 5) Choix de l'écran de démarrage
            if (!hasAnyUser) {
                // Aucun compte -> écran de configuration initiale (création du 1er admin)
                showSetupWizard(primaryStage);
            } else {
                // Des comptes existent déjà -> Landing page (présentation + login / inscription)
                showLanding(primaryStage);
            }

        } catch (Exception e) {
            System.err.println("Application startup failed.");
            e.printStackTrace();
            showStartupError(e);
            Platform.exit();
            System.exit(1);
        }
    }

    // =========================================================
    // =========== Écrans de démarrage (Landing / Setup) =======
    // =========================================================

    private void showLanding(Stage stage) throws IOException {
        URL fxmlUrl = getClass().getResource("/application/ui/view/LandingView.fxml");
        if (fxmlUrl == null) {
            throw new IOException("FXML resource not found: /application/ui/view/LandingView.fxml");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // Injection AccountService + Navigator (+ DemoDataSeeder si besoin)
        injectDependencies(loader);

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("SGEB – Bibliothèque");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    private void showSetupWizard(Stage stage) throws IOException {
        URL fxmlUrl = getClass().getResource("/application/ui/view/SetupWizardView.fxml");
        if (fxmlUrl == null) {
            throw new IOException("FXML resource not found: /application/ui/view/SetupWizardView.fxml");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // Injection AccountService + Navigator + DemoDataSeeder
        injectDependencies(loader);

        Scene scene = new Scene(root, 600, 420);
        stage.setTitle("SGEB – Configuration initiale");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    private void injectDependencies(FXMLLoader loader) {
        Object controller = loader.getController();
        if (controller == null) return;

        // Injection AccountService
        if (this.accountService != null) {
            try {
                Method m = controller.getClass().getMethod("setAccountService", AccountService.class);
                m.invoke(controller, this.accountService);
            } catch (NoSuchMethodException e) {
                // Le contrôleur ne gère pas l'accountService, ce n'est pas grave.
            } catch (Exception e) {
                throw new RuntimeException("Injection AccountService impossible dans " +
                        controller.getClass().getName() + " : " + e.getMessage(), e);
            }
        }

        // Injection Navigator
        if (this.navigator != null) {
            try {
                Method m = controller.getClass().getMethod("setNavigator", Navigator.class);
                m.invoke(controller, this.navigator);
            } catch (NoSuchMethodException e) {
                // Le contrôleur ne gère pas le Navigator, ce n'est pas grave.
            } catch (Exception e) {
                throw new RuntimeException("Injection Navigator impossible dans " +
                        controller.getClass().getName() + " : " + e.getMessage(), e);
            }
        }

        // Injection DemoDataSeeder (pour SetupWizardController)
        if (this.demoDataSeeder != null) {
            try {
                Method m = controller.getClass().getMethod("setDemoDataSeeder", DemoDataSeeder.class);
                m.invoke(controller, this.demoDataSeeder);
            } catch (NoSuchMethodException e) {
                // Le contrôleur ne gère pas DemoDataSeeder (normal pour la plupart)
            } catch (Exception e) {
                throw new RuntimeException("Injection DemoDataSeeder impossible dans " +
                        controller.getClass().getName() + " : " + e.getMessage(), e);
            }
        }
    }

    // =========================================================
    // ======================== Erreurs =========================
    // =========================================================

    private void showStartupError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur Critique");
        alert.setHeaderText("Échec du démarrage de l'application");

        String message = (e.getMessage() != null) ? e.getMessage() : e.toString();
        Throwable cause = e.getCause();
        while (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null && !causeMsg.isBlank() && !message.contains(causeMsg)) {
                message += "\nCause: " + causeMsg;
            }
            cause = cause.getCause();
        }

        alert.setContentText("Impossible de démarrer l'application.\nErreur: " + message);
        alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(e.toString())));
        alert.showAndWait();
    }

    private static DashboardService createSimpleDashboardService(
            DocumentRepository docRepo,
            AdherentRepository adhRepo,
            EmpruntRepository empRepo,
            PenaliteRepository penRepo
    ) {
        return new DashboardService() {
            @Override public int countDocuments() { return (int) docRepo.count(); }
            @Override public int countAdherents() { return (int) adhRepo.count(); }
            @Override public int countEmprunts() { return empRepo.findActiveEmprunts().size(); }
            @Override public double sumPenalites() {
                return penRepo.findAll().stream()
                        .filter(p -> p.getDateFin() == null)
                        .map(Penalite::getMontant)
                        .mapToDouble(m -> m.amount().doubleValue())
                        .sum();
            }
            @Override public int countRetards() {
                return empRepo.findLateEmprunts(LocalDate.now()).size();
            }
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
