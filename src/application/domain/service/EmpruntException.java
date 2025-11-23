package application.domain.service;

/**
 * Exception métier lancée lorsqu'une règle de gestion liée aux emprunts
 * n'est pas respectée :
 * <ul>
 *     <li>document déjà emprunté,</li>
 *     <li>adhérent bloqué ou avec pénalités,</li>
 *     <li>emprunt inexistant,</li>
 *     <li>ou retour déjà effectué.</li>
 * </ul>
 *
 * <p>Cette exception est non contrôlée (RuntimeException)
 * car elle représente un échec métier, non une erreur technique.</p>
 */
public class EmpruntException extends RuntimeException {

    /** Identifiant de version pour la sérialisation. */
    private static final long serialVersionUID = 1L;

    public EmpruntException(String message) {
        super(message);
    }
}