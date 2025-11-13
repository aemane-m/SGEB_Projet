package application.ui.viewmodel;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.service.EmpruntService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ViewModel pour l'écran des Emprunts.
 * Gère les opérations de manière asynchrone, charge les listes
 * pour les ComboBox et gère la logique d'emprunt/retour.
 *
 * CORRIGÉ : Implémentation de getActiveEmpruntsList et returnByDocumentId.
 */
public class EmpruntsViewModel {

    // --- Services et Repositories ---
    private final EmpruntService empruntService;
    private final AdherentRepository adherentRepository;
    private final DocumentRepository documentRepository;

    // --- Listes de Données ---
    /** Liste brute complète (cache local des emprunts chargés) */
    private final ObservableList<Emprunt> sourceEmprunts = FXCollections.observableArrayList();
    /** Liste filtrée, liée à la TableView */
    private final ObservableList<Emprunt> filteredEmprunts = FXCollections.observableArrayList();
    
    // Listes pour les ComboBox du formulaire
    private final ObservableList<Adherent> adherents = FXCollections.observableArrayList();
    private final ObservableList<Document> availableDocuments = FXCollections.observableArrayList();

    // --- État UI ---
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty  error = new SimpleStringProperty("");
    private final StringProperty  success = new SimpleStringProperty(""); // Pour les messages de succès
    private final ObjectProperty<Emprunt> selected = new SimpleObjectProperty<>(null);

    // --- Filtres ---
    private final BooleanProperty actifsOnly = new SimpleBooleanProperty(true);
    private final BooleanProperty overdueOnly = new SimpleBooleanProperty(false);

    /**
     * Constructeur principal pour l'injection de dépendances.
     */
    public EmpruntsViewModel(EmpruntService empruntService, 
                             AdherentRepository adherentRepository, 
                             DocumentRepository documentRepository) {
        this.empruntService = Objects.requireNonNull(empruntService, "empruntService");
        this.adherentRepository = Objects.requireNonNull(adherentRepository, "adherentRepository");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository");
        
        // Rafraîchir la liste principale quand les filtres changent
        actifsOnly.addListener((o, ov, nv) -> refresh());
        overdueOnly.addListener((o, ov, nv) -> applyFilters());
        
        // Charge les données initiales
        loadInitialData();
    }
    
    /** Constructeur Fallback (pour SceneBuilder ou tests sans services) */
    public EmpruntsViewModel() {
        this.empruntService = null;
        this.adherentRepository = null;
        this.documentRepository = null;
        setSource(List.of());
        applyFilters();
    }


    /** Charge les listes de base (Emprunts, Adhérents, Documents). */
    public void loadInitialData() {
        runSafely(() -> {
            if (empruntService == null || adherentRepository == null || documentRepository == null) {
                Platform.runLater(() -> {
                    setSource(List.of());
                    adherents.clear();
                    availableDocuments.clear();
                    applyFilters();
                });
                return;
            }
            
            // Charge les listes pour les ComboBox
            List<Adherent> adhList = adherentRepository.findAll();
            // Charge uniquement les documents *non empruntés* pour le formulaire
            List<Document> docList = documentRepository.findAll().stream()
                    .filter(doc -> doc.getId() != null && !empruntService.isDocumentEmprunte(doc.getId()))
                    .collect(Collectors.toList());
            
            // Charge la liste principale des emprunts
            List<Emprunt> empList = isActifsOnly()
                ? empruntService.findActive()
                : empruntService.findAll();
            
            // Met à jour l'UI sur le thread FX
            Platform.runLater(() -> {
                adherents.setAll(adhList != null ? adhList : List.of());
                availableDocuments.setAll(docList != null ? docList : List.of());
                setSource(empList != null ? empList : List.of());
                applyFilters();
                setSuccess(""); // Efface l'ancien message de succès
            });
        });
    }

