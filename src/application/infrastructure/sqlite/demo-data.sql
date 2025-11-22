-- =========================
-- PERSONNES (adhérents démo)
-- =========================
-- ⚠ On suppose que le 1er admin a déjà été créé par le wizard
--    → il a normalement personne_id = 1
-- Ces deux lignes créeront des adhérents "simples" (isAdmin = 0).

INSERT INTO PERSONNE (email, password_hash, nom, prenom, created_at, actif, isAdmin, password_must_change)
VALUES (
    'alice.dupont@example.com',
    'demo-hash-1',     -- valeur fictive, non utilisée pour se connecter
    'Dupont',
    'Alice',
    '2025-01-10',
    1,                 -- actif
    0,                 -- isAdmin = false
    0                  -- pas obligé de changer le mot de passe
);

INSERT INTO PERSONNE (email, password_hash, nom, prenom, created_at, actif, isAdmin, password_must_change)
VALUES (
    'bob.martin@example.com',
    'demo-hash-2',
    'Martin',
    'Bob',
    '2025-01-11',
    1,
    0,
    0
);

-- =========================
-- DOCUMENTS DE DÉMONSTRATION
-- =========================
-- dtype doit être parmi : 'Livre', 'Magazine', 'CD'

INSERT INTO DOCUMENT (dtype, titre, annee_publication, editeur, genre, auteur, isbn, nombre_pages)
VALUES (
    'Livre',
    'Le Petit Prince',
    1943,
    'Gallimard',
    'Conte philosophique',
    'Antoine de Saint-Exupéry',
    '9782070612758',
    96
);

INSERT INTO DOCUMENT (dtype, titre, annee_publication, editeur, genre, auteur, isbn, nombre_pages)
VALUES (
    'Livre',
    '1984',
    1949,
    'Secker & Warburg',
    'Roman',
    'George Orwell',
    '9780451524935',
    328
);

-- =========================
-- EMPRUNTS DE DÉMONSTRATION
-- =========================
-- Hypothèse (très probable dans ton scénario) :
--   - Admin créé par le wizard → PERSONNE.personne_id = 1
--   - Alice (1er adhérent démo ci-dessus) → personne_id = 2
--   - Bob   (2ème adhérent démo)         → personne_id = 3
--   - 1er document inséré ci-dessus      → document_id = 1
--   - 2ème document inséré ci-dessus     → document_id = 2

-- Emprunt en cours (non rendu) par Alice sur "Le Petit Prince"
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective)
VALUES (
    1,                             -- document_id
    2,                             -- emprunteur_id (Alice)
    '2025-01-01',
    '2025-01-22',
    NULL                           -- pas encore rendu
);

-- Emprunt rendu en retard par Bob sur "1984"
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective)
VALUES (
    2,                             -- document_id
    3,                             -- emprunteur_id (Bob)
    '2024-12-01',
    '2024-12-22',
    '2025-01-05'          -- rendu en retard
);

-- =========================
-- (OPTIONNEL) PÉNALITÉS DE DÉMONSTRATION
-- =========================
-- Ici, on crée une pénalité pour Bob (adherent_id = 3) sur son emprunt en retard (emprunt_id = 2).
-- ⚠ emprunt_id = 2 suppose que les emprunts ci-dessus sont bien les 1er et 2ème de la table.

INSERT INTO PENALITE (adherent_id, emprunt_id, type, montant, date_debut, date_fin, description, status)
VALUES (
    3,                      -- adherent_id (Bob)
    2,                      -- emprunt_id (emprunt en retard)
    'RETARD',               -- type (libre dans ton schéma)
    7.50,                   -- montant en euros
    '2025-01-06',  -- début de la pénalité
    NULL,                   -- pas encore clôturée
    'Retard sur la restitution du livre "1984".',
    'ACTIVE'                -- statut par défaut
);
