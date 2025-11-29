# SSGEB — Système de Gestion des Emprunts de Bibliothèque

**SGEB** est une application de bureau complète dédiée à la gestion d'une bibliothèque.  
Elle permet aux **administrateurs** de gérer le catalogue, les utilisateurs et les emprunts,  
et offre aux **adhérents** un espace dédié pour consulter leurs prêts et leurs pénalités.

Développé dans le cadre du **Master MIAGE (Université Paris Dauphine – PSL)**, ce projet illustre la mise en œuvre conjointe de **l’Architecture Hexagonale** et du **Domain-Driven Design (DDD)**.

---

# Architecture Logicielle & Choix DDD

Le projet respecte strictement la **séparation des préoccupations (SoC)** grâce à une organisation en **trois couches concentriques**.

## Pourquoi un choix DDD ?

- **Indépendance totale du métier**  
  Le domaine ne dépend ni de JavaFX ni de SQLite.  
  Le calcul des pénalités ou la gestion des emprunts peuvent être testés sans interface graphique.

- **Langage Ubiquitaire**  
  Le code reprend les concepts métier réels : *Emprunt*, *Adhérent*, *Pénalité*, *Catalogue*…

- **Évolutivité**  
  Une migration vers PostgreSQL ou une version Web n’impacte pas le domaine.

---

# Structure des Couches

## 1. Le Domaine — `application.domain`

Le **cœur du système**, entièrement indépendant des technologies.

### Core  
Entités riches (Emprunt, Adherent, Livre, CD…) et Value Objects (Money).

### Policy  
Règles métier telles que :
- limite de 5 emprunts,
- pénalité de 0,50€/jour,
- durée de prêt de 21 jours.

### Ports  
Interfaces définissant les contrats pour :
- la persistance (*AdherentRepository, DocumentRepository…*),
- les services externes (*BookInfoProvider, CoverStoragePort*).

### Services  
Orchestration des cas d’usage avec :
- *EmpruntService*
- *CatalogueService*
- *AccountService*
- *PenaliteService*

---

## 2.️ L’Infrastructure — `application.infrastructure`

Les **adaptateurs** concrets qui implémentent les ports du domaine.

### SQLite  
Transformation des objets métier en requêtes SQL via :
- *SqliteAdherentRepository*  
- *SqliteDocumentRepository*  
- *SqliteEmpruntRepository*  
- *SqlitePenaliteRepository*

### Services externes  
- *OpenLibraryProvider* : récupération des métadonnées (ISBN).  
- *LocalCoverStorageAdapter* : stockage des couvertures d’ouvrages.

---

## 3. Interface Utilisateur — `application.ui` (JavaFX + MVVM)

### View (FXML)  
Définit l’apparence : boutons, tableaux, cartes, etc.

### ViewModel  
Contient l’état observable et la logique de présentation.

### Controller  
Fait le lien entre la Vue et le ViewModel (action utilisateur → service).

### Navigator  
Service centralisé de navigation entre les écrans.

---

# Fonctionnalités

## Espace Administrateur

### Tableau de bord  
- KPIs (prêts en cours, retards, adhérents actifs)  
- Graphiques statistiques  
- Carrousel des derniers emprunts

### Export PDF  
- Rapport d’activité  
- Statistiques globales  
- Historique des emprunts

### Gestion du Catalogue  
- CRUD complet (Livres, Magazines, CD)  
- Import automatique par ISBN via **API OpenLibrary**  
- Mise à jour des couvertures (API ou fichier local)

### ‍Gestion des Adhérents  
- Création et suspension de comptes  
- Réinitialisation de mot de passe

### Gestion des Flux  
- Enregistrement d’emprunts et retours  
- Calcul automatique des pénalités en retard

### Gestion des Pénalités  
- Liste de toutes les pénalités actives  
- Validation des demandes de paiement d’adhérents

---

## Espace Adhérent

### Catalogue  
Recherche par titre, auteur, genre, ISBN.

### Mes Emprunts  
- Suivi en temps réel  
- Historique complet  
- Disponibilité des ouvrages

### Mes Pénalités  
- Visualisation immédiate  
- Possibilité de demander une régularisation  
  (transmise à l’administrateur pour validation)

---

# Limites & Améliorations Futures

## Limites Actuelles

