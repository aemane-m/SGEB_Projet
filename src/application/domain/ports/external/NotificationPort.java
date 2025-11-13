package application.domain.ports.external;

/**
 * Port pour l'envoi de notifications (email, messages internes...).
 */
public interface NotificationPort {

    /**
     * Envoie une notification à un adhérent.
     * @param destinataire Adresse email ou identifiant de l'adhérent
     * @param sujet Sujet du message
     * @param contenu Corps du message
     */
    void sendNotification(String destinataire, String sujet, String contenu);
}
