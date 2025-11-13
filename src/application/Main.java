package application;

// --- JavaFX Imports ---
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
// --- Application Specific Imports ---
import application.domain.core.emprunt.Penalite;

import application.domain.ports.repository.*;
import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.storage.CoverStoragePort;
import application.domain.service.*; // Importe tous les services

// CORRIGÉ: Import specific repository implementations
import application.infrastructure.external.OpenLibraryProvider;
import application.infrastructure.external.LocalCoverStorageAdapter; // Chemin corrigé
import application.infrastructure.sqlite.DatabaseInitializer;
import application.infrastructure.sqlite.SqliteAdherentRepository;
import application.infrastructure.sqlite.SqliteDocumentRepository;
import application.infrastructure.sqlite.SqliteEmpruntRepository;
import application.infrastructure.sqlite.SqlitePenaliteRepository;
import application.infrastructure.sqlite.AccountServiceSqlite;

import application.ui.navigation.Navigator;
import application.ui.navigation.fx.FxNavigator;
import application.ui.viewmodel.controller.LoginController;

// --- Standard Java Imports ---
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
// (Imports ArrayList/List supprimés car non utilisés)

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Global exception handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread " + t.getName());
            e.printStackTrace();
            Platform.runLater(() -> showStartupError(new RuntimeException("An unexpected error occurred.", e)));
        });

        try {
            // --- DATABASE INITIALIZATION ---
            System.out.println("Initializing database schema...");
            DatabaseInitializer.initialize(); // <<< MUST BE CALLED FIRST
            System.out.println("Database schema initialized successfully.");

            // --- DEPENDENCY ASSEMBLY (using real implementations) ---

            // 1) Repositories (Infrastructure Layer)
            final AdherentRepository adherentRepository = new SqliteAdherentRepository();
            final DocumentRepository documentRepository = new SqliteDocumentRepository();
            final EmpruntRepository empruntRepository = new SqliteEmpruntRepository();
            final PenaliteRepository penaliteRepository = new SqlitePenaliteRepository();

            // 2) Services Externes (Infrastructure Layer)
            final BookInfoProvider bookInfoProvider = new OpenLibraryProvider();
            final CoverStoragePort coverStorage = new LocalCoverStorageAdapter(); // S'assure que ./covers existe

            // 3) Services (Domain Layer - using REAL implementations)
            final DashboardService dashboardService = createSimpleDashboardService(documentRepository, adherentRepository, empruntRepository, penaliteRepository);
            
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

            // CORRIGÉ : Création du nouveau PenaliteService
            final PenaliteService penaliteService = new DefaultPenaliteService(penaliteRepository);


            // 4) Navigator (UI Layer) - Inject necessary services/repositories
            // CORRIGÉ : Passe TOUTES les dépendances nécessaires au Navigator
            final Navigator navigator = new FxNavigator(
                primaryStage,
                dashboardService,
                catalogueService,
                empruntService,
                penaliteService,    // <-- AJOUTÉ
                adherentRepository,
                documentRepository,
                empruntRepository,
                penaliteRepository,
                bookInfoProvider
            );

            // 5) Account Service (Specific Infrastructure/Domain Service)
            final AccountService accountService = new AccountServiceSqlite(navigator);

            // --- END OF ASSEMBLY ---

            // 6) Load Login FXML
            final URL fxmlUrl = getClass().getResource("/application/ui/view/LoginView.fxml");
            if (fxmlUrl == null) {
                throw new IOException("FXML resource not found: /application/ui/view/LoginView.fxml");
            }
            final FXMLLoader loader = new FXMLLoader(fxmlUrl);
            final Parent root = loader.load();

            // 7) Inject AccountService into LoginController
            final LoginController controller = loader.getController();
            if (controller == null) {
                throw new IllegalStateException("LoginController not found (fx:controller missing or incorrect?)");
            }
            controller.setAccountService(accountService);

            // 8) Setup and Show Login Stage
            final Scene scene = new Scene(root, 600, 420); // Login window size
            primaryStage.setTitle("SGEB - Connexion");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Login window is fixed size
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            // Catch initialization errors
            System.err.println("Application startup failed.");
            e.printStackTrace();
            showStartupError(e);
            Platform.exit();
            System.exit(1);
        }
    }

    /** Displays a critical error message dialog during startup. */
    private void showStartupError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur Critique");
        alert.setHeaderText("Échec du démarrage de l'application");
        String message = e.getMessage() != null ? e.getMessage() : e.toString();
        Throwable cause = e.getCause();
        // Include cause messages for better debugging
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

    // --- Simple Service Implementations (Placeholders) ---

    private static DashboardService createSimpleDashboardService(
            DocumentRepository docRepo, AdherentRepository adhRepo,
            EmpruntRepository empRepo, PenaliteRepository penRepo) {
        return new DashboardService() {
            @Override public int countDocuments() { return (int) docRepo.count(); }
            @Override public int countAdherents() { return (int) adhRepo.count(); }
            @Override public int countEmprunts() { return empRepo.findActiveEmprunts().size(); }
            @Override public double sumPenalites() {
                return penRepo.findAll().stream()
                       .filter(p -> p.getDateFin() == null) // Assuming null date_fin means unpaid
                       .map(Penalite::getMontant)           // Get Money object
                       .mapToDouble(m -> m.amount().doubleValue()) // Get double value
                       .sum();
            }
            @Override public int countRetards() { return empRepo.findLateEmprunts(LocalDate.now()).size(); } // Needs date
        };
    }

    // --- Main Entry Point ---

    public static void main(String[] args) {
        launch(args);
    }
}

