package application.ui.viewmodel;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * ViewModel de la page d'accueil (Landing).
 *
 * Purement "state" : textes affichés.
 * La navigation reste gérée par le Controller via Navigator.
 */
public final class LandingViewModel {

    private final ReadOnlyStringWrapper title =
            new ReadOnlyStringWrapper("SGEB – Système de Gestion des Emprunts de Bibliothèque");

    private final ReadOnlyStringWrapper subtitle =
            new ReadOnlyStringWrapper(
                    "Gérez le catalogue, les emprunts, les adhérents et les pénalités dans une application JavaFX embarquée."
            );

    public ReadOnlyStringProperty titleProperty() {
        return title.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty subtitleProperty() {
        return subtitle.getReadOnlyProperty();
    }
}