- **Sécurité des mots de passe**  
  Hachage simplifié (`hashCode()`).  
  → À remplacer par BCrypt ou Argon2.

- **Session utilisateur**  
  Pas d'expiration automatique.

- **Mono-poste**  
  SQLite ne permet pas une utilisation sur plusieurs postes en simultané.

## Améliorations Futures (Roadmap)

- Envoi d’emails via un **NotificationPort** (JavaMail).  
- Système de réservation des ouvrages.  
- Intégration d’un lecteur code-barres USB.  
- Export CSV / Excel.  
- Passage à PostgreSQL pour un usage multi-utilisateurs.

---

# Stack Technique
- **Langage** : Java 21  
- **Interface** : JavaFX + CSS  
- **BDD** : SQLite (`sgeb.db`)  
- **Build** : Maven  

### Dépendances clés
- `sqlite-jdbc` — Connexion à SQLite  
- `gson` — Parsing JSON (OpenLibrary)  
- `pdfbox` — Export PDF  
- `javafx-controls`, `javafx-fxml`

---

# Installation & Démarrage

## Prérequis
- JDK 21+
- Maven 3+
- JavaFX (géré automatiquement par Maven)

---
## Option A : Exécutable (recommandé)

À la racine du projet se trouve un dossier **`Livrable/`**, contenant :

- le fichier **`SGEB_Projet-0.0.1-SNAPSHOT.jar`**  
- le dossier **`data/`** (base SQLite)  
- le dossier **`covers/`** (images et couvertures)

Pour exécuter correctement l'application, assurez-vous que **les dossiers `data/` et `covers/` se trouvent bien à côté du fichier `.jar`**, c’est-à-dire dans le même répertoire `Livrable/`.

### Lancement

Depuis un terminal, positionnez-vous dans le dossier `Livrable/` puis lancez :

```bash
java -jar SGEB_Projet-0.0.1-SNAPSHOT.jar
```
L’application se lance immédiatement et charge automatiquement :

- la base SQLite depuis **`data/sgeb.db`**
- les couvertures de documents depuis **`covers/`**


## Option B : Compilation (développeur)

```bash
git clone https://github.com/aemane-m/SGEB_Projet.git
cd SGEB_Projet
mvn clean javafx:run
```
## Premier Lancement — Setup Wizard

Au premier démarrage, l’application ouvre automatiquement le **Setup Wizard**.  
Ce module guide l’utilisateur dans :

- **la création du premier Administrateur**, indispensable pour initialiser le système ;
- l’**import facultatif de données de démonstration** (catalogue minimal, adhérents fictifs), utile pour tester rapidement l’interface.

---

# Modèle de Données

Le SGEB utilise SQLite comme base embarquée (`sgeb.db`).  
Plusieurs choix structurants ont été adoptés pour garantir cohérence et simplicité.

### Héritage TPH (Table Per Hierarchy)
Toutes les sous-classes de `Document` (Livre, CD, Magazine) sont regroupées dans **une seule table** :

- Table : `DOCUMENT`  
- Colonne discriminante : **`dtype`**

Cela évite la duplication de colonnes, simplifie les requêtes, et s’adapte parfaitement aux contraintes d’un projet académique.

### Clés étrangères activées
SQLite désactive les contraintes référentielles par défaut.  
L’application force leur activation via : **`PRAGMA foreign_keys = ON`**


---
# Structure du Projet
```bash
src/main/java/application
├── domain                  # Cœur métier (DDD)
│   ├── core                # Entités : Livre, Emprunt, Adherent...
│   ├── policy              # Règles de gestion
│   ├── ports               # Interfaces (repositories & APIs)
│   └── service             # Services applicatifs
├── infrastructure          # Adaptateurs techniques
│   ├── sqlite              # Implémentations Repository (SQLite)
│   └── external            # API OpenLibrary & stockage images
└── ui                      # Présentation (JavaFX / MVVM)
    ├── view                # FXML
    ├── viewmodel           # ViewModels (état)
    ├── controller          # Contrôleurs JavaFX
    └── navigation          # Gestion de navigation
```
---
# Auteur

Développé par **Aemane MOUTEIROU &  Sara LOPEZ DE VARO**  
Master MIAGE — Université Paris Dauphine / PSL.

© 2025 SGEB Project — Tous droits réservés.


