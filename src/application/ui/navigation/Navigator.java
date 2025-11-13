package application.ui.navigation;

public interface Navigator {
    
    void showShell();
    
    /**
     * Méthode de navigation principale vers le tableau de bord, 
     * nécessitant un contexte utilisateur.
     */
    void goToDashboard(String username, String role);

    /**
     * (CORRECTION AJOUTÉE)
     * Surcharge pour la navigation simple (ex: depuis le menu principal).
     * Appelle la méthode principale avec des valeurs null par défaut.
     */
    default void goToDashboard() {
        goToDashboard(null, null);
    }

    void goToCatalog();
    
    void goToAdherents();
    
    void goToLoans();
}