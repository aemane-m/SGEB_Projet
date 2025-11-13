module SGEB_Projet {
    // --- Dépendances JavaFX ---
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.base;
    requires transitive javafx.web;
    // CORRIGÉ : Ajouté pour SwingFXUtils
    requires transitive javafx.swing; 
    

    // --- Dépendances standard ---
    requires java.sql;
    requires java.net.http;
    requires com.google.gson;
    requires java.logging;
    // CORRIGÉ : Ajouté pour ImageIO et BufferedImage
    requires java.desktop; 

    // --- Dépendances PDF ---
    requires org.apache.pdfbox;
    requires org.apache.pdfbox.io;
    requires org.apache.fontbox;
    requires org.apache.commons.logging;

    // === OUVERTURE DES PACKAGES (pour réflexion / FXML / Gson) ===
    opens application.ui.viewmodel.controller to javafx.fxml;
    opens application.ui.viewmodel to javafx.fxml;
    opens application.domain.core.document;
    opens application.domain.core.personne;
    opens application.domain.core.emprunt;
    opens application.infrastructure.sqlite;
    opens application.infrastructure.external;
    opens application to javafx.fxml;

    // === EXPORTS ===
    exports application;
    
}