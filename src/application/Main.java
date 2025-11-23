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
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import application.domain.ports.storage.CoverStoragePort;
import application.domain.service.AccountService;
import application.domain.service.CatalogueService;
import application.domain.service.DashboardService;
import application.domain.service.DefaultCatalogueService;
import application.domain.service.DefaultEmpruntService;
import application.domain.service.DefaultPenaliteService;
import application.domain.service.EmpruntService;
import application.domain.service.PenaliteService;
import application.infrastructure.external.LocalCoverStorageAdapter;
import application.infrastructure.external.OpenLibraryProvider;
import application.infrastructure.sqlite.AccountServiceSqlite;
import application.infrastructure.sqlite.DatabaseInitializer;
import application.infrastructure.sqlite.DemoDataSeeder;
import application.infrastructure.sqlite.SqliteAdherentRepository;
import application.infrastructure.sqlite.SqliteDocumentRepository;
import application.infrastructure.sqlite.SqliteEmpruntRepository;
import application.infrastructure.sqlite.SqlitePenaliteRepository;
import application.ui.navigation.Navigator;
import application.ui.navigation.fx.FxNavigator;

// --- Standard Java Imports ---
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDate;

/**
 * Point d'entrée principal de l'application JavaFX SGEB.
 * <p>
 * Cette classe :
 * <ul>
 *     <li>Initialise la base de données SQLite (schéma, tables...)</li>
 *     <li>Crée les repositories (accès aux données) et services métier</li>
 *     <li>Configure le système de navigation JavaFX via {@link Navigator}</li>
 *     <li>Décide quel écran afficher au démarrage :
 *         <ul>
 *             <li>Assistant de configuration si aucun compte n'existe</li>
 *             <li>Landing page si au moins un compte existe</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public class Main extends Application {

    /**
     * Gestionnaire de navigation (shell principal) permettant
     * de changer de vue (écrans) dans l'application JavaFX.
     */
    private Navigator navigator;

    /**
     * Service de gestion des comptes (authentification, création
     * d'administrateurs et d'adhérents, etc.).
     */
    private AccountService accountService;

    /**
     * Composant permettant de remplir la base avec des données
     * de démonstration (documents, adhérents, emprunts...).
     * <p>
     * Ce seeder est généralement utilisé après la création
     * du premier compte administrateur.
     */
    private DemoDataSeeder demoDataSeeder;

    /**
     * Méthode appelée automatiquement par JavaFX au démarrage de l'application.
     *
     * @param primaryStage fenêtre principale de l'application
     */
    @Override
    public void start(Stage primaryStage) {
        // Gestionnaire global des exceptions non interceptées (non catchées).
        // Cela permet d'afficher une alerte au lieu de laisser l'application planter silencieusement.
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread " + t.getName());
            e.printStackTrace();
            Platform.runLater(() ->
                    showStartupError(new RuntimeException("An unexpected error occurred.", e))
            );
        });

        try {
            // 1) Initialisation de la base de données
            System.out.println("Initializing database schema...");
            DatabaseInitializer.initialize();
            System.out.println("Database schema initialized successfully.");

            // 2) Assemblage des dépendances : repositories + services

            // Repositories (implémentations SQLite)
            final AdherentRepository adherentRepository = new SqliteAdherentRepository();
            final DocumentRepository documentRepository = new SqliteDocumentRepository();
            final EmpruntRepository empruntRepository   = new SqliteEmpruntRepository();
            final PenaliteRepository penaliteRepository = new SqlitePenaliteRepository();

            // Services externes (API OpenLibrary + stockage local des couvertures)
            final BookInfoProvider bookInfoProvider = new OpenLibraryProvider();
            final CoverStoragePort coverStorage     = new LocalCoverStorageAdapter(); // stocke les couvertures dans ./covers

            // Services métier (logique applicative)
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

            // 2bis) Service de comptes (authentification / gestion des utilisateurs)
            // On le crée avant le Navigator, car le Navigator en a besoin.
            this.accountService = new AccountServiceSqlite();

            // 2ter) Seeder de données de démo
            this.demoDataSeeder = new DemoDataSeeder();

            // 3) Création du Navigator JavaFX (shell principal de l'app)
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

            // 4) Vérification du premier démarrage : au moins un utilisateur existe-t-il ?
            boolean hasAnyUser = this.accountService.countUsers() > 0;

            // 5) Choix de l'écran de démarrage
            if (!hasAnyUser) {
                // Aucun compte -> assistant de configuration initiale (création du 1er admin)
                showSetupWizard(primaryStage);
            } else {
                // Au moins un compte -> landing page (présentation + login / inscription adhérent)
                showLanding(primaryStage);
            }

        } catch (Exception e) {
            System.err.println("Application startup failed.");
            e.printStackTrace();
            showStartupError(e);
            // On ferme proprement l'application en cas d'erreur critique au démarrage.
            Platform.exit();
            System.exit(1);
        }
    }

    // =========================================================
    // =========== Écrans de démarrage (Landing / Setup) =======
    // =========================================================

    /**
     * Affiche l'écran de Landing (page d'accueil) lorsque l'application démarre
     * et qu'au moins un compte existe déjà dans la base.
     *
     * @param stage fenêtre principale
     * @throws IOException si le fichier FXML n'est pas trouvé ou ne peut pas être chargé
     */
    private void showLanding(Stage stage) throws IOException {
        URL fxmlUrl = getClass().getResource("/application/ui/view/LandingView.fxml");
        if (fxmlUrl == null) {
            throw new IOException("FXML resource not found: /application/ui/view/LandingView.fxml");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // Injection des dépendances nécessaires dans le contrôleur (AccountService, Navigator, etc.)
        injectDependencies(loader);

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("SGEB – Bibliothèque");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Affiche l'assistant de configuration initiale (Setup Wizard) lorsque
     * l'application démarre sans qu'aucun compte n'existe encore.
     *
     * @param stage fenêtre principale
     * @throws IOException si le fichier FXML n'est pas trouvé ou ne peut pas être chargé
     */
    private void showSetupWizard(Stage stage) throws IOException {
        URL fxmlUrl = getClass().getResource("/application/ui/view/SetupWizardView.fxml");
        if (fxmlUrl == null) {
            throw new IOException("FXML resource not found: /application/ui/view/SetupWizardView.fxml");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // Injection des dépendances (AccountService, Navigator, DemoDataSeeder)
        injectDependencies(loader);

        Scene scene = new Scene(root, 600, 420);
        stage.setTitle("SGEB – Configuration initiale");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Injecte dynamiquement certaines dépendances dans le contrôleur FXML
     * chargé par le {@link FXMLLoader}.
     * <p>
     * La méthode utilise la réflexion pour appeler, si elles existent :
     * <ul>
     *     <li>{@code setAccountService(AccountService)}</li>
     *     <li>{@code setNavigator(Navigator)}</li>
     *     <li>{@code setDemoDataSeeder(DemoDataSeeder)}</li>
     * </ul>
     * sur le contrôleur associé au FXML.
     *
     * @param loader chargeur FXML déjà initialisé et ayant un contrôleur
     */
    private void injectDependencies(FXMLLoader loader) {
        Object controller = loader.getController();
        if (controller == null) {
            return;
        }

        // Injection de l'AccountService (si le contrôleur le demande)
        if (this.accountService != null) {
            try {
                Method m = controller.getClass().getMethod("setAccountService", AccountService.class);
                m.invoke(controller, this.accountService);
            } catch (NoSuchMethodException e) {
                // Le contrôleur ne déclare pas setAccountService : pas grave.
            } catch (Exception e) {
                throw new RuntimeException("Injection AccountService impossible dans "
                        + controller.getClass().getName() + " : " + e.getMessage(), e);
            }
        }

        // Injection du Navigator (si le contrôleur le demande)
        if (this.navigator != null) {
            try {
                Method m = controller.getClass().getMethod("setNavigator", Navigator.class);
                m.invoke(controller, this.navigator);
            } catch (NoSuchMethodException e) {
                // Le contrôleur ne déclare pas setNavigator : pas grave.
            } catch (Exception e) {
                throw new RuntimeException("Injection Navigator impossible dans "
                        + controller.getClass().getName() + " : " + e.getMessage(), e);
            }
        }

        // Injection du DemoDataSeeder (typiquement dans SetupWizardController)
        if (this.demoDataSeeder != null) {
            try {
                Method m = controller.getClass().getMethod("setDemoDataSeeder", DemoDataSeeder.class);
                m.invoke(controller, this.demoDataSeeder);
            } catch (NoSuchMethodException e) {
                // Le contrôleur ne déclare pas setDemoDataSeeder : normal pour la plupart.
            } catch (Exception e) {
                throw new RuntimeException("Injection DemoDataSeeder impossible dans "
                        + controller.getClass().getName() + " : " + e.getMessage(), e);
            }
        }
    }

    // =========================================================
    // ======================== Erreurs =========================
    // =========================================================

    /**
     * Affiche une boîte de dialogue en cas d'erreur critique au démarrage.
     * <p>
     * L'utilisateur voit un message simple, et il peut dérouler une zone
     * de texte pour voir plus de détails techniques sur l'exception.
     *
     * @param e exception qui a provoqué l'échec du démarrage
     */
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

        // Zone déroulante avec plus de détails sur l'erreur
        TextArea details = new TextArea(e.toString());
        details.setEditable(false);
        details.setWrapText(true);

        ScrollPane scrollPane = new ScrollPane(details);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        alert.getDialogPane().setExpandableContent(scrollPane);
        alert.showAndWait();
    }

    /**
     * Crée une implémentation simple de {@link DashboardService} en se basant
     * directement sur les repositories passés en paramètre.
     * <p>
     * Cette implémentation est utilisée pour alimenter le tableau de bord
     * (nombre de documents, d'adhérents, emprunts en cours, pénalités, etc.).
     *
     * @param docRepo repository des documents
     * @param adhRepo repository des adhérents
     * @param empRepo repository des emprunts
     * @param penRepo repository des pénalités
     * @return une implémentation anonyme de {@link DashboardService}
     */
    private static DashboardService createSimpleDashboardService(
            DocumentRepository docRepo,
            AdherentRepository adhRepo,
            EmpruntRepository empRepo,
            PenaliteRepository penRepo
    ) {
        return new DashboardService() {
            @Override
            public int countDocuments() {
                return (int) docRepo.count();
            }

            @Override
            public int countAdherents() {
                return (int) adhRepo.count();
            }

            @Override
            public int countEmprunts() {
                return empRepo.findActiveEmprunts().size();
            }

            @Override
            public double sumPenalites() {
                return penRepo.findAll().stream()
                        .filter(p -> p.getDateFin() == null) // pénalités toujours actives
                        .map(Penalite::getMontant)
                        .mapToDouble(m -> m.amount().doubleValue())
                        .sum();
            }

            @Override
            public int countRetards() {
                return empRepo.findLateEmprunts(LocalDate.now()).size();
            }
        };
    }

    /**
     * Méthode main standard : délègue le lancement à JavaFX via {@link #launch(String...)}.
     *
     * @param args arguments de la ligne de commande (non utilisés ici)
     */
    public static void main(String[] args) {
        launch(args);
    }
}