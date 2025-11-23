package application.ui.viewmodel;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * ViewModel pour la page d'accueil (Landing).
 *
 * <p>
 * Contient uniquement des valeurs textuelles statiques destinées
 * à être affichées dans la vue. Aucun état mutable ni dépendance.
 * </p>
 *
 * <p>
 * La navigation est entièrement gérée par le Controller
 * via l'interface {@code Navigator}.
 * </p>
 */
public final class LandingViewModel {

    // --- Propriétés affichées dans la vue ---
    private final ReadOnlyStringWrapper title =
            new ReadOnlyStringWrapper("SGEB – Système de Gestion des Emprunts de Bibliothèque");

    private final ReadOnlyStringWrapper subtitle =
            new ReadOnlyStringWrapper(
                    "Gérez le catalogue, les emprunts, les adhérents et les pénalités dans une application JavaFX embarquée."
            );

    /** Constructeur explicite (bonne pratique, même vide). */
    public LandingViewModel() {
        // Rien à initialiser : ViewModel statique
    }

    // --- Getters READ-ONLY exposés à la vue ---
    public ReadOnlyStringProperty titleProperty() {
        return title.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty subtitleProperty() {
        return subtitle.getReadOnlyProperty();
    }
}