    /** Rafraîchit uniquement la liste des emprunts (et des documents dispo). */
    public void refresh() {
        runSafely(() -> {
            if (empruntService == null) {
                 Platform.runLater(() -> {
                    setSource(List.of());
                    applyFilters();
                 });
                return;
            }
            
            List<Emprunt> list = isActifsOnly()
                ? empruntService.findActive()
                : empruntService.findAll();
            
            // Rafraîchit aussi les documents disponibles
            List<Document> docList = documentRepository.findAll().stream()
                    .filter(doc -> doc.getId() != null && !empruntService.isDocumentEmprunte(doc.getId()))
                    .collect(Collectors.toList());
            
            Platform.runLater(() -> {
                setSource(list != null ? list : List.of());
                availableDocuments.setAll(docList != null ? docList : List.of()); // Met à jour ComboBox
                applyFilters();
                setSuccess("");
            });
        });
    }

    /** Applique les filtres (retards) sur la liste 'source'. */
    private void applyFilters() {
        List<Emprunt> filtered = sourceEmprunts.stream()
                .filter(e -> {
                    // S'assure que l'objet n'est pas null (évite NullPointerException)
                    if (e == null) return false;
                    if (overdueOnly.get()) {
                        return e.estEnRetard(LocalDate.now());
                    }
                    return true;
                })
                .collect(Collectors.toList());
        setEmprunts(filtered);
    }

    /** Tente de créer un nouvel emprunt. */
    public void borrow(long adherentId, long documentId, LocalDate dateEmprunt) {
        runSafely(() -> {
            if (empruntService == null) throw new IllegalStateException("Service indisponible.");
            LocalDate d = (dateEmprunt != null) ? dateEmprunt : LocalDate.now();
            
            Emprunt created = empruntService.borrow(adherentId, documentId, d);
            
            Platform.runLater(() -> {
                if (created != null && created.getId() != null) {
                    setSuccess("Emprunt enregistré (ID: " + created.getId() + ")");
                    refresh(); // Recharge la liste
                } else {
                    // Normalement géré par l'exception du service, mais sécurité
                    setError("Échec de la création de l'emprunt.");
                }
            });
        });
    }

    /** Tente de retourner l'emprunt sélectionné (avec la date du jour). */
    public void returnSelected() {
        Emprunt e = selected.get();
        if (e != null && e.getId() != null) {
            // Appelle la surcharge qui prend aussi la date
            returnEmprunt(e.getId(), LocalDate.now());
        } else {
            setError("Aucun emprunt sélectionné.");
        }
    }

    /** Logique de retour d'emprunt (appelée par le Controller). */
    public void returnEmprunt(long empruntId, LocalDate dateRetour) {
        if (dateRetour == null) {
            setError("Veuillez sélectionner une date de retour.");
            return;
        }
        runSafely(() -> {
            if (empruntService == null) throw new IllegalStateException("Service indisponible.");
            
            Penalite penalite = empruntService.returnEmprunt(empruntId, dateRetour);
            
            Platform.runLater(() -> {
                if (penalite != null) {
                    setSuccess("Retour enregistré. Pénalité générée : " + penalite.getMontant());
                } else {
                    setSuccess("Retour enregistré. Pas de pénalité.");
                }
                refresh();
            });
        });
    }
   
