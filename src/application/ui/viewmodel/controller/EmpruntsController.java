package application.ui.viewmodel.controller;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money; // <-- Ajouté
import application.ui.viewmodel.EmpruntsViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets; // Ajouté
import javafx.scene.Node; // Ajouté
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.VBox; // Ajouté
import javafx.util.StringConverter;

import java.net.URL; // Ajouté
import java.time.LocalDate;
import java.util.Optional; // Ajouté
import java.util.function.Consumer; // Ajouté

/**
 * Controller pour la vue des Emprunts.
 * Gère la logique d'emprunt, de retour (sélection ou ID),
 * et le basculement entre les modes.
 */
public class EmpruntsController {

    // ====== FXML Table & Actions ======
    @FXML private TableView<Emprunt> table;
    @FXML private TableColumn<Emprunt, String>  colId;
    @FXML private TableColumn<Emprunt, String>  colDocument;
    @FXML private TableColumn<Emprunt, String>  colEmprunteur;
    @FXML private TableColumn<Emprunt, String>  colDateEmprunt;
    @FXML private TableColumn<Emprunt, String>  colDatePrevue;
    @FXML private TableColumn<Emprunt, String>  colDateRetour;
    @FXML private TableColumn<Emprunt, String>  colStatut;
    @FXML private TableColumn<Emprunt, String>  colPenaliteCourante;

    @FXML private ToggleButton tgEmprunts;
    @FXML private ToggleButton tgRetours;

    @FXML private CheckBox chkActifsOnly;
    @FXML private CheckBox chkOverdueOnly;
    @FXML private ProgressIndicator progress;
    @FXML private Button btnRefresh;
    @FXML private Button btnReturn;

    // ====== FXML Formulaire (partagé) ======
    @FXML private TitledPane formPane;
    // --- Champs Emprunt ---
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
    // --- Champs Retour ---
    
    @FXML private Label lblEmpruntActif; // <-- Changé
    @FXML private ComboBox<Emprunt> cbEmpruntsActifs; // <-- Changé
    @FXML private Label lblDateRetour;
    @FXML private DatePicker dpRetourEffectif;
    @FXML private Button btnMarkReturn;
    
    // --- Champs Message ---
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private EmpruntsViewModel viewModel;
    private final BooleanProperty modeRetours = new SimpleBooleanProperty(false);

    // --- Constructeurs ---
    public EmpruntsController() { }
    public EmpruntsController(EmpruntsViewModel viewModel) { this.viewModel = viewModel; }
    public void setViewModel(EmpruntsViewModel vm) { this.viewModel = vm; }

    @FXML
    private void initialize() {
        if (viewModel == null) {
            // Crée un VM vide si non injecté (pour SceneBuilder)
            viewModel = new EmpruntsViewModel(); 
        }

        setupTableColumns();
        setupTogglesAndFilters();
        setupActionButtons();
        setupForm();
        bindUIControls();
        
        // Initialise le mode (Emprunt)
        updateFormMode(false);
    }

