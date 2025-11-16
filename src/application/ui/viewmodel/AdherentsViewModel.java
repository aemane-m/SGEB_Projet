package application.ui.viewmodel;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.domain.core.personne.Adherent;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import application.domain.service.AccountService;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ViewModel de gestion des adhérents (côté admin).
 *
 * - Liste + statut (via PenaliteRepository)
 * - CRUD adhérents (fiche personne)
 * - Lors de la création :
 *   -> création d'un compte réel via AccountService (ADMIN ou ADHERENT)
 *   -> mot de passe temporaire imposé
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

        public Adherent getAdherent() {
            return adherent;
        }

        public StringProperty statutProperty() {
            return statutProperty;
        }

        public String getStatut() {
            return statutProperty.get();
        }
    }

    // --- Validation téléphone : format FR (0X..., +33..., 0033...) ---
    private static final String PHONE_HINT =
            "Ex : 06 12 34 56 78 ou +33 6 12 34 56 78";

    // Pattern : autorise chiffres, espaces, points, tirets, mais contrôle le nombre de chiffres
    private static boolean isTelephoneValid(String s) {
        if (s == null || s.isBlank()) return true; // téléphone optionnel
        String digits = s.replaceAll("\\D", "");    // ne garde que les chiffres

        // 0XXXXXXXXX (10 chiffres, commence par 0)
        if (digits.startsWith("0") && digits.length() == 10) {
            return true;
        }
        // 33XXXXXXXXX (33 + 9 chiffres = 11)
        if (digits.startsWith("33") && digits.length() == 11) {
            return true;
        }
        // 0033XXXXXXXXX (0033 + 9 chiffres = 13)
        if (digits.startsWith("0033") && digits.length() == 13) {
            return true;
        }
        return false;
    }

    // --- Repositories (Services) ---
    private final AdherentRepository adherentRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteRepository penaliteRepository;
    private final AccountService accountService;

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

    // Nouveaux champs pour la création de compte
    private final StringProperty formRole = new SimpleStringProperty("");       // "ADMIN" ou "ADHERENT"
    private final StringProperty formPassword = new SimpleStringProperty("");   // mot de passe temporaire

    /**
     * Constructeur principal.
     */
    public AdherentsViewModel(AdherentRepository adhRepo,
                              EmpruntRepository empRepo,
                              PenaliteRepository penRepo,
                              AccountService accountService) {
        this.adherentRepository = Objects.requireNonNull(adhRepo, "adhRepo");
        this.empruntRepository = Objects.requireNonNull(empRepo, "empRepo");
        this.penaliteRepository = Objects.requireNonNull(penRepo, "penRepo");
        this.accountService = Objects.requireNonNull(accountService, "accountService");

        selected.addListener((obs, oldVal, newVal) ->
                loadFormFrom(newVal == null ? null : newVal.getAdherent())
        );
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

    /** Prépare un nouveau formulaire (création). */
    public void createNew() {
        selected.set(null);
        clearForm();
        setError("");
    }

    /** Prépare l’édition d’un adhérent existant. */
    public void edit(Adherent a) {
        AdherentStatusWrapper wrapper = (a == null)
                ? selected.get()
                : items.stream()
                        .filter(w -> w.getAdherent().getId() != null
                                && w.getAdherent().getId().equals(a.getId()))
                        .findFirst()
                        .orElse(null);

        loadFormFrom(a);

        if (wrapper != null) {
            selected.set(wrapper);
        }

        setError("");
    }

    /**
     * Sauvegarde l’état courant du formulaire.
     * - Si formId == 0 => création : on crée aussi un compte via AccountService.
     * - Sinon => simple mise à jour de la fiche adhérent.
     */
    public void saveCurrent() {
        boolean isNew = (formId.get() == 0);

        if (!isFormValid(isNew)) {
            // isFormValid a déjà renseigné setError()
            return;
        }

        if (isNew) {
            // Création : on délègue la création du compte au domaine
            final String email = trimToNull(formEmail.get());
            final String nom = trimToNull(formNom.get());
            final String prenom = trimToNull(formPrenom.get());
            final String password = formPassword.get();
            final String role = formRole.get();

            runSafely(() -> {
                // Vérifie d'abord côté AdherentRepository s'il n'y a pas déjà quelqu'un avec cet email
                Optional<Adherent> existingByEmail = adherentRepository.findByEmail(email);
                if (existingByEmail.isPresent()) {
                    throw new RuntimeException("Cet email est déjà utilisé par un autre adhérent.");
                }

                // Ensuite, création du compte réél via AccountService
                if ("ADMIN".equalsIgnoreCase(role)) {
                    accountService.createAdmin(email, password, nom, prenom);
                } else {
                    // Par défaut on considère ADHERENT si non ADMIN
                    accountService.createAdherentPublic(email, password, nom, prenom);
                }

                // Le domaine se charge de créer le compte (et l’adhérent si nécessaire).
                Platform.runLater(() -> {
                    clearForm();
                    refresh();
                });
            }, err -> setError("Erreur de création : " + getRootCauseMessage(err)));

        } else {
            // Edition : on ne touche PAS au compte, on met juste à jour la fiche adhérent
            Adherent model = toModelFromForm();
            runSafely(() -> {
                Optional<Adherent> existingByEmail = adherentRepository.findByEmail(model.getEmail());
                if (existingByEmail.isPresent()
                        && (model.getId() == null || !existingByEmail.get().getId().equals(model.getId()))) {
                    throw new RuntimeException("Cet email est déjà utilisé par un autre adhérent.");
                }

                Adherent saved = adherentRepository.save(model);

                Platform.runLater(() -> {
                    clearForm();
                    refresh();
                    items.stream()
                            .filter(w -> w.getAdherent().getId().equals(saved.getId()))
                            .findFirst()
                            .ifPresent(selected::set);
                });
            }, err -> setError("Erreur de mise à jour : " + getRootCauseMessage(err)));
        }
    }

    /**
     * Supprime l'adhérent actuellement sélectionné.
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

            adherentRepository.delete(idToDelete);

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

    public void searchByIdOrName(String query) {
        if (query == null || query.isBlank()) {
            refresh();
            return;
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

    // ===========================================================
    // =============== HISTORIQUE EMPRUNTS =======================
    // ===========================================================

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
        formRole.set("");
        formPassword.set("");
    }

    /** Validation fine du formulaire (avec rôle + mot de passe en création). */
    private boolean isFormValid(boolean isNew) {
        String nom = formNom.get();
        String prenom = formPrenom.get();
        String email = formEmail.get();
        String tel = formTelephone.get();
        String role = formRole.get();
        String password = formPassword.get();

        if (isBlank(nom) || isBlank(prenom) || isBlank(email)) {
            setError("Nom, prénom et email sont obligatoires.");
            return false;
        }
        if (!email.contains("@")) {
            setError("Email invalide. Exemple : utilisateur@domaine.fr");
            return false;
        }

        if (!isTelephoneValid(tel)) {
            setError("Numéro de téléphone invalide. " + PHONE_HINT);
            return false;
        }

        if (isNew) {
            if (isBlank(role)) {
                setError("Veuillez sélectionner un rôle (ADMIN ou ADHERENT).");
                return false;
            }
            if (!"ADMIN".equalsIgnoreCase(role) && !"ADHERENT".equalsIgnoreCase(role)) {
                setError("Rôle invalide. Choisissez ADMIN ou ADHERENT.");
                return false;
            }
            if (isBlank(password) || password.length() < 8) {
                setError("Mot de passe temporaire invalide. Minimum 8 caractères.");
                return false;
            }
        }

        // OK
        return true;
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

        // En édition, on ne propose pas de changer le rôle ni le mot de passe ici
        formRole.set("");
        formPassword.set("");
    }

    /** Construit un modèle Adherent à partir des champs du formulaire (édition). */
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

    // ===========================================================
    // =============== Threading / Erreurs =======================
    // ===========================================================

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

    public ObservableList<AdherentStatusWrapper> getItems() {
        return items;
    }

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    public ObjectProperty<AdherentStatusWrapper> selectedProperty() {
        return selected;
    }

    public AdherentStatusWrapper getSelected() {
        return selected.get();
    }

    public void setSelected(AdherentStatusWrapper a) {
        selected.set(a);
    }

    public LongProperty formIdProperty() { return formId; }
    public StringProperty formNomProperty() { return formNom; }
    public StringProperty formPrenomProperty() { return formPrenom; }
    public StringProperty formEmailProperty() { return formEmail; }
    public StringProperty formTelephoneProperty() { return formTelephone; }
    public StringProperty formRoleProperty() { return formRole; }
    public StringProperty formPasswordProperty() { return formPassword; }

    public Adherent getSelectedAdherent() {
        return (selected.get() == null) ? null : selected.get().getAdherent();
    }

    public static String getTelephoneHint() {
        return PHONE_HINT;
    }
}