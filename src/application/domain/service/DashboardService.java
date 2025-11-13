package application.domain.service;

/**
 * Service métier pour le tableau de bord administrateur.
 * Agrège les données provenant des différents repositories.
 */
public interface DashboardService {
    int countDocuments();
    int countAdherents();
    int countEmprunts();
    double sumPenalites();
 // MÉTHODE À AJOUTER
    int countRetards();
}
