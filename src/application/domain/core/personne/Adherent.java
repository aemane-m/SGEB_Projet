package application.domain.core.personne;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;
import application.domain.core.emprunt.Penalite;
import application.domain.core.security.Capability;
import application.domain.core.security.Role;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p><strong>Adherent</strong> est une entité du domaine représentant un membre de la bibliothèque.</p>
 *
 * <p>Responsabilités principales :</p>
 * <ul>
 *   <li>Hérite de {@link Personne} (identité : id, nom, prenom, email, téléphone)</li>
 *   <li>Gère ses emprunts actifs (liste en mémoire côté domaine)</li>
 *   <li>Ne stocke pas de total de pénalités : calcul à la demande via {@link Emprunt}</li>
 *   <li>Porte un {@link Role} fonctionnel (autorisation) distinct des règles métier</li>
 * </ul>
 *
 * <p>Invariants métier :</p>
 * <ul>
 *   <li>Au plus {@link #MAX_EMPRUNTS} emprunts EN COURS</li>
 *   <li>Un adhérent avec retard/pénalités en cours ne peut pas emprunter</li>
 * </ul>
 *
 * <p>Remarque : l'autorisation (peut-il faire l'action ?) est distincte des règles métier
 * (a-t-il le droit fonctionnellement vs a-t-il le droit au regard des contraintes de gestion).
 * L'autorisation s'appuie sur {@link Role}/{@link Capability}, les règles métier sur l'état des {@link Emprunt}.</p>
 */
public class Adherent extends Personne {

    /** Nombre maximal d'emprunts simultanés autorisés côté métier. */
    public static final int MAX_EMPRUNTS = 5;

    // --- État propre à l'adhérent ---
    private LocalDate dateInscription;
    /** Collection des emprunts (seuls les "en cours" devraient être ici si on sépare l'historique). */
    private final List<Emprunt> empruntsActifs = new ArrayList<>();

    // --- Autorisation (sécurité) ---
    /** Rôle fonctionnel de l'adhérent (autorisation). */
    private final Role role = initialiserRoleAdherent();

    // --- Ctors ---
    public Adherent() {
        super();
        this.dateInscription = LocalDate.now();
    }

    public Adherent(Long id, String nom, String prenom, String email, String telephone, LocalDate dateInscription) {
        super(id, nom, prenom, email, telephone);
        this.dateInscription = (dateInscription != null) ? dateInscription : LocalDate.now();
    }

    // --- Initialisation du rôle ADHERENT ---
    private Role initialiserRoleAdherent() {
        Role r = Role.of("ADHERENT");
        r.addCapability(Capability.CONSULT_CATALOG);
        r.addCapability(Capability.BORROW_DOCUMENT);
        r.addCapability(Capability.RETURN_DOCUMENT);
        return r;
    }

    // --- Getters basiques ---
    /** Date d'inscription de l'adhérent. */
    public LocalDate getDateInscription() { return dateInscription; }

    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    /** Vue immuable pour éviter les modifications hors invariants. */
    public List<Emprunt> getEmpruntsActifs() {
        return Collections.unmodifiableList(empruntsActifs);
    }

    /** Rôle fonctionnel (autorisation) de l'adhérent. */
    public Role getRole() { return role; }

    // =========================
    // ===== Métier Adhérent ===
    // =========================

    /**
     * L'adhérent peut-il emprunter "à la date asOf" ?
     * <p>Conditions métier :</p>
     * <ul>
     *   <li>Nombre d'emprunts en cours &lt; {@link #MAX_EMPRUNTS}</li>
     *   <li>Aucun emprunt en retard à {@code asOf}</li>
     *   <li>Aucune pénalité déjà générée (si {@link Penalite} porte un statut, affiner ici)</li>
     * </ul>
     * <p>Note : l'autorisation fonctionnelle (capability {@link Capability#BORROW_DOCUMENT})
     * est une vérification distincte (côté service via AccessPolicy ou via {@link #peutEmprunterCoteAutorisation()}).</p>
     *
     * @param asOf date de référence
     * @return {@code true} si l'adhérent satisfait les contraintes métier d'emprunt
     */
    public boolean peutEmprunter(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        if (empruntsActifs.size() >= MAX_EMPRUNTS) return false;

        boolean aUnRetard = empruntsActifs.stream().anyMatch(e -> e.estEnRetard(asOf));
        if (aUnRetard) return false;

        boolean aPenalites = empruntsActifs.stream().anyMatch(e -> e.getPenalite().isPresent());
        return !aPenalites;
    }

    /**
     * Ajoute un emprunt (créé via {@link Emprunt#creer}) si l'invariant permet d'emprunter.
     * Vérifie aussi que l'emprunt est bien au nom de cet adhérent et que l'emprunt est en cours.
     *
     * @param emprunt emprunt à ajouter
     * @param asOf    date de référence pour la vérification métier
     * @return {@code true} si ajouté, {@code false} sinon (ex. plafond atteint)
     */
    public boolean ajouterEmprunt(Emprunt emprunt, LocalDate asOf) {
        Objects.requireNonNull(emprunt, "emprunt");
        Objects.requireNonNull(asOf, "asOf");
        if (emprunt.getEmprunteur() != this) {
            throw new IllegalArgumentException("L'emprunt n'appartient pas à cet adhérent.");
        }
        if (!peutEmprunter(asOf)) return false;
        if (!emprunt.estEnCours()) {
            throw new IllegalArgumentException("On ne peut ajouter qu'un emprunt en cours.");
        }
        return empruntsActifs.add(emprunt);
    }

