📚 SGEB - Système de Gestion de Bibliothèque

Application de bureau complète pour la gestion d'une bibliothèque, développée en Java (JavaFX) avec une architecture MVC.

🌟 Fonctionnalités

Gestion du catalogue : Ajout, modification, suppression et recherche de livres.

Gestion des adhérents : Inscription et suivi des utilisateurs.

Système d'emprunt : Enregistrement des prêts et retours.

Gestion des pénalités : Calcul automatique des retards et amendes.

Tableau de bord : Vue d'ensemble des statistiques de la bibliothèque.

Persistance des données : Sauvegarde locale via SQLite.

Export PDF : Génération de rapports (via PDFBox).

🛠️ Technologies Utilisées

Langage : Java 21

Interface Graphique : JavaFX (FXML & CSS)

Base de Données : SQLite (via JDBC)

Build Tool : Maven

Dépendances clés :

pdfbox (Génération PDF)

gson / json (Manipulation de données)

sqlite-jdbc

📋 Prérequis

Java 21 (JDK 21) ou supérieur.

Maven (pour la compilation).

🚀 Installation et Exécution

Option A : Depuis l'exécutable (Si fourni dans l'archive)

Si vous avez téléchargé ce projet sous forme d'archive (ZIP) contenant un dossier Livrable :

Ouvrez le dossier Livrable.

Assurez-vous que les dossiers data et covers sont présents à côté du .jar.

Double-cliquez sur SGEB_Projet-0.0.1-SNAPSHOT.jar.

Option B : Compilation depuis les sources (GitHub)

Si vous avez cloné le dépôt Git, vous devez construire l'application :

Cloner le projet :

git clone [https://github.com/VOTRE_USERNAME/SGEB_Projet.git](https://github.com/VOTRE_USERNAME/SGEB_Projet.git)
cd SGEB_Projet


Compiler et générer le Fat JAR :

mvn clean package


Lancer l'application :
Une fois la compilation réussie, il est recommandé de copier le JAR généré vers un dossier propre (pour qu'il trouve la base de données) ou de le lancer ainsi :

Création du dossier de test :

mkdir run_app
cp target/SGEB_Projet-0.0.1-SNAPSHOT.jar run_app/
cp -r data run_app/
cp -r covers run_app/
cd run_app
java -jar SGEB_Projet-0.0.1-SNAPSHOT.jar


📂 Structure du Projet

SGEB_Projet/
├── src/               # Code source (Java & FXML)
├── data/              # Base de données SQLite (sgeb.db)
├── covers/            # Images des couvertures de livres
├── pom.xml            # Configuration Maven
└── README.md          # Documentation


👤 Auteur

[Votre Nom] - Projet Étudiant

Ce projet a été réalisé dans le cadre d'un cursus universitaire.