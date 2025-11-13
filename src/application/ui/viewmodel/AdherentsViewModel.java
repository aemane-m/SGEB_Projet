package application.ui.viewmodel;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.domain.core.personne.Adherent;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Gère le CRUD Adhérents, l'historique (via EmpruntRepository)
 * et le statut (via PenaliteRepository).
 * Utilise AdherentStatusWrapper pour charger les statuts en 1 seule requête.
 */
public class AdherentsViewModel {

    // --- Wrapper pour la TableView ---
    public static class AdherentStatusWrapper {
        private final Adherent adherent;
        private final StringProperty statutProperty = new SimpleStringProperty(); 
        
        public AdherentStatusWrapper(Adherent adherent, String statut) {
            this.adherent = adherent;
            this.statutProperty.set(statut);
        }
        public Adherent getAdherent() { return adherent; }
        public StringProperty statutProperty() { return statutProperty; }
        public String getStatut() { return statutProperty.get(); }
    }

    // --- Repositories (Services) ---
    private final AdherentRepository adherentRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteRepository penaliteRepository;

    // --- État UI ---
    private final ObservableList<AdherentStatusWrapper> items = FXCollections.observableArrayList();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");
    private final ObjectProperty<AdherentStatusWrapper> selected = new SimpleObjectProperty<>(null);
    private final ObservableList<Emprunt> selectedAdherentHistorique = FXCollections.observableArrayList();

    // --- État Formulaire ---
    private final LongProperty formId = new SimpleLongProperty(0);
    private final StringProperty formNom = new SimpleStringProperty("");
    private final StringProperty formPrenom = new SimpleStringProperty("");
    private final StringProperty formEmail = new SimpleStringProperty("");
    private final StringProperty formTelephone = new SimpleStringProperty("");

    /**
     * Constructeur principal.
     */
    public AdherentsViewModel(AdherentRepository adhRepo, EmpruntRepository empRepo, PenaliteRepository penRepo) {
        this.adherentRepository = Objects.requireNonNull(adhRepo, "adhRepo");
        this.empruntRepository = Objects.requireNonNull(empRepo, "empRepo");
        this.penaliteRepository = Objects.requireNonNull(penRepo, "penRepo");
        
        selected.addListener((obs, oldVal, newVal) -> loadFormFrom(newVal == null ? null : newVal.getAdherent()));
        refresh(); // Charge la liste initiale
    }

    /**
     * Recharge la liste + les statuts en 2 requêtes.
     */
    public void refresh() {
        runSafely(() -> {
            // Requête 1: Adhérents
            var list = adherentRepository.findAll();
            // Requête 2: Comptes de pénalités
            Map<Long, Integer> penaltyCounts = penaliteRepository.findActivePenaltyCountsMap();
            
            // Combine en mémoire
            List<AdherentStatusWrapper> wrappers = list.stream()
                .map(adherent -> {
                    int count = penaltyCounts.getOrDefault(adherent.getId(), 0);
                    String statut = (count > 0) ? "⚠️ Pénalités (" + count + ")" : "✔️ OK";
                    return new AdherentStatusWrapper(adherent, statut);
                })
                .collect(Collectors.toList());
            
            // Mise à jour UI
            Platform.runLater(() -> {
                items.setAll(wrappers);
                var sel = selected.get();
                if (sel != null && sel.getAdherent().getId() != null &&
                    wrappers.stream().noneMatch(x -> sel.getAdherent().getId().equals(x.getAdherent().getId()))) {
                    selected.set(null);
                }
            });
        });
    }

    // ===========================================================
    // =============== CRUD (Formulaire) =========================
    // ===========================================================

    public void createNew() {
        selected.set(null);
        clearForm();
        setError("");
    }
    
    public void edit(Adherent a) {
        // Si aucun adhérent n'est passé en paramètre, on reprend la sélection actuelle
        AdherentStatusWrapper wrapper = (a == null)
            ? selected.get()
            : items.stream()
                   .filter(w -> w.getAdherent().getId() != null 
                             && w.getAdherent().getId().equals(a.getId()))
                   .findFirst()
                   .orElse(null);

        // Charge les données de l'adhérent dans le formulaire
        loadFormFrom(a);

        // Met à jour la sélection si un wrapper correspondant a été trouvé
        if (wrapper != null) {
            selected.set(wrapper);
        }

        // Réinitialise les erreurs
        setError("");
    }


  

    public void saveCurrent() {
        if (!isFormValid()) {
            setError("Formulaire invalide : nom, prénom et email (valide) requis.");
            return;
        }
        Adherent model = toModelFromForm();
        runSafely(() -> {
            Optional<Adherent> existingByEmail = adherentRepository.findByEmail(model.getEmail());
            if (existingByEmail.isPresent() && (model.getId() == null || !existingByEmail.get().getId().equals(model.getId()))) {
                throw new RuntimeException("Cet email est déjà utilisé par un autre adhérent.");
            }
            Adherent saved = adherentRepository.save(model);
            
            Platform.runLater(() -> {
                clearForm();
                refresh(); // Recharge tout
                items.stream()
                     .filter(w -> w.getAdherent().getId().equals(saved.getId()))
                     .findFirst()
                     .ifPresent(selected::set);
            });
        }, error -> setError("Erreur de sauvegarde: " + getRootCauseMessage(error)));
    }
    

