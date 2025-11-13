package application.domain.core.personne;

/**
 * Classe abstraite représentant une personne dans le système.
 * Sert de base pour les entités telles qu'Adherent, Employe, etc.
 */
public abstract class Personne {

    // === Attributs communs ===
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    // === Constructeurs ===
    public Personne() {
        super();
    }

    public Personne(Long id, String nom, String prenom, String email, String telephone) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
    }

    // === Getters / Setters ===
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    // === Méthodes utilitaires ===
    @Override
    public String toString() {
        return prenom + " " + nom + " (" + email + ")";
    }
}
