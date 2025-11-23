-- =====================================================================
-- DEMO SGEB : SCENARIO 90 JOURS DYNAMIQUES
-- Format Date : ISO 8601 Strict (YYYY-MM-DDTHH:MM:SS.SSSZ)
-- Compatible avec : java.time.Instant.parse()
-- =====================================================================

-- =====================================================================
-- 1. PERSONNE (Adhérents)
-- Création échelonnée sur les 90 derniers jours
-- =====================================================================
INSERT INTO PERSONNE (email, password_hash, nom, prenom, telephone, isAdmin, actif, status, password_must_change, created_at) VALUES
    ('alice.dupont@example.com',    'demo-hash-1',  'Dupont',   'Alice',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-90 days')),
    ('bob.martin@example.com',      'demo-hash-2',  'Martin',   'Bob',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-85 days')),
    ('claire.bernard@example.com',  'demo-hash-3',  'Bernard',  'Claire',  NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-80 days')),
    ('david.leroy@example.com',     'demo-hash-4',  'Leroy',    'David',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-75 days')),
    ('emma.rossi@example.com',      'demo-hash-5',  'Rossi',    'Emma',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days')),
    ('felix.moreau@example.com',    'demo-hash-6',  'Moreau',   'Felix',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-65 days')),
    ('lea.gomes@example.com',       'demo-hash-7',  'Gomes',    'Lea',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-60 days')),
    ('hugo.lambert@example.com',    'demo-hash-8',  'Lambert',  'Hugo',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-55 days')),
    ('ines.robert@example.com',     'demo-hash-9',  'Robert',   'Ines',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days')),
    ('jules.faure@example.com',     'demo-hash-10', 'Faure',    'Jules',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days')),
    ('karim.lopez@example.com',     'demo-hash-11', 'Lopez',    'Karim',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days')),
    ('laura.petit@example.com',     'demo-hash-12', 'Petit',    'Laura',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-35 days')),
    ('maxime.blanc@example.com',    'demo-hash-13', 'Blanc',    'Maxime',  NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-30 days')),
    ('nina.garcia@example.com',     'demo-hash-14', 'Garcia',   'Nina',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-25 days')),
    ('oscar.henry@example.com',     'demo-hash-15', 'Henry',    'Oscar',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-20 days')),
    ('paul.renaud@example.com',     'demo-hash-16', 'Renaud',   'Paul',    NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days')),
    ('sarah.colin@example.com',     'demo-hash-17', 'Colin',    'Sarah',   NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days')),
    ('tom.meyer@example.com',       'demo-hash-18', 'Meyer',    'Tom',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days')),
    ('victor.noel@example.com',     'demo-hash-19', 'Noel',     'Victor',  NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days')),
    ('zoe.marchand@example.com',    'demo-hash-20', 'Marchand', 'Zoe',     NULL, 0, 1, 'ACTIVE', 0, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));

-- =====================================================================
-- 2. DOCUMENT (Catalogue)
-- =====================================================================
INSERT INTO DOCUMENT (dtype, titre, annee_publication, editeur, genre, auteur, isbn, nombre_pages, numero, periodicite, date_parution, artiste, duree_minutes, cover_path, cover_mime, cover_updated_at) VALUES
    ('Livre', 'Feuilles d''automne', 2002, 'France loisirs', 'Biography', 'Adeline Yen Mah', '9782744151866', 301, NULL, NULL, NULL, NULL, NULL, 'covers/0444c2d3-da0d-4f3f-a929-2108efc10e17.jpg', 'image/jpeg', 1763933106451),
    ('Livre', 'Le théorème du homard ou Comment trouver la femme idéale', 2014, 'NiL', 'Australia', 'Graeme Simsion', '9782841117208', 382, NULL, NULL, NULL, NULL, NULL, 'covers/510249c0-c1a6-4ffe-890a-00d84e76b6bd.jpg', 'image/jpeg', 1763933125937),
    ('Livre', 'Running Man', 1996, 'J''ai Lu', 'horror fiction', 'Stephen King', '9782290026946', 250, NULL, NULL, NULL, NULL, NULL, 'covers/baf9dc97-c217-46b0-b866-bf6572a62cba.jpg', 'image/jpeg', 1763933155682),
    ('Livre', 'Le Dernier Restaurant Avant La Fin De Monde', 2004, 'Denoel', 'Fiction', 'Douglas Adams', '9782070416158', 214, NULL, NULL, NULL, NULL, NULL, 'covers/322b3881-0aae-4643-a481-242f69c2e939.jpg', 'image/jpeg', 1763933196222),
    ('Livre', 'Octobre Rouge', 1986, 'A. Michel', 'Fiction', 'Tom Clancy', '9782226025913', 485, NULL, NULL, NULL, NULL, NULL, 'covers/af09d581-ab81-459c-8f55-1d8185e990d3.jpg', 'image/jpeg', 1763933265065),
    ('Livre', 'La dernière énigme', 2014, 'Editions du Masque', 'Fiction', 'Agatha Christie', '9782702440926', 290, NULL, NULL, NULL, NULL, NULL, 'covers/493169d5-4aa8-44b4-a1af-c4e960031ac6.jpg', 'image/jpeg', 1763933303315),
    ('Livre', 'No Logo', 2007, 'Editions 84', 'Brand name products', 'Naomi Klein', '9782290003527', 200, NULL, NULL, NULL, NULL, NULL, 'covers/5fe2ca46-5058-48da-990e-6986e374dbb5.jpg', 'image/jpeg', 1763933361062),
    ('Livre', 'Computer networks', 2011, 'Pearson Education France', 'Computer networks', 'Andrew S. Tanenbaum', '9782744075216', 950, NULL, NULL, NULL, NULL, NULL, 'covers/36435d83-3080-4126-8e8b-8084d70604d2.jpg', 'image/jpeg', 1763933383676),
    ('Livre', 'Pas de lettre pour le colonel', 1980, 'Bernard Grasset', 'Fiction', 'Gabriel García Márquez', '9782246252313', 125, NULL, NULL, NULL, NULL, NULL, 'covers/ee30b918-6c34-4a66-9336-0a804f816881.jpg', 'image/jpeg', 1763933435600),
    ('Livre', 'Skellig', 2000, 'Flammarion', 'Strangers', 'David Almond', '9782081645813', 266, NULL, NULL, NULL, NULL, NULL, 'covers/054f753d-9052-496c-aca3-da6b969afbb5.jpg', 'image/jpeg', 1763933463401),
    ('Livre', 'Dracula', 2005, 'Ecole des Loisirs', 'English literature', 'Bram Stoker', '9782211074384', 221, NULL, NULL, NULL, NULL, NULL, 'covers/57840659-03b2-4981-aa47-6c2fc9e4719d.jpg', 'image/jpeg', 1763933489192),
    ('Livre', 'Livre de La Jungle / The Jungle Book', 1987, 'Schoenhof Foreign Books Inc', 'Jungles', 'Rudyard Kipling', '9782070334568', 200, NULL, NULL, NULL, NULL, NULL, 'covers/9e9c4e80-a2d4-4456-82f2-e1d02868193a.jpg', 'image/jpeg', 1763933813442),
    ('Livre', 'The Pelican Brief', 1994, 'France Loisirs', 'White House Chief of Staff', 'John Grisham', '9782724278859', 200, NULL, NULL, NULL, NULL, NULL, 'covers/b2f8b5e0-24aa-4e56-bad3-4a8acf44e711.jpg', 'image/jpeg', 1763933926638),
    ('Livre', 'La dernière récolte', 2002, 'Le Grand Livre du Mois', 'Boys', 'John Grisham', '9782702875636', 200, NULL, NULL, NULL, NULL, NULL, 'covers/4fb4faab-7c49-4b4b-8918-f8983aa72b18.jpg', 'image/jpeg', 1763933966708),
    ('Livre', 'Pour toujours', 1998, 'Medium', 'homosexuality', 'Judy Blume', '9782211098106', 209, NULL, NULL, NULL, NULL, NULL, 'covers/7343cedc-618d-4ce0-9872-921aa96b1f50.jpg', 'image/jpeg', 1763934068153),
    ('Livre', 'Le Petit Prince', 2007, 'Editions Gallimard', 'adventure', 'Antoine de Saint-Exupéry', '9782070612758', 120, NULL, NULL, NULL, NULL, NULL, 'covers/7d96e4b8-553c-4dcd-b090-1551aa4c0c8a.jpg', 'image/jpeg', 1763934633425),
    ('Livre', 'L’étranger', 1984, 'Gallimard', 'Philosophical Novels', 'Albert Camus', '9782070360024', 185, NULL, NULL, NULL, NULL, NULL, 'covers/95e0c199-0855-4ed0-889c-ac8c55209bd5.jpg', 'image/jpeg', 1763934646918),
    ('Livre', 'La Peste', 2022, 'Gallimard', 'Fiction', 'Albert Camus', '9782070360420', 355, NULL, NULL, NULL, NULL, NULL, 'covers/8aec0dd4-ab33-4c0b-b609-4d3ee5bf4d49.jpg', 'image/jpeg', 1763934663074),
    ('Livre', 'The lord of the rings', 1991, 'Grafton', 'The Lord of the Rings', 'J.R.R. Tolkien', '9780261102385', 200, NULL, NULL, NULL, NULL, NULL, 'covers/2f50d6c2-bbec-425d-9eb2-58321a3503bd.jpg', 'image/jpeg', 1763934673273),
    ('Livre', 'The Two Towers', 1991, 'Grafton', 'Ents', 'J.R.R. Tolkien', '9780261102361', 447, NULL, NULL, NULL, NULL, NULL, 'covers/b659b3ff-dcd9-49d5-bdb1-2c94db7a8446.jpg', 'image/jpeg', 1763934684039),
    ('Livre', 'The Return of the King', 1991, 'Grafton', 'Elves', 'J.R.R. Tolkien', '9780261102378', 560, NULL, NULL, NULL, NULL, NULL, 'covers/f8f9ff60-72b4-4033-bd7e-1a513d4aa16e.jpg', 'image/jpeg', 1763934714659),
    ('Livre', 'Foundation', 1991, 'Spectra', 'Psychohistory', 'Isaac Asimov', '9780553293357', 320, NULL, NULL, NULL, NULL, NULL, 'covers/ac34b2ea-7a70-4a65-8331-2e3bf80cd6f0.jpg', 'image/jpeg', 1763934731116),
    ('Livre', 'Dune', 2020, 'Ace', 'Dune (Imaginary place)', 'Frank Herbert', '9780441172719', 896, NULL, NULL, NULL, NULL, NULL, 'covers/b12c7ca8-d7ef-4eac-ba37-589b4e3bd570.jpg', 'image/jpeg', 1763934743002),
    ('Livre', 'Sapiens', 2015, 'Vintage Books', 'Technology and civilization', 'Yuval Noah Harari', '9780099590088', 512, NULL, NULL, NULL, NULL, NULL, 'covers/903acd9a-f4fb-4375-af35-1f97c569493e.jpg', 'image/jpeg', 1763934814662),
    ('Livre', 'Homo Deus', 2017, 'Vintage', 'Civilization, modern, 21st century', 'Yuval Noah Harari', '9781784703936', 528, NULL, NULL, NULL, NULL, NULL, 'covers/1dc2eb6e-225b-45d9-a67a-ca641864393f.jpg', 'image/jpeg', 1763934823757),
    ('Magazine','Science & Vie',           2025,'Mondo','Science',          NULL,NULL,NULL,1280,'Mensuel','2025-01-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','National Geographic',     2025,'NatGeo','Découverte',      NULL,NULL,NULL,920, 'Mensuel','2025-02-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Le Monde Diplomatique',   2025,'Le Monde','Géopolitique',  NULL,NULL,NULL,780, 'Mensuel','2025-03-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Wired',                   2024,'Condé Nast','Technologie', NULL,NULL,NULL,540, 'Mensuel','2024-04-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Linux Magazine',          2024,'Diamond','Informatique',   NULL,NULL,NULL,250, 'Mensuel','2024-05-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Psychologies',            2024,'Psychologies','Bien-être', NULL,NULL,NULL,410, 'Mensuel','2024-06-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Esprit',                  2023,'Esprit','Culture',         NULL,NULL,NULL,500, 'Mensuel','2023-07-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Nature',                  2023,'Springer','Science',       NULL,NULL,NULL,670, 'Mensuel','2023-08-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','Pour la Science',         2022,'Belin','Science',          NULL,NULL,NULL,890, 'Mensuel','2022-09-01',NULL,NULL,NULL,NULL,NULL),
    ('Magazine','MIT Technology Review',   2022,'MIT','Technologie',        NULL,NULL,NULL,300, 'Mensuel','2022-10-01',NULL,NULL,NULL,NULL,NULL),
    ('CD','Kind of Blue',               1959,'Columbia','Jazz',        NULL,NULL,NULL,NULL,NULL,NULL,'Miles Davis',        46,NULL,NULL,NULL),
    ('CD','Abbey Road',                 1969,'Apple','Rock',           NULL,NULL,NULL,NULL,NULL,NULL,'The Beatles',        47,NULL,NULL,NULL),
    ('CD','Thriller',                   1982,'Epic','Pop',             NULL,NULL,NULL,NULL,NULL,NULL,'Michael Jackson',    42,NULL,NULL,NULL),
    ('CD','Random Access Memories',     2013,'Columbia','Electro',     NULL,NULL,NULL,NULL,NULL,NULL,'Daft Punk',          74,NULL,NULL,NULL),
    ('CD','OK Computer',                1997,'Parlophone','Rock',      NULL,NULL,NULL,NULL,NULL,NULL,'Radiohead',          53,NULL,NULL,NULL),
    ('CD','The Dark Side of the Moon',  1973,'Harvest','Rock',         NULL,NULL,NULL,NULL,NULL,NULL,'Pink Floyd',         43,NULL,NULL,NULL),
    ('CD','Back to Black',              2006,'Island','Soul',          NULL,NULL,NULL,NULL,NULL,NULL,'Amy Winehouse',      34,NULL,NULL,NULL),
    ('CD','Discovery',                  2001,'Virgin','Electro',       NULL,NULL,NULL,NULL,NULL,NULL,'Daft Punk',          60,NULL,NULL,NULL),
    ('CD','Blue Train',                 1958,'Blue Note','Jazz',       NULL,NULL,NULL,NULL,NULL,NULL,'John Coltrane',      42,NULL,NULL,NULL),
    ('CD','In Rainbows',                2007,'XL Recordings','Rock',   NULL,NULL,NULL,NULL,NULL,NULL,'Radiohead',          42,NULL,NULL,NULL);

-- =====================================================================
-- 3. EMPRUNT : DONNÉES DE SCÉNARIO DYNAMIQUE
-- Toutes les dates sont calculées par rapport à 'now' (exécution)
-- Format: ISO 8601 Strict
-- =====================================================================

-- SCENARIO A : Volume récent sur livres avec couvertures (pour le Top 5)
-- Livres ID 1-25 = Livres avec couverture
-- Emprunts "En cours" (Date retour effective = NULL)

-- Alice (ID 2) - Top Lectrice
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (21, 2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days', '+21 days'), NULL),
    (22, 2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days', '+21 days'), NULL),
    (1,  2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days', '+21 days'), NULL),
    (2,  2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days', '+21 days'), NULL),
    (3,  2, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'),   strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day', '+21 days'), NULL);

-- Bob (ID 3)
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (4,  3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-20 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-20 days', '+21 days'), NULL),
    (5,  3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-18 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-18 days', '+21 days'), NULL),
    (6,  3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-15 days', '+21 days'), NULL),
    (7,  3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-12 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-12 days', '+21 days'), NULL),
    (8,  3, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-10 days', '+21 days'), NULL);

-- Claire (ID 4) - Emprunts très récents (Top 5 candidats)
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (9,  4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'),  strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day', '+21 days'), NULL),
    (10, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-2 days', '+21 days'), NULL),
    (11, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-3 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-3 days', '+21 days'), NULL),
    (12, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-4 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-4 days', '+21 days'), NULL),
    (13, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days', '+21 days'), NULL);

-- David (ID 5)
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (14, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-25 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-25 days', '+21 days'), NULL), -- En retard
    (15, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-24 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-24 days', '+21 days'), NULL), -- En retard
    (16, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-23 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-23 days', '+21 days'), NULL),
    (17, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days'),  strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-5 days', '+21 days'), NULL),
    (18, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day'),   strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-1 day', '+21 days'), NULL);

-- SCENARIO B : Historique & Pénalités (Adhérents 7-11)
-- Emprunts vieux de 60-80 jours, rendus en retard ou pas rendus
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (39, 7,  strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-80 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-80 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days')),
    (40, 8,  strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-75 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-75 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-35 days')),
    (41, 9,  strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-30 days')),
    (42, 10, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-60 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-60 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-30 days')),
    (43, 11, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-85 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-85 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days'));

-- SCENARIO C : Cycle de vie (Livre 1 emprunté, rendu, ré-emprunté)
-- Livre 1 déjà emprunté par Alice (ID 2) à J-10 (voir plus haut).
-- Ajoutons un historique pour ce livre 1 par quelqu'un d'autre (Claire ID 4) il y a longtemps.
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (1, 4, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-70 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days'));

-- Livre 2 emprunté par David (ID 5) il y a 60 jours, rendu. (Ré-emprunté par Alice à J-15 plus haut)
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (2, 5, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-60 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-60 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days'));

-- Quelques emprunts "terminés" pour l'historique moyen terme (Adhérents 12-13)
INSERT INTO EMPRUNT (document_id, emprunteur_id, date_emprunt, date_retour_prevue, date_retour_effective) VALUES
    (19, 12, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-50 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-30 days')),
    (20, 13, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-45 days', '+21 days'), strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-25 days'));


-- =====================================================================
-- 4. PENALITE
-- =====================================================================
INSERT INTO PENALITE (adherent_id, emprunt_id, type, montant, date_debut, date_fin, description, status) VALUES
    (
        7, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 7 AND document_id = 39 LIMIT 1),
        'RETARD', 5.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-40 days'), NULL, 'Retard volume', 'ACTIVE'
    ),
    (
        8, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 8 AND document_id = 40 LIMIT 1),
        'RETARD', 7.50, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-35 days'), NULL, 'Retard volume', 'ACTIVE'
    ),
    (
        9, (SELECT emprunt_id FROM EMPRUNT WHERE emprunteur_id = 9 AND document_id = 41 LIMIT 1),
        'RETARD', 4.00, strftime('%Y-%m-%dT%H:%M:%fZ', 'now', '-30 days'), NULL, 'Retard volume', 'ACTIVE'
    );