    private void setupTableColumns() {
        table.setItems(viewModel.getEmprunts());
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> viewModel.setSelected(n));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        colId.setCellValueFactory(cd -> new ReadOnlyStringWrapper(String.valueOf(cd.getValue().getId())));
        colDocument.setCellValueFactory(cd -> new ReadOnlyStringWrapper(safe(cd.getValue().getDocument().getTitre())));
        colEmprunteur.setCellValueFactory(cd -> new ReadOnlyStringWrapper(safe(cd.getValue().getEmprunteur().getNomComplet())));
        colDateEmprunt.setCellValueFactory(cd -> new ReadOnlyStringWrapper(toIso(cd.getValue().getDateEmprunt())));
        colDatePrevue.setCellValueFactory(cd -> new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourPrevue())));
        colDateRetour.setCellValueFactory(cd -> new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourEffective())));
        
        colStatut.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().statut(LocalDate.now()).name()));
        
        // Implémentation du calcul de pénalité courante
        colPenaliteCourante.setCellValueFactory(cd -> {
            Emprunt emprunt = cd.getValue();
            // Appelle la méthode de l'entité Emprunt
            Money penalite = emprunt.penaliteCourante(LocalDate.now());
            
            if (penalite == null || !penalite.isPositive()) {
                return new ReadOnlyStringWrapper("—");
            }
            return new ReadOnlyStringWrapper(penalite.toString()); // Ex: "1.50 EUR"
        });
    }

    private void setupTogglesAndFilters() {
        ToggleGroup group = new ToggleGroup();
        tgEmprunts.setToggleGroup(group);
        tgRetours.setToggleGroup(group);
        tgEmprunts.setSelected(true);
        modeRetours.bind(tgRetours.selectedProperty());

        // Bouton "Retourner" (haut) est pour le mode Retour
        btnReturn.visibleProperty().bind(modeRetours);
        btnReturn.managedProperty().bind(modeRetours);
        
        // Filtre "Actifs"
        chkActifsOnly.selectedProperty().bindBidirectional(viewModel.actifsOnlyProperty());
        
        // Filtre "En retard"
        if (chkOverdueOnly != null) {
             chkOverdueOnly.selectedProperty().bindBidirectional(viewModel.overdueOnlyProperty());
        }

        // Changement de mode
        group.selectedToggleProperty().addListener((obs, old, cur) -> {
            boolean isRetour = (cur == tgRetours);
            updateFormMode(isRetour); // Met à jour l'UI du formulaire
            
            if (isRetour) {
                viewModel.actifsOnlyProperty().set(true); // Retour force Actifs
            }
            viewModel.refresh(); // Rafraîchit la table
        });
        
        // Désactive les filtres non pertinents
        chkActifsOnly.disableProperty().bind(modeRetours);
        if (chkOverdueOnly != null) {
            chkOverdueOnly.visibleProperty().bind(modeRetours.not()); // Visible seulement en mode Emprunt
            chkOverdueOnly.managedProperty().bind(chkOverdueOnly.visibleProperty());
        }
    }
    
    /** Met à jour le TitledPane pour basculer entre Emprunt et Retour */
    /** CORRIGÉ : Gère les nouveaux champs (cbEmpruntsActifs) */
    private void updateFormMode(boolean isRetour) {
        if (formPane == null) return;
        
        formPane.setText(isRetour ? "Enregistrer un retour" : "Nouvel emprunt");
        
        // Champs Emprunt
        lblAdherent.setVisible(!isRetour); lblAdherent.setManaged(!isRetour);
        cbAdherent.setVisible(!isRetour); cbAdherent.setManaged(!isRetour);
        lblDocument.setVisible(!isRetour); lblDocument.setManaged(!isRetour);
        cbDocument.setVisible(!isRetour); cbDocument.setManaged(!isRetour);
        lblDateEmprunt.setVisible(!isRetour); lblDateEmprunt.setManaged(!isRetour);
        dpEmprunt.setVisible(!isRetour); dpEmprunt.setManaged(!isRetour);
        lblDuree.setVisible(!isRetour); lblDuree.setManaged(!isRetour);
        spDureeJours.setVisible(!isRetour); spDureeJours.setManaged(!isRetour);
        lblRetourPrevu.setVisible(!isRetour); lblRetourPrevu.setManaged(!isRetour);
        dpRetourPrevu.setVisible(!isRetour); dpRetourPrevu.setManaged(!isRetour);
        btnBorrow.setVisible(!isRetour); btnBorrow.setManaged(!isRetour);
        
        // Champs Retour
        lblEmpruntActif.setVisible(isRetour); lblEmpruntActif.setManaged(isRetour);
        cbEmpruntsActifs.setVisible(isRetour); cbEmpruntsActifs.setManaged(isRetour);
        lblDateRetour.setVisible(isRetour); lblDateRetour.setManaged(isRetour);
        dpRetourEffectif.setVisible(isRetour); dpRetourEffectif.setManaged(isRetour);
        btnMarkReturn.setVisible(isRetour); btnMarkReturn.setManaged(isRetour);
        
        if (isRetour) {
            dpRetourEffectif.setValue(LocalDate.now());
        }
    }

    private void setupActionButtons() {
        btnRefresh.setOnAction(e -> viewModel.refresh());
        
        // CORRIGÉ : Ouvre un dialogue pour choisir la date
        btnReturn.setOnAction(e -> {
            Emprunt selected = viewModel.getSelected();
            if (selected != null) {
                promptForReturnDate(selected.getDateEmprunt(), date -> {
                    viewModel.returnEmprunt(selected.getId(), date);
                });
            } else {
                viewModel.reportError("Veuillez sélectionner un emprunt dans la table.");
            }
        });

        btnReturn.disableProperty().bind(
            modeRetours.not()
            .or(viewModel.loadingProperty())
            .or(Bindings.createBooleanBinding(
                () -> {
                    Emprunt sel = viewModel.getSelected();
                    return sel == null || !sel.estEnCours(); 
                },
                viewModel.selectedProperty()
            ))
        );
    }
    
    private void setupForm() {
        if (formPane == null) return;

        // --- Section Emprunt ---
        cbAdherent.setItems(viewModel.getAdherents());
        // CORRIGÉ : Utilise la liste des documents DISPONIBLES
        cbDocument.setItems(viewModel.getAvailableDocuments()); 

        // ... (Converters pour cbAdherent et cbDocument) ...
        cbAdherent.setConverter(new StringConverter<>() {
            @Override public String toString(Adherent a) { return (a == null) ? "" : a.getNomComplet() + " (ID: " + a.getId() + ")"; }
            @Override public Adherent fromString(String s) { return null; }
        });
        cbDocument.setConverter(new StringConverter<>() {
            @Override public String toString(Document d) { return (d == null) ? "" : d.getTitre() + " (ID: " + d.getId() + ")"; }
            @Override public Document fromString(String s) { return null; }
        });

        // ... (Logique des dates d'emprunt) ...
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

        // Bouton "Emprunter"
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
            if (adh == null || doc == null || start == null) return;
            viewModel.borrow(adh.getId(), doc.getId(), start);
        });
        
        // --- Section Retour (CORRIGÉ) ---
        
        // CORRIGÉ: Lie le ComboBox des retours à la liste des emprunts actifs
        cbEmpruntsActifs.setItems(viewModel.getActiveEmpruntsList()); 
        cbEmpruntsActifs.setConverter(new StringConverter<Emprunt>() {
            @Override
            public String toString(Emprunt emprunt) {
                if (emprunt == null) return "";
                // Utilise les getters des proxys (qui sont maintenant peuplés par le Repo)
                return String.format("Doc: %s (par: %s)",
                    safe(emprunt.getDocument().getTitre()),
                    safe(emprunt.getEmprunteur().getNomComplet())
                );
            }
            @Override public Emprunt fromString(String string) { return null; }
        });

        // Bouton "Retourner" (dans le formulaire)
        btnMarkReturn.setOnAction(e -> {
             // CORRIGÉ: Récupère l'Emprunt depuis le ComboBox
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
             
             // Appelle la méthode de retour standard avec l'ID de l'emprunt
             viewModel.returnEmprunt(emprunt.getId(), dateRetour);
             cbEmpruntsActifs.setValue(null); // Réinitialise le champ
        });
        
        // CORRIGÉ: Lie la désactivation au ComboBox, pas au TextField
        btnMarkReturn.disableProperty().bind(
            viewModel.loadingProperty()
            .or(cbEmpruntsActifs.valueProperty().isNull())
            .or(dpRetourEffectif.valueProperty().isNull())
        );
    }

    private void bindUIControls() {
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());

        // Gère les messages d'erreur OU de succès (exclusifs)
        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        
        if (successLabel != null) {
            successLabel.textProperty().bind(viewModel.successProperty());
            successLabel.visibleProperty().bind(viewModel.successProperty().isNotEmpty());
            successLabel.managedProperty().bind(successLabel.visibleProperty());
            
            // Cache l'erreur si succès, et vice-versa
            errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty().and(viewModel.successProperty().isEmpty()));
            successLabel.visibleProperty().bind(viewModel.successProperty().isNotEmpty().and(viewModel.errorProperty().isEmpty()));
        }
        
        btnRefresh.disableProperty().bind(viewModel.loadingProperty());
    }
    
    /**
     * Ouvre un dialogue pour demander la date de retour.
     */
    private void promptForReturnDate(LocalDate minDate, Consumer<LocalDate> onConfirm) {
        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Confirmer le retour");
        dlg.setHeaderText("Veuillez sélectionner la date de retour effective.");
        addTheme(dlg.getDialogPane());

        VBox content = new VBox(10);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        
        // Empêche de sélectionner une date avant l'emprunt ou dans le futur
        datePicker.setDayCellFactory(d -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(item.isBefore(minDate) || item.isAfter(LocalDate.now()));
            }
        });
        
        content.getChildren().addAll(new Label("Date de retour:"), datePicker);
        content.setPadding(new Insets(10));
        
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleAsPrimary(dlg.getDialogPane(), ButtonType.OK);
        
        dlg.setResultConverter(btnType -> (btnType == ButtonType.OK) ? datePicker.getValue() : null);
        
        Optional<LocalDate> result = dlg.showAndWait();
        result.ifPresent(onConfirm); // Appelle le viewModel si OK
    }

    // --- helpers ---
    private static String toIso(LocalDate d) { return d == null ? "—" : d.toString(); }
    private static String safe(String s) { return (s == null || s.isBlank()) ? "" : s.trim(); }
    
    // --- Helpers CSS (copiés depuis AdherentsController) ---
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