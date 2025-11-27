-- =====================================================================
-- DEMO SGEB : JEU DE DONNEES "STRESS TEST" (SCENARIO 3 MOIS)
-- Objectif : 10 Pénalités, 10 Retards en cours, Activité sur 7j/30j/90j
-- Format Date : ISO 8601 Strict (YYYY-MM-DDTHH:MM:SS.SSSZ)
-- =====================================================================

-- =====================================================================
-- 1. PERSONNE (Adhérents)
-- 20 Adhérents pour supporter la charge des emprunts
-- =====================================================================
INSERT INTO PERSONNE (email, password_hash, nom, prenom, telephone, isAdmin, actif, status, password_must_change, created_at) VALUES
    ('alice.dupont@example.com',    'hash', 'Dupont',   'Alice',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-90 days')),
    ('bob.martin@example.com',      'hash', 'Martin',   'Bob',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-85 days')),
    ('claire.bernard@example.com',  'hash', 'Bernard',  'Claire',  NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-80 days')),
    ('david.leroy@example.com',     'hash', 'Leroy',    'David',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-75 days')),
    ('emma.rossi@example.com',      'hash', 'Rossi',    'Emma',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days')),
    ('felix.moreau@example.com',    'hash', 'Moreau',   'Felix',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-65 days')),
    ('lea.gomes@example.com',       'hash', 'Gomes',    'Lea',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-60 days')),
    ('hugo.lambert@example.com',    'hash', 'Lambert',  'Hugo',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-55 days')),
    ('ines.robert@example.com',     'hash', 'Robert',   'Ines',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days')),
    ('jules.faure@example.com',     'hash', 'Faure',    'Jules',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days')),
    ('karim.lopez@example.com',     'hash', 'Lopez',    'Karim',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days')),
    ('laura.petit@example.com',     'hash', 'Petit',    'Laura',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-35 days')),
    ('maxime.blanc@example.com',    'hash', 'Blanc',    'Maxime',  NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-30 days')),
    ('nina.garcia@example.com',     'hash', 'Garcia',   'Nina',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-25 days')),
    ('oscar.henry@example.com',     'hash', 'Henry',    'Oscar',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-20 days')),
    ('paul.renaud@example.com',     'hash', 'Renaud',   'Paul',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days')),
    ('sarah.colin@example.com',     'hash', 'Colin',    'Sarah',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days')),
    ('tom.meyer@example.com',       'hash', 'Meyer',    'Tom',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days')),
    ('victor.noel@example.com',     'hash', 'Noel',     'Victor',  NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days')),
    ('zoe.marchand@example.com',    'hash', 'Marchand', 'Zoe',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));

-- =====================================================================
-- 2. DOCUMENT (Catalogue)
-- =====================================================================
INSERT INTO DOCUMENT (dtype, titre, annee_publication, editeur, genre, auteur, isbn, nombre_pages, numero, periodicite, date_parution, artiste, duree_minutes, cover_path, cover_mime, cover_updated_at) VALUES
    ('Livre', 'Feuilles d''automne', 2002, 'France loisirs', 'Biography', 'Adeline Yen Mah', '9782744151866', 301, NULL, NULL, NULL, NULL, NULL, 'covers/0444c2d3-da0d-4f3f-a929-2108efc10e17.jpg', 'image/jpeg', 1763933106451),
    ('Livre', 'Le théorème du homard', 2014, 'NiL', 'Roman', 'Graeme Simsion', '9782841117208', 382, NULL, NULL, NULL, NULL, NULL, 'covers/510249c0-c1a6-4ffe-890a-00d84e76b6bd.jpg', 'image/jpeg', 1763933125937),
    ('Livre', 'Running Man', 1996, 'J''ai Lu', 'Horror', 'Stephen King', '9782290026946', 250, NULL, NULL, NULL, NULL, NULL, 'covers/baf9dc97-c217-46b0-b866-bf6572a62cba.jpg', 'image/jpeg', 1763933155682),
    ('Livre', 'Le Dernier Restaurant', 2004, 'Denoel', 'Sci-Fi', 'Douglas Adams', '9782070416158', 214, NULL, NULL, NULL, NULL, NULL, 'covers/322b3881-0aae-4643-a481-242f69c2e939.jpg', 'image/jpeg', 1763933196222),
    ('Livre', 'Octobre Rouge', 1986, 'A. Michel', 'Thriller', 'Tom Clancy', '9782226025913', 485, NULL, NULL, NULL, NULL, NULL, 'covers/af09d581-ab81-459c-8f55-1d8185e990d3.jpg', 'image/jpeg', 1763933265065),
    ('Livre', 'La dernière énigme', 2014, 'Masque', 'Policier', 'Agatha Christie', '9782702440926', 290, NULL, NULL, NULL, NULL, NULL, 'covers/493169d5-4aa8-44b4-a1af-c4e960031ac6.jpg', 'image/jpeg', 1763933303315),
    ('Livre', 'No Logo', 2007, 'Editions 84', 'Essai', 'Naomi Klein', '9782290003527', 200, NULL, NULL, NULL, NULL, NULL, 'covers/5fe2ca46-5058-48da-990e-6986e374dbb5.jpg', 'image/jpeg', 1763933361062),
    ('Livre', 'Computer networks', 2011, 'Pearson', 'Tech', 'Andrew S. Tanenbaum', '9782744075216', 950, NULL, NULL, NULL, NULL, NULL, 'covers/36435d83-3080-4126-8e8b-8084d70604d2.jpg', 'image/jpeg', 1763933383676),
    ('Livre', 'Pas de lettre pour le colonel', 1980, 'Grasset', 'Roman', 'Gabo', '9782246252313', 125, NULL, NULL, NULL, NULL, NULL, 'covers/ee30b918-6c34-4a66-9336-0a804f816881.jpg', 'image/jpeg', 1763933435600),
    ('Livre', 'Skellig', 2000, 'Flammarion', 'Jeunesse', 'David Almond', '9782081645813', 266, NULL, NULL, NULL, NULL, NULL, 'covers/054f753d-9052-496c-aca3-da6b969afbb5.jpg', 'image/jpeg', 1763933463401),
    ('Livre', 'Dracula', 2005, 'Ecole des Loisirs', 'Horror', 'Bram Stoker', '9782211074384', 221, NULL, NULL, NULL, NULL, NULL, 'covers/57840659-03b2-4981-aa47-6c2fc9e4719d.jpg', 'image/jpeg', 1763933489192),
    ('Livre', 'Livre de La Jungle', 1987, 'Schoenhof', 'Jeunesse', 'Rudyard Kipling', '9782070334568', 200, NULL, NULL, NULL, NULL, NULL, 'covers/9e9c4e80-a2d4-4456-82f2-e1d02868193a.jpg', 'image/jpeg', 1763933813442),
    ('Livre', 'The Pelican Brief', 1994, 'France Loisirs', 'Thriller', 'John Grisham', '9782724278859', 200, NULL, NULL, NULL, NULL, NULL, 'covers/b2f8b5e0-24aa-4e56-bad3-4a8acf44e711.jpg', 'image/jpeg', 1763933926638),
    ('Livre', 'La dernière récolte', 2002, 'GLM', 'Thriller', 'John Grisham', '9782702875636', 200, NULL, NULL, NULL, NULL, NULL, 'covers/4fb4faab-7c49-4b4b-8918-f8983aa72b18.jpg', 'image/jpeg', 1763933966708),
    ('Livre', 'Pour toujours', 1998, 'Medium', 'Romance', 'Judy Blume', '9782211098106', 209, NULL, NULL, NULL, NULL, NULL, 'covers/7343cedc-618d-4ce0-9872-921aa96b1f50.jpg', 'image/jpeg', 1763934068153),
    ('Livre', 'Le Petit Prince', 2007, 'Gallimard', 'Jeunesse', 'Saint-Exupéry', '9782070612758', 120, NULL, NULL, NULL, NULL, NULL, 'covers/7d96e4b8-553c-4dcd-b090-1551aa4c0c8a.jpg', 'image/jpeg', 1763934633425),
    ('Livre', 'L’étranger', 1984, 'Gallimard', 'Philosophie', 'Albert Camus', '9782070360024', 185, NULL, NULL, NULL, NULL, NULL, 'covers/95e0c199-0855-4ed0-889c-ac8c55209bd5.jpg', 'image/jpeg', 1763934646918),
    ('Livre', 'La Peste', 2022, 'Gallimard', 'Philosophie', 'Albert Camus', '9782070360420', 355, NULL, NULL, NULL, NULL, NULL, 'covers/8aec0dd4-ab33-4c0b-b609-4d3ee5bf4d49.jpg', 'image/jpeg', 1763934663074),
    ('Livre', 'The lord of the rings', 1991, 'Grafton', 'Fantasy', 'Tolkien', '9780261102385', 200, NULL, NULL, NULL, NULL, NULL, 'covers/2f50d6c2-bbec-425d-9eb2-58321a3503bd.jpg', 'image/jpeg', 1763934673273),
    ('Livre', 'The Two Towers', 1991, 'Grafton', 'Fantasy', 'Tolkien', '9780261102361', 447, NULL, NULL, NULL, NULL, NULL, 'covers/b659b3ff-dcd9-49d5-bdb1-2c94db7a8446.jpg', 'image/jpeg', 1763934684039),
    ('Livre', 'The Return of the King', 1991, 'Grafton', 'Fantasy', 'Tolkien', '9780261102378', 560, NULL, NULL, NULL, NULL, NULL, 'covers/f8f9ff60-72b4-4033-bd7e-1a513d4aa16e.jpg', 'image/jpeg', 1763934714659),
    ('Livre', 'Foundation', 1991, 'Spectra', 'Sci-Fi', 'Isaac Asimov', '9780553293357', 320, NULL, NULL, NULL, NULL, NULL, 'covers/ac34b2ea-7a70-4a65-8331-2e3bf80cd6f0.jpg', 'image/jpeg', 1763934731116),
    ('Livre', 'Dune', 2020, 'Ace', 'Sci-Fi', 'Frank Herbert', '9780441172719', 896, NULL, NULL, NULL, NULL, NULL, 'covers/b12c7ca8-d7ef-4eac-ba37-589b4e3bd570.jpg', 'image/jpeg', 1763934743002),
    ('Livre', 'Sapiens', 2015, 'Vintage', 'Histoire', 'Harari', '9780099590088', 512, NULL, NULL, NULL, NULL, NULL, 'covers/903acd9a-f4fb-4375-af35-1f97c569493e.jpg', 'image/jpeg', 1763934814662),
    ('Livre', 'Homo Deus', 2017, 'Vintage', 'Futurisme', 'Harari', '9781784703936', 528, NULL, NULL, NULL, NULL, NULL, 'covers/1dc2eb6e-225b-45d9-a67a-ca641864393f.jpg', 'image/jpeg', 1763934823757),
    ('Magazine','Science & Vie', 2025,'Mondo','Science',NULL,NULL,NULL,1280,'Mensuel','2025-01-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','National Geo', 2025,'NatGeo','Nature',NULL,NULL,NULL,920, 'Mensuel','2025-02-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Le Monde Diplo', 2025,'Le Monde','Politique',NULL,NULL,NULL,780, 'Mensuel','2025-03-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Wired', 2024,'Condé Nast','Tech', NULL,NULL,NULL,540, 'Mensuel','2024-04-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Linux Mag', 2024,'Diamond','Info', NULL,NULL,NULL,250, 'Mensuel','2024-05-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Psychologies', 2024,'Psychologies','Bien-être',NULL,NULL,NULL,410, 'Mensuel','2024-06-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Esprit', 2023,'Esprit','Culture',NULL,NULL,NULL,500, 'Mensuel','2023-07-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Nature', 2023,'Springer','Science',NULL,NULL,NULL,670, 'Mensuel','2023-08-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Pour la Science', 2022,'Belin','Science',NULL,NULL,NULL,890, 'Mensuel','2022-09-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','MIT Tech Review', 2022,'MIT','Tech',NULL,NULL,NULL,300, 'Mensuel','2022-10-01',NULL,NULL,NULL,NULL,NULL),
    ('CD','Kind of Blue', 1959,'Columbia','Jazz',NULL,NULL,NULL,NULL,NULL,NULL,'Miles Davis', 46,NULL,NULL,NULL),
    ('CD','Abbey Road', 1969,'Apple','Rock', NULL,NULL,NULL,NULL,NULL,NULL,'The Beatles', 47,NULL,NULL,NULL),
    ('CD','Thriller', 1982,'Epic','Pop', NULL,NULL,NULL,NULL,NULL,NULL,'MJ', 42,NULL,NULL,NULL),
    ('CD','Random Access', 2013,'Columbia','Electro',NULL,NULL,NULL,NULL,NULL,NULL,'Daft Punk', 74,NULL,NULL,NULL),
    ('CD','OK Computer', 1997,'Parlophone','Rock',NULL,NULL,NULL,NULL,NULL,NULL,'Radiohead', 53,NULL,NULL,NULL),
    ('CD','Dark Side', 1973,'Harvest','Rock', NULL,NULL,NULL,NULL,NULL,NULL,'Pink Floyd', 43,NULL,NULL,NULL),
    ('CD','Back to Black', 2006,'Island','Soul', NULL,NULL,NULL,NULL,NULL,NULL,'Amy Winehouse', 34,NULL,NULL,NULL),
    ('CD','Discovery', 2001,'Virgin','Electro',NULL,NULL,NULL,NULL,NULL,NULL,'Daft Punk', 60,NULL,NULL,NULL),
    ('CD','Blue Train', 1958,'Blue Note','Jazz', NULL,NULL,NULL,NULL,NULL,NULL,'John Coltrane', 42,NULL,NULL,NULL);


-- =====================================================================
-- 3. EMPRUNT : REPARTITION (10 RETARDS EN COURS, 3 ZONES)
-- =====================================================================

-- ---------------------------------------------------------------------
-- A. ZONE "HISTORIQUE" (J-90 à J-31)
-- Objectif : 3 Retards historiques (livres probablement perdus)
--            3 Emprunts rendus en retard (pour générer 3 pénalités)
--            Des emprunts rendus à l'heure
-- ---------------------------------------------------------------------

INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    -- 1, 2, 3 : LES RETARDS "PERDUS" (Toujours pas rendus, date prévue dépassée depuis 2 mois)
    (1, 2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-85 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-64 days'), NULL),
    (2, 3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-80 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-59 days'), NULL),
    (3, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-75 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-54 days'), NULL),

    -- 4, 5, 6 : RENDUS EN RETARD (Base pour pénalités historiques)
    -- Prévu J-59, rendu J-50 (9j retard)
    (4, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-80 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-59 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days')),
    -- Prévu J-54, rendu J-40 (14j retard)
    (5, 6, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-75 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-54 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days')),
    -- Prévu J-49, rendu J-45 (4j retard)
    (6, 7, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-49 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days')),

    -- 7, 8, 9 : RENDUS A L'HEURE (Activité normale historique)
    (20, 2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-85 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-64 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days')),
    (21, 3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-60 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-39 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days')),
    (22, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-65 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-44 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days'));


-- ---------------------------------------------------------------------
-- B. ZONE "MOIS DERNIER" (J-30 à J-8)
-- Objectif : 4 Retards confirmés (le mois dernier)
--            4 Emprunts rendus en retard (pour générer 4 pénalités)
--            Activité normale
-- ---------------------------------------------------------------------

INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    -- 1, 2, 3, 4 : LES RETARDS DU MOIS (Non rendus, dépassement de 1 à 2 semaines)
    (7,  8, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-19 days'), NULL),
    (8,  9, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-35 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-14 days'), NULL),
    (9,  10,strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-30 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-9 days'), NULL),
    (10, 11,strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-29 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-8 days'), NULL),

    -- 5, 6, 7, 8 : RENDUS EN RETARD (Base pour pénalités du mois)
    -- Prévu J-20, rendu J-15 (5j retard)
    (11, 12, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-41 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-20 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days')),
    -- Prévu J-18, rendu J-10 (8j retard)
    (12, 13, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-39 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-18 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days')),
    -- Prévu J-15, rendu J-12 (3j retard)
    (13, 14, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-36 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-12 days')),
    -- Prévu J-12, rendu J-9 (3j retard)
    (14, 15, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-33 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-12 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-9 days')),

    -- ACTIVITE NORMALE (Rendus à l'heure ou en cours non échus)
    (23, 8, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '+6 days'), NULL), -- En cours OK
    (31, 9, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-12 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '+9 days'), NULL); -- En cours OK


-- ---------------------------------------------------------------------
-- C. ZONE "CETTE SEMAINE" (J-7 à Aujourd'hui)
-- Objectif : 3 Retards tout frais (dûs hier ou avant-hier)
--            3 Emprunts rendus en retard (pénalités immédiates)
--            Nouveaux emprunts
-- ---------------------------------------------------------------------

INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    -- 1, 2, 3 : RETARDS FRAIS (Dûs il y a 2, 3, 4 jours, non rendus)
    (15, 16, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-25 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-4 days'), NULL),
    (16, 17, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-24 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-3 days'), NULL),
    (17, 18, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-23 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days'), NULL),

    -- 4, 5, 6 : RENDUS EN RETARD HIER (Base pour pénalités immédiates)
    -- Prévu J-5, rendu J-1 (4j retard)
    (18, 19, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-26 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day')),
    -- Prévu J-3, rendu J-1 (2j retard)
    (19, 20, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-24 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-3 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day')),
    -- Prévu J-2, rendu J-0 (2j retard)
    (25, 2,  strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-23 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    -- NOUVEAUX EMPRUNTS (5 Derniers prêts - Top 25 garanti)
    -- J-2 (1 prêt, Doc Top 25)
    (23, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days', '+21 days'), NULL),

    -- J-1 (2 prêts, Docs Top 25)
    (22, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day', '+21 days'), NULL),
    (20, 3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day', '+21 days'), NULL),

    -- J-0 (2 prêts, Docs Top 25)
    (24, 6, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '+21 days'), NULL),
    (21, 2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '+21 days'), NULL);


-- =====================================================================
-- 4. 10 PENALITES (Réparties sur les 3 zones)
-- Doivent correspondre aux emprunts "RENDUS EN RETARD" ci-dessus
-- =====================================================================
INSERT INTO PENALITE (adherent_id, emprunt_id, type, montant, date_debut, date_fin, description, status) VALUES
    -- ZONE 90J (3 Pénalités)
    (5, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 5 AND document_id = 4 LIMIT 1), 'RETARD', 5.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days'), NULL, 'Retard 9j historique', 'ACTIVE'),
    (6, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 6 AND document_id = 5 LIMIT 1), 'RETARD', 7.50, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days'), NULL, 'Retard 14j historique', 'ACTIVE'),
    (7, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 7 AND document_id = 6 LIMIT 1), 'RETARD', 2.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days'), NULL, 'Retard 4j historique', 'ACTIVE'),

    -- ZONE 30J (4 Pénalités)
    (12, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 12 AND document_id = 11 LIMIT 1), 'RETARD', 2.50, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days'), NULL, 'Retard moyen', 'ACTIVE'),
    (13, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 13 AND document_id = 12 LIMIT 1), 'RETARD', 4.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days'), NULL, 'Retard moyen', 'ACTIVE'),
    (14, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 14 AND document_id = 13 LIMIT 1), 'RETARD', 1.50, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-12 days'), NULL, 'Petit retard', 'ACTIVE'),
    (15, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 15 AND document_id = 14 LIMIT 1), 'RETARD', 1.50, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-9 days'), NULL, 'Petit retard', 'ACTIVE'),

    -- ZONE 7J (3 Pénalités)
    (19, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 19 AND document_id = 18 LIMIT 1), 'RETARD', 2.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'), NULL, 'Retard frais', 'ACTIVE'),
    (20, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 20 AND document_id = 19 LIMIT 1), 'RETARD', 1.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'), NULL, 'Retard frais', 'ACTIVE'),
    (2,  (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 2  AND document_id = 25 LIMIT 1), 'RETARD', 1.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), NULL, 'Retard du jour', 'ACTIVE');