    /**
     * Supprime l'adhérent actuellement sélectionné, s'il y en a un.
     */
    public void deleteSelected() {
        if (selected.get() != null) {
            delete(selected.get().getAdherent());
        } else {
            setError("Aucun adhérent sélectionné.");
        }
    }

    /**
     * Supprime un adhérent donné, après vérification qu'il n'a pas d'emprunts en cours.
     */
    public void delete(Adherent a) {
        if (a == null || a.getId() == null) {
            setError("Aucun adhérent sélectionné.");
            return;
        }

        final Long idToDelete = a.getId();

        runSafely(() -> {
            // Vérifie si l'adhérent a encore des emprunts non retournés
            boolean hasActiveLoans = empruntRepository.findByAdherentId(idToDelete).stream()
                .anyMatch(emprunt -> emprunt.getDateRetourEffective() == null);

            if (hasActiveLoans) {
                throw new RuntimeException("Impossible de supprimer : cet adhérent a des emprunts en cours.");
            }

            // Supprime l'adhérent de la base
            adherentRepository.delete(idToDelete);

            // Met à jour la liste et la sélection dans l'interface
            Platform.runLater(() -> {
                if (selected.get() != null 
                        && idToDelete.equals(selected.get().getAdherent().getId())) {
                    selected.set(null);
                }
                items.removeIf(w -> idToDelete.equals(w.getAdherent().getId()));
            });

        }, error -> setError("Erreur de suppression : " + getRootCauseMessage(error)));
    }


    // ===========================================================
    // =============== RECHERCHE / STATUT ========================
    // ===========================================================

    /**
     * CORRIGÉ : La variable searchResult est maintenant "effectively final".
     */
    /**
     * CORRIGÉ : La variable searchResult est déclarée non-finale,
     * puis copiée dans une variable finale pour la lambda.
     */
    public void searchByIdOrName(String query) {
        if (query == null || query.isBlank()) {
            refresh(); return;
        }
        runSafely(() -> {
            String q = query.trim();
            List<Adherent> searchResult;
            try {
                long id = Long.parseLong(q);
                searchResult = adherentRepository.findById(id).map(List::of).orElse(List.of());
            } catch (NumberFormatException nfe) {
                searchResult = adherentRepository.findByNom(q);
            }
            
            Map<Long, Integer> penaltyCounts = penaliteRepository.findActivePenaltyCountsMap();
            
            List<AdherentStatusWrapper> wrappers = searchResult.stream()
                .map(adherent -> {
                    int count = penaltyCounts.getOrDefault(adherent.getId(), 0);
                    String statut = (count > 0) ? "⚠️ Pénalités (" + count + ")" : "✔️ OK";
                    return new AdherentStatusWrapper(adherent, statut);
                })
                .collect(Collectors.toList());
            
            Platform.runLater(() -> {
                items.setAll(wrappers);
                setError(wrappers.isEmpty() ? "Aucun adhérent trouvé." : "");
            });
        });
    }


