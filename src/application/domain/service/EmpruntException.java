package application.domain.service;

/**
 * Exception spécifique pour les échecs de logique métier liés aux emprunts.
 * (Ex: Adhérent bloqué, document non disponible, etc.)
 *
 * CORRIGÉ : Ajout du serialVersionUID pour la sérialisation.
 */
public class EmpruntException extends RuntimeException {

    /**
     * Identifiant de version pour la sérialisation.
     */
    private static final long serialVersionUID = 1L;

    public EmpruntException(String message) {
        super(message);
    }
}