    /**
     * NOUVEAU : Tente de retourner un emprunt basé sur l'ID du DOCUMENT.
     */
    public void returnByDocumentId(long documentId, LocalDate dateRetour) {
        if (dateRetour == null) {
            setError("Veuillez sélectionner une date de retour.");
            return;
        }
        
        runSafely(() -> {
            if (empruntService == null) throw new IllegalStateException("Service indisponible.");

            // 1. Trouver l'emprunt ACTIF pour ce document
            //    On utilise la liste 'source' qui contient les emprunts actifs
            Optional<Emprunt> empruntOpt = sourceEmprunts.stream()
                .filter(e -> e.getDocument() != null && e.getDocument().getId() != null && e.getDocument().getId().equals(documentId))
                .findFirst();

            if (empruntOpt.isEmpty()) {
                // Tente de rafraîchir la source au cas où
                List<Emprunt> list = empruntService.findActive();
                Platform.runLater(() -> sourceEmprunts.setAll(list));
                
                empruntOpt = list.stream()
                   .filter(e -> e.getDocument() != null && e.getDocument().getId() != null && e.getDocument().getId().equals(documentId))
                   .findFirst();
                   
                if (empruntOpt.isEmpty()) {
                     throw new RuntimeException("Aucun emprunt actif trouvé pour le document ID: " + documentId);
                }
            }

            // 2. Appeler la logique de retour normale avec l'ID de l'emprunt trouvé
            Emprunt empruntARetourner = empruntOpt.get();
            Penalite penalite = empruntService.returnEmprunt(empruntARetourner.getId(), dateRetour);
            
            // 3. Mettre à jour l'UI
            Platform.runLater(() -> {
                if (penalite != null) {
                    setSuccess("Retour (Doc ID: " + documentId + ") enregistré. Pénalité : " + penalite.getMontant());
                } else {
                    setSuccess("Retour (Doc ID: " + documentId + ") enregistré. Pas de pénalité.");
                }
                refresh();
            });
        });
    }
    
    
    // =======================================================
    // ===== Utilitaires (Async) =============================
    // =======================================================

    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        setSuccess("");
        
        CompletableFuture.runAsync(() -> {
            if (empruntService == null || adherentRepository == null || documentRepository == null) {
                Platform.runLater(() -> setError("Service non disponible (Mode Fallback)."));
                return; 
            }
            op.run();
        })
            .whenComplete((res, ex) -> {
                Platform.runLater(() -> {
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                    }
                    setLoading(false);
                });
            });
    }

    // --- Helpers thread-safe pour màj UI ---
    private void setLoading(boolean v) { Platform.runLater(() -> loading.set(v)); }
    private void setError(String msg) { Platform.runLater(() -> error.set(msg == null ? "" : msg)); }
    private void setSuccess(String msg) { Platform.runLater(() -> success.set(msg == null ? "" : msg)); }
    private void setSource(List<Emprunt> items) {
        List<Emprunt> safeList = items != null ? items : List.of();
        if (Platform.isFxApplicationThread()) {
            sourceEmprunts.setAll(safeList);
        } else {
            Platform.runLater(() -> sourceEmprunts.setAll(safeList));
        }
    }
    private void setEmprunts(List<Emprunt> items) {
        List<Emprunt> safeList = items != null ? items : List.of();
        if (Platform.isFxApplicationThread()) {
            filteredEmprunts.setAll(safeList);
        } else {
            Platform.runLater(() -> filteredEmprunts.setAll(safeList));
        }
    }

    
    // ===== Bindings exposés =====

    /** Liste filtrée pour la TableView */
    public ObservableList<Emprunt> getEmprunts() { return filteredEmprunts; }
    
    /** CORRIGÉ : Expose la liste source, qui contient les emprunts actifs pour le ComboBox */
    public ObservableList<Emprunt> getActiveEmpruntsList() {
        return sourceEmprunts;
    }
    
    public ObservableList<Adherent> getAdherents() { return adherents; }
    
    /** CORRIGÉ : Renommé pour correspondre au Controller */
    public ObservableList<Document> getAvailableDocuments() { return availableDocuments; }

    public ReadOnlyBooleanProperty loadingProperty() { return loading; }
    public ReadOnlyStringProperty  errorProperty() { return error; }
    public ReadOnlyStringProperty  successProperty() { return success; } 

    public ObjectProperty<Emprunt> selectedProperty() { return selected; }
    public Emprunt getSelected() { return selected.get(); }
    public void setSelected(Emprunt e) { selected.set(e); }

    public BooleanProperty actifsOnlyProperty() { return actifsOnly; }
    public boolean isActifsOnly() { return actifsOnly.get(); }
    public void setActifsOnly(boolean v) { actifsOnly.set(v); }

    public BooleanProperty overdueOnlyProperty() { return overdueOnly; }
    public boolean isOverdueOnly() { return overdueOnly.get(); }
    public void setOverdueOnly(boolean v) { overdueOnly.set(v); }

    // Ajout d'un helper pour le contrôleur
    public void reportError(String message) {
        setError(message);
    }
}