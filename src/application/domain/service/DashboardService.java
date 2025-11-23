package application.domain.service;

/**
 * Service métier fournissant les données affichées dans le
 * tableau de bord administrateur : nombre de documents,
 * d'adhérents, d'emprunts, et informations sur les pénalités.
 *
 * <p>Ce service interroge les différents dépôts (repositories)
 * pour agréger des valeurs simples utiles à l’administration.</p>
 */
public interface DashboardService {

    /**
     * Retourne le nombre total de documents enregistrés
     * dans le catalogue (livres, magazines, CD...).
     *
     * @return nombre de documents
     */
    int countDocuments();

    /**
     * Retourne le nombre total d’adhérents inscrits
     * dans la bibliothèque.
     *
     * @return nombre d’adhérents
     */
    int countAdherents();

    /**
     * Retourne le nombre d’emprunts actuellement en cours
     * (documents empruntés mais non encore retournés).
     *
     * @return nombre d’emprunts actifs
     */
    int countEmprunts();

    /**
     * Retourne la somme totale des pénalités non payées.
     * Le montant est exprimé en euros.
     *
     * @return total des pénalités en cours
     */
    double sumPenalites();

    /**
     * Retourne le nombre d’emprunts actuellement en retard
     * à la date du jour.
     *
     * @return nombre d’emprunts en retard
     */
    int countRetards();
}