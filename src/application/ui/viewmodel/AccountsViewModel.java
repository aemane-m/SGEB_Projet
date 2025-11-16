package application.ui.viewmodel;

import application.domain.service.AccountService;
import application.domain.service.AccountService.UserAccountSummary;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * ViewModel pour la gestion des comptes applicatifs (ADMIN + ADHERENT).
 *
 * - Liste tous les comptes via AccountService.listAllAccounts()
 * - Permet de changer le statut (ACTIVE <-> SUSPENDED)
 * - Permet de supprimer un compte (avec règles métier gérées côté domaine)
 */
public final class AccountsViewModel {

    private final AccountService accountService;
    private final long actingUserId; // id de l'utilisateur connecté (admin actuel)

    // --- État UI ---
    private final ObservableList<UserAccountSummary> accounts =
            FXCollections.observableArrayList();

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");
    private final ObjectProperty<UserAccountSummary> selected =
            new SimpleObjectProperty<>(null);

    public AccountsViewModel(AccountService accountService, long actingUserId) {
        this.accountService = Objects.requireNonNull(accountService, "accountService");
        this.actingUserId = actingUserId;

        refresh();
    }

    // ===========================================================
    // ===================== Actions =============================
    // ===========================================================

    /** Recharge la liste complète des comptes. */
    public void refresh() {
        runSafely(() -> {
            List<UserAccountSummary> list = accountService.listAllAccounts();
            Platform.runLater(() -> {
                accounts.setAll(list);

                // Si le compte sélectionné a disparu, on reset
                UserAccountSummary sel = selected.get();
                if (sel != null &&
                        list.stream().noneMatch(a -> a.userId() == sel.userId())) {
                    selected.set(null);
                }
            });
        });
    }

    /** Change le statut du compte sélectionné (ACTIVE <-> SUSPENDED). */
    public void toggleStatusForSelected() {
        UserAccountSummary sel = selected.get();
        if (sel == null) {
            setError("Aucun compte sélectionné.");
            return;
        }

        final long targetUserId = sel.userId();
        final String currentStatus = (sel.status() == null) ? "" : sel.status().trim().toUpperCase();
        final String newStatus;

        if ("ACTIVE".equals(currentStatus)) {
            newStatus = "SUSPENDED";
        } else {
            newStatus = "ACTIVE";
        }

        runSafely(() -> {
            accountService.changeAccountStatus(targetUserId, newStatus, actingUserId);
            // On recharge la liste après modification
            List<UserAccountSummary> list = accountService.listAllAccounts();
            Platform.runLater(() -> {
                accounts.setAll(list);
                // Réappliquer la sélection sur le même id si encore présent
                list.stream()
                        .filter(a -> a.userId() == targetUserId)
                        .findFirst()
                        .ifPresentOrElse(
                                selected::set,
                                () -> selected.set(null)
                        );
            });
        }, err -> setError("Erreur lors du changement de statut : " + getRootCauseMessage(err)));
    }

    /** Supprime le compte sélectionné. */
    public void deleteSelected() {
        UserAccountSummary sel = selected.get();
        if (sel == null) {
            setError("Aucun compte sélectionné.");
            return;
        }

        final long targetUserId = sel.userId();

        runSafely(() -> {
            accountService.deleteAccount(targetUserId, actingUserId);
            // On recharge la liste après suppression
            List<UserAccountSummary> list = accountService.listAllAccounts();
            Platform.runLater(() -> {
                accounts.setAll(list);
                selected.set(null);
            });
        }, err -> setError("Erreur lors de la suppression : " + getRootCauseMessage(err)));
    }

    // ===========================================================
    // =============== Helpers Threading / Erreurs ===============
    // ===========================================================

    private void runSafely(Runnable operation) {
        runSafely(operation, defaultErrorHandler());
    }

    private void runSafely(Runnable operation, Consumer<Throwable> errorHandler) {
        setLoading(true);
        setError("");

        CompletableFuture.runAsync(operation)
                .whenComplete((result, exception) -> {
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
        String safe = (message == null) ? "" : message;
        if (Platform.isFxApplicationThread()) {
            error.set(safe);
        } else {
            Platform.runLater(() -> error.set(safe));
        }
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
    // =============== Getters pour la UI ========================
    // ===========================================================

    public ObservableList<UserAccountSummary> getAccounts() {
        return accounts;
    }

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    public ObjectProperty<UserAccountSummary> selectedProperty() {
        return selected;
    }

    public UserAccountSummary getSelected() {
        return selected.get();
    }

    public void setSelected(UserAccountSummary summary) {
        selected.set(summary);
    }
}