    public CompletableFuture<String> fetchAdherentStatut(Adherent a) {
        if (a == null || a.getId() == null) {
            return CompletableFuture.completedFuture("Erreur");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Penalite> penalites = penaliteRepository.findActivesByAdherentId(a.getId());
                if (penalites.isEmpty()) {
                    return "✔️ OK";
                } else {
                    return "⚠️ Pénalités (" + penalites.size() + ")";
                }
            } catch (Exception e) {
                return "Erreur Statut";
            }
        });
    }
    
	 // ===========================================================
	 // =============== HISTORIQUE EMPRUNTS =======================
	 // ===========================================================
	
	 /**
	  * Charge l'historique des emprunts pour un adhérent donné
	  * (ou pour l'adhérent actuellement sélectionné si a == null).
	  */
	 public void loadHistoriqueFor(Adherent a) {
	     Adherent target = (a != null)
	         ? a
	         : (selected.get() != null ? selected.get().getAdherent() : null);
	
	     if (target == null || target.getId() == null) {
	         setError("Aucun adhérent sélectionné pour voir l'historique.");
	         selectedAdherentHistorique.clear();
	         return;
	     }
	
	     runSafely(() -> {
	         List<Emprunt> historique = empruntRepository.findByAdherentId(target.getId());
	         Platform.runLater(() -> {
	             selectedAdherentHistorique.setAll(historique);
	             if (historique.isEmpty()) {
	                 setError("Cet adhérent n'a aucun historique d'emprunt.");
	             }
	         });
	     });
	 }
	
	 public ObservableList<Emprunt> getSelectedAdherentHistorique() {
	     return selectedAdherentHistorique;
	 }
	
	 // ===========================================================
	 // =============== Helpers (Formulaire, Threading) ===========
	 // ===========================================================
	
	 /** Réinitialise tous les champs du formulaire. */
	 private void clearForm() {
	     formId.set(0);
	     formNom.set("");
	     formPrenom.set("");
	     formEmail.set("");
	     formTelephone.set("");
	 }
	
	 /** Validation minimale du formulaire. */
	 private boolean isFormValid() {
	     return !isBlank(formNom.get())
	         && !isBlank(formPrenom.get())
	         && !isBlank(formEmail.get())
	         && formEmail.get().contains("@");
	 }
	
	 /** Charge les champs du formulaire depuis un modèle d'adhérent. */
	 private void loadFormFrom(Adherent a) {
	     if (a == null) {
	         clearForm();
	         return;
	     }
	     formId.set(a.getId() == null ? 0 : a.getId());
	     formNom.set(nz(a.getNom()));
	     formPrenom.set(nz(a.getPrenom()));
	     formEmail.set(nz(a.getEmail()));
	     formTelephone.set(nz(a.getTelephone()));
	 }
	
	 /** Construit un modèle Adherent à partir des champs du formulaire. */
	 private Adherent toModelFromForm() {
	     Adherent a = new Adherent();
	     if (formId.get() > 0) {
	         a.setId(formId.get());
	     }
	     a.setNom(trimToNull(formNom.get()));
	     a.setPrenom(trimToNull(formPrenom.get()));
	     a.setEmail(trimToNull(formEmail.get()));
	     a.setTelephone(trimToNull(formTelephone.get()));
	     return a;
	 }
	
	 private static boolean isBlank(String s) {
	     return s == null || s.isBlank();
	 }
	
	 private static String nz(String s) {
	     return (s == null) ? "" : s;
	 }
	
	 private static String trimToNull(String s) {
	     if (s == null) return null;
	     String t = s.trim();
	     return t.isEmpty() ? null : t;
	 }
	
	 /** Enveloppe d'exécution asynchrone avec gestion d'état et d'erreur. */
	 private void runSafely(Runnable operation) {
	     runSafely(operation, defaultErrorHandler());
	 }
	
	 private void runSafely(Runnable operation, Consumer<Throwable> errorHandler) {
	     setLoading(true);
	     setError("");
	
	     CompletableFuture.runAsync(() -> {
	         if (adherentRepository == null || empruntRepository == null || penaliteRepository == null) {
	             throw new IllegalStateException("Un ou plusieurs repositories ne sont pas disponibles.");
	         }
	         operation.run();
	     }).whenComplete((result, exception) -> {
	         Platform.runLater(() -> {
	             if (exception != null) {
	                 errorHandler.accept(exception);
	             }
	             setLoading(false);
	         });
	     });
	 }
	
	 private Consumer<Throwable> defaultErrorHandler() {
	     return error -> setError(getRootCauseMessage(error));
	 }
	
	 private void setLoading(boolean value) {
	     if (Platform.isFxApplicationThread()) {
	         loading.set(value);
	     } else {
	         Platform.runLater(() -> loading.set(value));
	     }
	 }
	
	 private void setError(String message) {
	     String safeMessage = (message == null) ? "" : message;
	     if (Platform.isFxApplicationThread()) {
	         error.set(safeMessage);
	     } else {
	         Platform.runLater(() -> error.set(safeMessage));
	     }
	 }
	
	 public void reportError(String message) {
	     setError(message);
	 }
	
	 private static String getRootCauseMessage(Throwable throwable) {
	     Objects.requireNonNull(throwable);
	     Throwable root = throwable;
	     while (root.getCause() != null && root.getCause() != root) {
	         root = root.getCause();
	     }
	     String msg = root.getMessage();
	     return (msg != null && !msg.isBlank()) ? msg : root.toString();
	 }
	
	 // ===========================================================
	 // =============== Exposition (UI Bind) ======================
	 // ===========================================================
	
	 public ObservableList<AdherentStatusWrapper> getItems() { return items; }
	 public ReadOnlyBooleanProperty loadingProperty() { return loading; }
	 public ReadOnlyStringProperty errorProperty() { return error; }
	 public ObjectProperty<AdherentStatusWrapper> selectedProperty() { return selected; }
	
	 public AdherentStatusWrapper getSelected() { return selected.get(); }
	 public void setSelected(AdherentStatusWrapper a) { selected.set(a); }
	
	 public LongProperty formIdProperty() { return formId; }
	 public StringProperty formNomProperty() { return formNom; }
	 public StringProperty formPrenomProperty() { return formPrenom; }
	 public StringProperty formEmailProperty() { return formEmail; }
	 public StringProperty formTelephoneProperty() { return formTelephone; }
	
	 public Adherent getSelectedAdherent() {
	     return (selected.get() == null) ? null : selected.get().getAdherent();
	 }
}