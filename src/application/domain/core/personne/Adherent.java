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
 * Représente un <strong>adhérent</strong> (membre de la bibliothèque) dans le domaine SGEB.
 *
 * <p>L’adhérent :</p>
 * <ul>
 *     <li>Hérite de {@link Personne} (id, nom, prénom, email, téléphone).</li>
 *     <li>Possède une date d’inscription à la bibliothèque.</li>
 *     <li>Maintient en mémoire ses emprunts actifs ({@link Emprunt}).</li>
 *     <li>Ne stocke pas de total de pénalités : ce total est calculé à la demande.</li>
 *     <li>Dispose d’un rôle fonctionnel {@link Role} de type {@code ADHERENT}
 *         (autorisation / sécurité via {@link Capability}).</li>
 * </ul>
 *
 * <h3>Règles métier (invariants) principales</h3>
 * <ul>
 *     <li>Un adhérent ne peut pas avoir plus de {@link #MAX_EMPRUNTS} emprunts en cours.</li>
 *     <li>Un adhérent avec des retards ou des pénalités en cours ne peut pas emprunter.</li>
 * </ul>
 *
 * <p>
 * Attention : les <strong>autorisations</strong> (peut-il techniquement faire l’action,
 * via {@link Role}/{@link Capability}) sont distinctes des <strong>règles métier</strong>
 * (a-t-il le droit au regard de son état : retards, pénalités, plafond atteint…).
 * Ici, on gère les règles métier ; l’autorisation est une vérification complémentaire.
 * </p>
 */
public class Adherent extends Personne {

    /** Nombre maximal d'emprunts simultanés autorisés côté métier. */
    public static final int MAX_EMPRUNTS = 5;

    // ======================================================================
    // ================================ CHAMPS ===============================
    // ======================================================================

    /** Date d'inscription de l'adhérent à la bibliothèque. */
    private LocalDate dateInscription;

    /**
     * Collection des emprunts actifs de l’adhérent.
     * <p>
     * Par convention, on ne garde dans cette liste que les emprunts
     * considérés comme "actifs" (en cours de suivi).  
     * La séparation actif / historique est gérée côté service si besoin.
     * </p>
     */
    private final List<Emprunt> empruntsActifs = new ArrayList<>();

    /** Rôle fonctionnel de l'adhérent (modèle RBAC). */
    private final Role role = initialiserRoleAdherent();

    // ======================================================================
    // ============================= CONSTRUCTEURS ===========================
    // ======================================================================

    /**
     * Constructeur vide requis pour certains frameworks (ORM, désérialisation).
     * <p>
     * La date d’inscription est initialisée à la date du jour.
     * </p>
     */
    public Adherent() {
        super();
        this.dateInscription = LocalDate.now();
    }

    /**
     * Constructeur complet.
     *
     * @param id identifiant technique (peut être null avant persistance)
     * @param nom nom de l’adhérent
     * @param prenom prénom de l’adhérent
     * @param email adresse email
     * @param telephone téléphone
     * @param dateInscription date d’inscription (si null → aujourd’hui)
     */
    public Adherent(Long id,
                    String nom,
                    String prenom,
                    String email,
                    String telephone,
                    LocalDate dateInscription) {
        super(id, nom, prenom, email, telephone);
        this.dateInscription = (dateInscription != null) ? dateInscription : LocalDate.now();
    }

    // ======================================================================
    // ============================== RÔLE / RBAC ============================
    // ======================================================================

    /**
     * Initialise le rôle fonctionnel {@code ADHERENT} avec les capacités
     * standard d’un membre de bibliothèque.
     *
     * @return rôle configuré
     */
    private Role initialiserRoleAdherent() {
        Role r = Role.of("ADHERENT");
        r.addCapability(Capability.CONSULT_CATALOG);
        r.addCapability(Capability.BORROW_DOCUMENT);
        r.addCapability(Capability.RETURN_DOCUMENT);
        return r;
    }

    // ======================================================================
    // ================================ GETTERS ==============================
    // ======================================================================

    /** @return date d’inscription de l’adhérent. */
    public LocalDate getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDate dateInscription) {
        this.dateInscription = dateInscription;
    }

    /**
     * Retourne une vue <strong>immuable</strong> des emprunts actifs.
     * <p>Cela évite les modifications directes en dehors des méthodes métier.</p>
     */
    public List<Emprunt> getEmpruntsActifs() {
        return Collections.unmodifiableList(empruntsActifs);
    }

    /** @return rôle fonctionnel (sécurité) de l’adhérent. */
    public Role getRole() {
        return role;
    }

    // ======================================================================
    // =============================== MÉTIER ================================
    // ======================================================================

    /**
     * Vérifie si l'adhérent peut emprunter à la date {@code asOf}, du point de vue
     * des <strong>règles métier</strong> (sans tenir compte du RBAC).
     *
     * <p>Conditions :</p>
     * <ul>
     *     <li>Nombre d'emprunts en cours &lt; {@link #MAX_EMPRUNTS}</li>
     *     <li>Aucun emprunt en retard à la date {@code asOf}</li>
     *     <li>Aucune pénalité déjà générée (via {@link Emprunt#getPenalite()})</li>
     * </ul>
     *
     * @param asOf date de référence
     * @return {@code true} si l’adhérent satisfait les contraintes métier pour emprunter
     */
    public boolean peutEmprunter(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");

        // Plafond d’emprunts en cours
        if (empruntsActifs.size() >= MAX_EMPRUNTS) {
            return false;
        }

        // Retard ?
        boolean aUnRetard = empruntsActifs.stream().anyMatch(e -> e.estEnRetard(asOf));
        if (aUnRetard) {
            return false;
        }

        // Pénalité déjà générée ?
        boolean aPenalites = empruntsActifs.stream().anyMatch(e -> e.getPenalite().isPresent());
        return !aPenalites;
    }

    /**
     * Ajoute un emprunt à la liste des emprunts actifs si les conditions métier le permettent.
     *
     * <p>Vérifications :</p>
     * <ul>
     *     <li>L’emprunt ne doit pas être null.</li>
     *     <li>L’emprunt doit appartenir à cet adhérent.</li>
     *     <li>L’emprunt doit être en cours ({@link Emprunt#estEnCours()}).</li>
     *     <li>{@link #peutEmprunter(LocalDate)} doit être vrai à la date {@code asOf}.</li>
     * </ul>
     *
     * @param emprunt emprunt à ajouter (créé via {@link Emprunt#creer} par exemple)
     * @param asOf date de référence pour la vérification des règles métier
     * @return {@code true} si l’emprunt a été ajouté, {@code false} si refus métier (plafond, retard, pénalités)
     * @throws IllegalArgumentException si l’emprunt ne correspond pas à cet adhérent ou n’est pas en cours
     */
    public boolean ajouterEmprunt(Emprunt emprunt, LocalDate asOf) {
        Objects.requireNonNull(emprunt, "emprunt");
        Objects.requireNonNull(asOf, "asOf");

        if (emprunt.getEmprunteur() != this) {
            throw new IllegalArgumentException("L'emprunt n'appartient pas à cet adhérent.");
        }
        if (!peutEmprunter(asOf)) {
            return false;
        }
        if (!emprunt.estEnCours()) {
            throw new IllegalArgumentException("On ne peut ajouter qu'un emprunt en cours.");
        }

        return empruntsActifs.add(emprunt);
    }

    /**
     * Clôture un emprunt actif (retour du document) et génère une pénalité si nécessaire.
     * <p>
     * L’emprunt reste dans la liste des actifs : la séparation actif / historique
     * peut être gérée par un service qui appelle ensuite {@link #retirerEmprunt(Emprunt)}.
     * </p>
     *
     * @param emprunt emprunt actif à clôturer
     * @param dateRetour date effective de retour
     * @throws IllegalArgumentException si l’emprunt n’est pas connu comme actif pour cet adhérent
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
     * Liste les pénalités (déjà générées) attachées aux emprunts actifs de l’adhérent.
     *
     * @return liste (copie) des pénalités existantes
     */
    public List<Penalite> listerPenalites() {
        return empruntsActifs.stream()
                .flatMap(e -> e.getPenalite().stream())
                .collect(Collectors.toList());
    }

    /**
     * Calcule le montant total des pénalités <strong>courantes</strong>
     * (non encore générées en entité {@link Penalite}) pour les emprunts
     * en cours et en retard à la date {@code asOf}.
     *
     * <p>Utilise la politique par défaut de {@link Emprunt} (0,50 €/jour).</p>
     *
     * @param asOf date de référence
     * @return montant total calculé à la volée en {@link Money}
     */
    public Money penalitesCourantes(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        Money total = Money.euros(0.0);

        for (Emprunt e : empruntsActifs) {
            if (e.estEnCours() && e.estEnRetard(asOf)) {
                total = total.add(e.penaliteCourante(asOf)); // 0,50 €/jour par défaut
            }
        }
        return total;
    }

    /**
     * Génère des {@link Penalite} pour tous les emprunts en retard à la date
     * {@code asOf}, si ce n’est pas déjà fait.
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
     * Retire un emprunt de la liste des "emprunts actifs".
     * <p>
     * Généralement appelé par un service lorsque l’emprunt est archivé
     * dans un historique.
     * </p>
     *
     * @param emprunt emprunt à retirer
     * @return {@code true} si l’emprunt était présent et a été retiré
     */
    public boolean retirerEmprunt(Emprunt emprunt) {
        Objects.requireNonNull(emprunt, "emprunt");
        return empruntsActifs.remove(emprunt);
    }

    // ======================================================================
    // ===================== AIDES DE LECTURE / VUES FILTRÉES ===============
    // ======================================================================

    /**
     * @return liste immuable des emprunts en cours.
     */
    public List<Emprunt> empruntsEnCours() {
        return empruntsActifs.stream()
                .filter(Emprunt::estEnCours)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retourne la liste des emprunts en retard à {@code asOf}.
     *
     * @param asOf date de référence
     * @return liste immuable des emprunts en retard
     */
    public List<Emprunt> empruntsEnRetard(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        return empruntsActifs.stream()
                .filter(e -> e.estEnRetard(asOf))
                .collect(Collectors.toUnmodifiableList());
    }

    // ======================================================================
    // =========================== AUTORISATION HELPER =======================
    // ======================================================================

    /** @return true si l'adhérent possède la capacité de consulter le catalogue. */
    public boolean peutConsulterCatalogue() {
        return role.hasCapability(Capability.CONSULT_CATALOG);
    }

    /** @return true si l'adhérent possède la capacité d’emprunter (côté sécurité). */
    public boolean peutEmprunterCoteAutorisation() {
        return role.hasCapability(Capability.BORROW_DOCUMENT);
    }

    /** @return true si l'adhérent possède la capacité de retourner un document (côté sécurité). */
    public boolean peutRetournerCoteAutorisation() {
        return role.hasCapability(Capability.RETURN_DOCUMENT);
    }

    /**
     * Indique si l’adhérent a des pénalités :
     * <ul>
     *     <li>soit déjà générées ({@link #listerPenalites()})</li>
     *     <li>soit courantes (calcul à la date {@code asOf})</li>
     * </ul>
     *
     * @param asOf date de référence
     * @return true si au moins une pénalité existe ou est due
     */
    public boolean hasPenalite(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        return !listerPenalites().isEmpty()
                || penalitesCourantes(asOf).isPositive();
    }

    /**
     * Variante de {@link #hasPenalite(LocalDate)} pilotée par une {@link Clock},
     * pratique pour les tests unitaires.
     */
    public boolean hasPenalite(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return hasPenalite(LocalDate.now(clock));
    }

    /**
     * Variante utilisant la date système actuelle.
     * <p>A éviter dans les tests unitaires, préférer {@link #hasPenalite(Clock)}.</p>
     */
    public boolean hasPenaliteNow() {
        return hasPenalite(LocalDate.now());
    }

    // ======================================================================
    // =============================== UTILITAIRE ============================
    // ======================================================================

    @Override
    public String toString() {
        return super.toString() +
                " | Adhérent depuis " + dateInscription +
                " | Emprunts actifs: " + empruntsActifs.size();
    }

    /**
     * Renvoie le nom complet de l'adhérent (Prénom Nom), en gérant
     * les cas où le prénom ou le nom seraient null ou vides.
     *
     * @return nom complet formaté, ou chaîne vide si aucune info
     */
    public String getNomComplet() {
        String prenom = getPrenom();
        String nom = getNom();

        String prenomPart = (prenom == null || prenom.isBlank()) ? "" : prenom.trim();
        String nomPart = (nom == null || nom.isBlank()) ? "" : nom.trim();

        if (!prenomPart.isEmpty() && !nomPart.isEmpty()) {
            return prenomPart + " " + nomPart;
        }
        // Si un seul est non vide, on le retourne ; sinon, chaîne vide
        return prenomPart + nomPart;
    }
}