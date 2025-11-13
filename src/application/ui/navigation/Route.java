package application.ui.navigation;

public enum Route {
    DASHBOARD("/application/ui/view/DashboardView.fxml"),
    CATALOGUE("/application/ui/view/CatalogView.fxml"),
    ADHERENTS("/application/ui/view/AdherentsView.fxml"),
    EMPRUNTS("/application/ui/view/LoansView.fxml");

    private final String fxml;
    Route(String fxml) { this.fxml = fxml; }
    public String fxml() { return fxml; }
}

