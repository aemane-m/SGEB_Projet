package application.ui.viewmodel.controller;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;
import application.ui.viewmodel.EmpruntsViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Contrôleur de la vue "Emprunts".
 *
 * - Liste des emprunts (avec pénalités courantes)
 * - Mode "Nouvel emprunt"
 * - Mode "Retour" (par sélection ou via combo d'emprunts actifs)
 */
public final class EmpruntsController {

    // =====================================================================
    // FXML : Table & actions globales
    // =====================================================================

    @FXML private TableView<Emprunt> table;
    @FXML private TableColumn<Emprunt, String> colId;
    @FXML private TableColumn<Emprunt, String> colDocument;
    @FXML private TableColumn<Emprunt, String> colEmprunteur;
    @FXML private TableColumn<Emprunt, String> colDateEmprunt;
    @FXML private TableColumn<Emprunt, String> colDatePrevue;
    @FXML private TableColumn<Emprunt, String> colDateRetour;
    @FXML private TableColumn<Emprunt, String> colStatut;
    @FXML private TableColumn<Emprunt, String> colPenaliteCourante;

    @FXML private ToggleButton tgEmprunts;
    @FXML private ToggleButton tgRetours;

    @FXML private CheckBox chkActifsOnly;
    @FXML private CheckBox chkOverdueOnly;

    @FXML private ProgressIndicator progress;
    @FXML private Button btnRefresh;
    @FXML private Button btnReturn; // bouton "Retourner" en haut de la table (mode Retour)

    // =====================================================================
    // FXML : Formulaire (TitledPane partagé Emprunt / Retour)
    // =====================================================================

    @FXML private TitledPane formPane;

    // --- Bloc Emprunt ---
    @FXML private Label lblAdherent;
    @FXML private ComboBox<Adherent> cbAdherent;
    @FXML private Label lblDocument;
    @FXML private ComboBox<Document> cbDocument;
    @FXML private Label lblDateEmprunt;
    @FXML private DatePicker dpEmprunt;
    @FXML private Label lblDuree;
    @FXML private Spinner<Integer> spDureeJours;
    @FXML private Label lblRetourPrevu;
    @FXML private DatePicker dpRetourPrevu;
    @FXML private Button btnBorrow;

    // --- Bloc Retour ---
    @FXML private Label lblEmpruntActif;
    @FXML private ComboBox<Emprunt> cbEmpruntsActifs;
    @FXML private Label lblDateRetour;
    @FXML private DatePicker dpRetourEffectif;
    @FXML private Button btnMarkReturn;

    // --- Messages ---
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    // =====================================================================
    // ViewModel & état local
    // =====================================================================

    private EmpruntsViewModel viewModel;
    /** true = mode "Retour", false = mode "Emprunt". */
    private final BooleanProperty modeRetours = new SimpleBooleanProperty(false);

    // =====================================================================
    // Construction / injection
    // =====================================================================

    public EmpruntsController() {
        // requis par FXMLLoader
    }

    public EmpruntsController(EmpruntsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setViewModel(EmpruntsViewModel vm) {
        this.viewModel = vm;
    }

    // =====================================================================
    // Initialisation FXML
    // =====================================================================

    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException(
                    "EmpruntsViewModel non injecté. " +
                    "Utilise le constructeur (EmpruntsViewModel) ou setViewModel() avant le chargement FXML."
            );
        }

        setupTableColumns();
        setupTogglesAndFilters();
        setupActionButtons();
        setupForm();
        bindUIControls();

        // Mode par défaut : "Emprunts"
        updateFormMode(false);

        // Chargement initial
        viewModel.refresh();
    }

    // =====================================================================
    // Table
    // =====================================================================

    private void setupTableColumns() {
        table.setItems(viewModel.getEmprunts());
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> viewModel.setSelected(newVal));

        colId.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getId() == null
                                ? "—"
                                : cd.getValue().getId().toString()
                )
        );

        colDocument.setCellValueFactory(cd -> {
            Document doc = cd.getValue().getDocument();
            return new ReadOnlyStringWrapper(doc == null ? "—" : safe(doc.getTitre()));
        });

        colEmprunteur.setCellValueFactory(cd -> {
            Adherent adh = cd.getValue().getEmprunteur();
            return new ReadOnlyStringWrapper(adh == null ? "—" : safe(adh.getNomComplet()));
        });

        colDateEmprunt.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(toIso(cd.getValue().getDateEmprunt()))
        );
        colDatePrevue.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourPrevue()))
        );
        colDateRetour.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourEffective()))
        );

        colStatut.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().statut(LocalDate.now()).name()
                )
        );

        colPenaliteCourante.setCellValueFactory(cd -> {
            Emprunt emprunt = cd.getValue();
            Money penalite = emprunt.penaliteCourante(LocalDate.now());
            if (penalite == null || !penalite.isPositive()) {
                return new ReadOnlyStringWrapper("—");
            }
            return new ReadOnlyStringWrapper(penalite.toString());
        });
    }

    // =====================================================================
    // Mode Emprunts / Retours + filtres
    // =====================================================================

    private void setupTogglesAndFilters() {
        ToggleGroup group = new ToggleGroup();
        tgEmprunts.setToggleGroup(group);
        tgRetours.setToggleGroup(group);

        tgEmprunts.setSelected(true);
        modeRetours.bind(tgRetours.selectedProperty());

        // Bouton "Retourner" en haut visible uniquement en mode Retour
        btnReturn.visibleProperty().bind(modeRetours);
        btnReturn.managedProperty().bind(modeRetours);

        // Filtre "Actifs uniquement"
        chkActifsOnly.selectedProperty().bindBidirectional(viewModel.actifsOnlyProperty());

        // Filtre "En retard uniquement"
        if (chkOverdueOnly != null) {
            chkOverdueOnly.selectedProperty().bindBidirectional(viewModel.overdueOnlyProperty());
        }

        // Changement de mode : met à jour l'UI + rafraîchit la table
        group.selectedToggleProperty().addListener((obs, old, cur) -> {
            boolean isRetour = (cur == tgRetours);
            updateFormMode(isRetour);

            if (isRetour) {
                // En mode Retour, on force le filtre "actifs"
                viewModel.actifsOnlyProperty().set(true);
            }

            viewModel.refresh();
        });

        // Désactive certains filtres selon le mode
        chkActifsOnly.disableProperty().bind(modeRetours); // en mode retour, toujours "actifs = true"
        if (chkOverdueOnly != null) {
            chkOverdueOnly.visibleProperty().bind(modeRetours.not());
            chkOverdueOnly.managedProperty().bind(chkOverdueOnly.visibleProperty());
        }
    }

    /**
     * Bascule le formulaire entre :
     * - "Nouvel emprunt"
     * - "Enregistrer un retour"
     */
    private void updateFormMode(boolean isRetour) {
        if (formPane == null) return;

        formPane.setText(isRetour ? "Enregistrer un retour" : "Nouvel emprunt");

        // Bloc Emprunt
        setVisibleManaged(lblAdherent,    !isRetour);
        setVisibleManaged(cbAdherent,     !isRetour);
        setVisibleManaged(lblDocument,    !isRetour);
        setVisibleManaged(cbDocument,     !isRetour);
        setVisibleManaged(lblDateEmprunt, !isRetour);
        setVisibleManaged(dpEmprunt,      !isRetour);
        setVisibleManaged(lblDuree,       !isRetour);
        setVisibleManaged(spDureeJours,   !isRetour);
        setVisibleManaged(lblRetourPrevu, !isRetour);
        setVisibleManaged(dpRetourPrevu,  !isRetour);
        setVisibleManaged(btnBorrow,      !isRetour);

        // Bloc Retour
        setVisibleManaged(lblEmpruntActif,   isRetour);
        setVisibleManaged(cbEmpruntsActifs,  isRetour);
        setVisibleManaged(lblDateRetour,     isRetour);
        setVisibleManaged(dpRetourEffectif,  isRetour);
        setVisibleManaged(btnMarkReturn,     isRetour);

        if (isRetour && dpRetourEffectif != null && dpRetourEffectif.getValue() == null) {
            dpRetourEffectif.setValue(LocalDate.now());
        }
    }

    // =====================================================================
    // Boutons d’action (haut + formulaire)
    // =====================================================================

    private void setupActionButtons() {
        btnRefresh.setOnAction(e -> viewModel.refresh());

        // Bouton "Retourner" (en haut, mode Retour)
        btnReturn.setOnAction(e -> {
            Emprunt selected = viewModel.getSelected();
            if (selected == null || selected.getId() == null) {
                viewModel.reportError("Veuillez sélectionner un emprunt en cours dans la table.");
                return;
            }
            if (!selected.estEnCours()) {
                viewModel.reportError("Seuls les emprunts en cours peuvent être retournés.");
                return;
            }

            LocalDate dateEmprunt = selected.getDateEmprunt();
            if (dateEmprunt == null) {
                viewModel.reportError("Date d'emprunt inconnue pour cet emprunt.");
                return;
            }

            promptForReturnDate(dateEmprunt, date ->
                    viewModel.returnEmprunt(selected.getId(), date)
            );
        });

        // Désactivation dynamique du bouton de retour (haut)
        BooleanBinding btnReturnDisabled = modeRetours.not()
                .or(viewModel.loadingProperty())
                .or(Bindings.createBooleanBinding(
                        () -> {
                            Emprunt sel = viewModel.getSelected();
                            return sel == null || !sel.estEnCours();
                        },
                        viewModel.selectedProperty()
                ));

        btnReturn.disableProperty().bind(btnReturnDisabled);
    }

    // =====================================================================
    // Formulaire Emprunt / Retour
    // =====================================================================

    private void setupForm() {
        if (formPane == null) return;

        // -----------------------------
        // Section Emprunt
        // -----------------------------
        cbAdherent.setItems(viewModel.getAdherents());
        cbDocument.setItems(viewModel.getAvailableDocuments());

        cbAdherent.setConverter(new StringConverter<>() {
            @Override
            public String toString(Adherent a) {
                if (a == null) return "";
                return a.getNomComplet() + " (ID: " + a.getId() + ")";
            }
            @Override
            public Adherent fromString(String s) { return null; }
        });

        cbDocument.setConverter(new StringConverter<>() {
            @Override
            public String toString(Document d) {
                if (d == null) return "";
                return safe(d.getTitre()) + " (ID: " + d.getId() + ")";
            }
            @Override
            public Document fromString(String s) { return null; }
        });

        spDureeJours.setValueFactory(new IntegerSpinnerValueFactory(1, 90, 21));
        dpEmprunt.setValue(LocalDate.now());
        dpRetourPrevu.setEditable(false);
        dpRetourPrevu.setDisable(true);

        final Runnable recomputeRetourPrevu = () -> {
            LocalDate start = dpEmprunt.getValue();
            Integer days = spDureeJours.getValue();
            dpRetourPrevu.setValue((start != null && days != null) ? start.plusDays(days) : null);
        };

        dpEmprunt.valueProperty().addListener((o, ov, nv) -> recomputeRetourPrevu.run());
        spDureeJours.valueProperty().addListener((o, ov, nv) -> recomputeRetourPrevu.run());
        recomputeRetourPrevu.run();

        btnBorrow.disableProperty().bind(
                viewModel.loadingProperty()
                        .or(cbAdherent.valueProperty().isNull())
                        .or(cbDocument.valueProperty().isNull())
                        .or(dpEmprunt.valueProperty().isNull())
        );

        btnBorrow.setOnAction(e -> {
            Adherent adh = cbAdherent.getValue();
            Document doc = cbDocument.getValue();
            LocalDate start = dpEmprunt.getValue();

            if (adh == null || adh.getId() == null
                    || doc == null || doc.getId() == null
                    || start == null) {
                return;
            }

            viewModel.borrow(adh.getId(), doc.getId(), start);
        });

        // -----------------------------
        // Section Retour (formulaire bas)
        // -----------------------------

        cbEmpruntsActifs.setItems(viewModel.getActiveEmpruntsList());
        cbEmpruntsActifs.setConverter(new StringConverter<>() {
            @Override
            public String toString(Emprunt emprunt) {
                if (emprunt == null) return "";
                Document d = emprunt.getDocument();
                Adherent a = emprunt.getEmprunteur();
                return String.format(
                        "Doc: %s (par %s)",
                        d == null ? "—" : safe(d.getTitre()),
                        a == null ? "—" : safe(a.getNomComplet())
                );
            }
            @Override
            public Emprunt fromString(String string) { return null; }
        });

        btnMarkReturn.setOnAction(e -> {
            Emprunt emprunt = cbEmpruntsActifs.getValue();
            LocalDate dateRetour = dpRetourEffectif.getValue();

            if (dateRetour == null) {
                viewModel.reportError("Veuillez sélectionner une date de retour.");
                return;
            }
            if (emprunt == null || emprunt.getId() == null) {
                viewModel.reportError("Veuillez sélectionner un emprunt à retourner.");
                return;
            }

            viewModel.returnEmprunt(emprunt.getId(), dateRetour);
            cbEmpruntsActifs.setValue(null);
        });

        btnMarkReturn.disableProperty().bind(
                viewModel.loadingProperty()
                        .or(cbEmpruntsActifs.valueProperty().isNull())
                        .or(dpRetourEffectif.valueProperty().isNull())
        );
    }

    // =====================================================================
    // Binding global (erreurs / succès / chargement)
    // =====================================================================

    private void bindUIControls() {
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());

        errorLabel.textProperty().bind(viewModel.errorProperty());
        successLabel.textProperty().bind(viewModel.successProperty());

        // Visibilité exclusive des messages
        BooleanBinding hasError   = viewModel.errorProperty().isNotEmpty();
        BooleanBinding hasSuccess = viewModel.successProperty().isNotEmpty();

        errorLabel.visibleProperty().bind(hasError.and(hasSuccess.not()));
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        successLabel.visibleProperty().bind(hasSuccess.and(hasError.not()));
        successLabel.managedProperty().bind(successLabel.visibleProperty());

        btnRefresh.disableProperty().bind(viewModel.loadingProperty());
    }

    // =====================================================================
    // Dialogue de saisie de date de retour
    // =====================================================================

    private void promptForReturnDate(LocalDate minDate, Consumer<LocalDate> onConfirm) {
        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Confirmer le retour");
        dlg.setHeaderText("Veuillez sélectionner la date de retour effective.");
        addTheme(dlg.getDialogPane());

        VBox content = new VBox(10);
        DatePicker datePicker = new DatePicker(LocalDate.now());

        if (minDate == null) {
            minDate = LocalDate.MIN;
        }

        LocalDate finalMinDate = minDate;
        datePicker.setDayCellFactory(d -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setDisable(true);
                } else {
                    setDisable(item.isBefore(finalMinDate) || item.isAfter(LocalDate.now()));
                }
            }
        });

        content.getChildren().addAll(new Label("Date de retour :"), datePicker);
        content.setPadding(new Insets(10));

        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleAsPrimary(dlg.getDialogPane(), ButtonType.OK);

        dlg.setResultConverter(btnType ->
                (btnType == ButtonType.OK) ? datePicker.getValue() : null
        );

        Optional<LocalDate> result = dlg.showAndWait();
        result.ifPresent(onConfirm);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static String toIso(LocalDate d) {
        return d == null ? "—" : d.toString();
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "" : s.trim();
    }

    private void setVisibleManaged(Node node, boolean value) {
        if (node != null) {
            node.setVisible(value);
            node.setManaged(value);
        }
    }

    // --- Helpers CSS (comme dans les autres contrôleurs) ---

    private void addTheme(DialogPane pane) {
        try {
            URL css = getClass().getResource("/application/ui/css/style.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
                pane.getStyleClass().add("entity-dialog");
            }
        } catch (Exception ignored) {
            System.err.println("Warning: Could not load CSS for dialog.");
        }
    }

    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-primary");
            button.setDefaultButton(true);
        }
    }
}