    /**
     * Clôture un emprunt actif (retour) et, si nécessaire, génère une pénalité.
     * L'objet peut rester dans la liste (purge/archivage à faire côté service si besoin).
     *
     * @param emprunt    emprunt actif à clôturer
     * @param dateRetour date effective de retour
     */
    public void cloturerEmprunt(Emprunt emprunt, LocalDate dateRetour) {
        Objects.requireNonNull(emprunt, "emprunt");
        Objects.requireNonNull(dateRetour, "dateRetour");
        if (!empruntsActifs.contains(emprunt)) {
            throw new IllegalArgumentException("Emprunt non trouvé parmi les emprunts actifs de l'adhérent.");
        }
        emprunt.cloturerRetour(dateRetour);
        emprunt.genererPenaliteSiNecessaire(dateRetour, Emprunt.PENALITE_PAR_JOUR_DEFAUT);
    }

    /**
     * Liste des pénalités attachées aux emprunts actifs.
     * @return liste (copie) des pénalités présentes
     */
    public List<Penalite> listerPenalites() {
        return empruntsActifs.stream()
                .flatMap(e -> e.getPenalite().stream())
                .collect(Collectors.toList());
    }

    /**
     * Somme des pénalités courantes (non "générées") au sens calcul instantané,
     * uniquement sur les retards des emprunts EN COURS à la date {@code asOf}.
     * Utilise la politique par défaut d'Emprunt (0,50 €/jour).
     *
     * @param asOf date de référence
     * @return montant total calculé à la volée
     */
    public Money penalitesCourantes(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        Money total = Money.euros(0.0);
        for (Emprunt e : empruntsActifs) {
            if (e.estEnCours() && e.estEnRetard(asOf)) {
                total = total.add(e.penaliteCourante(asOf)); // par défaut 0.50€/j
            }
        }
        return total;
    }

    /**
     * Génère et attache des pénalités pour tous les emprunts en retard à {@code asOf}
     * (si pas déjà générées). Retourne la liste des pénalités nouvellement créées.
     *
     * @param asOf date de référence
     * @return liste des nouvelles pénalités créées
     */
    public List<Penalite> genererPenalitesSiNecessaire(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        List<Penalite> nouvelles = new ArrayList<>();
        for (Emprunt e : empruntsActifs) {
            e.genererPenaliteSiNecessaire(asOf, Emprunt.PENALITE_PAR_JOUR_DEFAUT)
             .ifPresent(nouvelles::add);
        }
        return nouvelles;
    }

    /**
     * Retire l'emprunt de la liste des "actifs".
     * À appeler côté service si tu sépares actif / historique après retour.
     *
     * @param emprunt emprunt à retirer
     * @return {@code true} si retiré
     */
    public boolean retirerEmprunt(Emprunt emprunt) {
        Objects.requireNonNull(emprunt, "emprunt");
        return empruntsActifs.remove(emprunt);
    }

    // --- Aides de lecture ---
    /** @return vue immuable des emprunts en cours. */
    public List<Emprunt> empruntsEnCours() {
        return empruntsActifs.stream()
                .filter(Emprunt::estEnCours)
                .collect(Collectors.toUnmodifiableList());
    }

    /** @return vue immuable des emprunts en retard à {@code asOf}. */
    public List<Emprunt> empruntsEnRetard(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        return empruntsActifs.stream()
                .filter(e -> e.estEnRetard(asOf))
                .collect(Collectors.toUnmodifiableList());
    }

    // ===== Helpers autorisation (optionnels) =====

    /** Autorisation : l'adhérent possède-t-il la capability de consulter le catalogue ? */
    public boolean peutConsulterCatalogue() {
        return role.hasCapability(Capability.CONSULT_CATALOG);
    }

    /** Autorisation : l'adhérent possède-t-il la capability d'emprunter (côté sécurité) ? */
    public boolean peutEmprunterCoteAutorisation() {
        return role.hasCapability(Capability.BORROW_DOCUMENT);
    }

    /** Autorisation : l'adhérent possède-t-il la capability de retourner (côté sécurité) ? */
    public boolean peutRetournerCoteAutorisation() {
        return role.hasCapability(Capability.RETURN_DOCUMENT);
    }
    
    /** L’adhérent a-t-il des pénalités (déjà générées OU courantes à la date asOf) ? */
    public boolean hasPenalite(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        return !listerPenalites().isEmpty()
            || penalitesCourantes(asOf).isPositive(); // Money.isPositive()
    }

    /** Variante utilitaire pilotée par horloge (utile en tests). */
    public boolean hasPenalite(Clock clock) {
        return hasPenalite(LocalDate.now(clock));
    }

    /** Variante “now” (à éviter en tests unitaires). */
    public boolean hasPenaliteNow() {
        return hasPenalite(LocalDate.now());
        }

    @Override
    public String toString() {
        return super.toString() +
                " | Adhérent depuis " + dateInscription +
                " | Emprunts actifs: " + empruntsActifs.size();
    }

    /**
     * Renvoie le nom complet de l'adhérent (Prénom Nom).
     * Gère les cas où le prénom ou le nom pourraient être null ou vides.
     * @return Le nom complet formaté.
     */
    public String getNomComplet() {
        // Utilise les getters hérités de Personne
        String prenom = getPrenom();
        String nom = getNom();

        // Utilisation d'une méthode helper pour simplifier la gestion des null/blancs
        String prenomPart = (prenom == null || prenom.isBlank()) ? "" : prenom.trim();
        String nomPart = (nom == null || nom.isBlank()) ? "" : nom.trim();

        // Combine les deux parties, en ajoutant un espace seulement si les deux existent
        if (!prenomPart.isEmpty() && !nomPart.isEmpty()) {
            return prenomPart + " " + nomPart;
        } else {
            // Si l'un ou l'autre est vide, retourne juste la partie non vide (ou une chaîne vide si les deux le sont)
            return prenomPart + nomPart;
        }
    }
    
    
}
