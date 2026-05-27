package insa.aubin.devisbatiment.modele;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GestionnaireSauvegarde {

    private boolean sauvegardeActive;
    private String cheminRacine;
    private static final String FICHIER_CONFIG = "config.properties";
    private CatalogueRevetements catalogue;

    public CatalogueRevetements getCatalogue() {
        if (this.catalogue == null) {
            this.catalogue = new CatalogueRevetements("src/main/resources/data/revetements.txt");
        }
        return this.catalogue;
    }

    public GestionnaireSauvegarde() {
        this.sauvegardeActive = false;
        this.cheminRacine = null;
    }

    // ────────────────────────────────────────────
    // Activation / Désactivation Sauvegarde
    // ────────────────────────────────────────────

    public void activer(String chemin) throws IllegalArgumentException {
        if (chemin == null || chemin.trim().isEmpty()) {
            throw new IllegalArgumentException("Le chemin ne peut pas être vide.");
        }

        File dossier = new File(chemin);
        if (!dossier.exists() && !dossier.mkdirs()) {
            throw new IllegalArgumentException("Impossible de créer le dossier : " + chemin);
        }

        if (!dossier.isDirectory()) {
            throw new IllegalArgumentException("Le chemin indiqué n'est pas un dossier : " + chemin);
        }

        if (!dossier.canWrite()) {
            throw new IllegalArgumentException("Le dossier n'est pas accessible en écriture : " + chemin);
        }

        this.cheminRacine = chemin.trim();
        this.sauvegardeActive = true;
        sauvegarderConfig();
    }

    public void desactiver() {
        this.sauvegardeActive = false;
        this.cheminRacine = null;
        sauvegarderConfig();
    }

    private void sauvegarderConfig() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FICHIER_CONFIG))) {
            pw.println("actif=" + sauvegardeActive);
            pw.println("chemin=" + (cheminRacine != null ? cheminRacine : ""));
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde config : " + e.getMessage());
        }
    }

    public void chargerConfig() {
        File f = new File(FICHIER_CONFIG);
        if (!f.exists()) return;

        try (java.util.Scanner sc = new java.util.Scanner(f)) {
            String chemin = "";
            boolean actif = false;

            while (sc.hasNextLine()) {
                String[] parts = sc.nextLine().split("=", 2);
                if (parts.length < 2) continue;

                if (parts[0].equals("actif")) {
                    actif = Boolean.parseBoolean(parts[1]);
                }

                if (parts[0].equals("chemin")) {
                    chemin = parts[1];
                }
            }

            if (actif && !chemin.isEmpty()) {
                activer(chemin);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement config : " + e.getMessage());
        }
    }

    public boolean isSauvegardeActive() {
        return sauvegardeActive;
    }

    public String getCheminRacine() {
        return cheminRacine;
    }

    // ────────────────────────────────────────────
    // Batiment
    // ────────────────────────────────────────────

    public void sauvegarderBatiment(Batiment b) {
        if (!sauvegardeActive) return;

        String cheminDossier = getCheminBatiment(b);
        new File(cheminDossier).mkdirs();
        ecrireLigne(cheminRacine + "/batiments.txt", b.toCSV());
    }

    public String getCheminBatiment(Batiment b) {
        return cheminRacine + "/" + b.getNomBatiment();
    }

    // ────────────────────────────────────────────
    // Niveau
    // ────────────────────────────────────────────

    public void sauvegarderNiveau(Niveau n, Batiment b) {
        if (!sauvegardeActive) return;

        String cheminParent = getCheminBatiment(b);
        String cheminDossier = getCheminNiveau(n, b);
        new File(cheminDossier).mkdirs();
        ecrireLigne(cheminParent + "/niveaux.txt", n.toCSV());
    }

    public String getCheminNiveau(Niveau n, Batiment b) {
        return getCheminBatiment(b) + "/" + n.getId();
    }

    // ────────────────────────────────────────────
    // Appartement
    // ────────────────────────────────────────────

    public void sauvegarderAppartement(Appartement a, Niveau n, Batiment b) {
        if (!sauvegardeActive) return;

        String cheminParent = getCheminNiveau(n, b);
        String cheminDossier = getCheminAppartement(a, n, b);
        new File(cheminDossier).mkdirs();
        sauvegarderListeAppartements(n, b);
        sauvegarderDetailsAppartement(a, n, b);
    }

    public String getCheminAppartement(Appartement a, Niveau n, Batiment b) {
        return getCheminNiveau(n, b) + "/" + a.getId();
    }

    // ────────────────────────────────────────────
    // Couloirs
    // ────────────────────────────────────────────

    public void sauvegarderCouloirs(Niveau n, Batiment b) {
        if (!sauvegardeActive) return;

        String cheminParent = getCheminNiveau(n, b);
        new File(cheminParent).mkdirs();

        List<String> lignes = new ArrayList<>();
        for (Couloir couloir : n.getCouloirs()) {
            lignes.add(couloir.toCSV());
        }

        ecrireLignes(cheminParent + "/couloirs.txt", lignes);
    }

    public void sauvegarderTremies(Niveau n, Batiment b) {
        if (!sauvegardeActive) return;

        String cheminParent = getCheminNiveau(n, b);
        new File(cheminParent).mkdirs();

        List<String> lignes = new ArrayList<>();
        for (Tremie tremie : n.getTremies()) {
            lignes.add(tremie.toCSV());
        }

        ecrireLignes(cheminParent + "/tremies.txt", lignes);
    }

    // ────────────────────────────────────────────
    // Piece
    // ────────────────────────────────────────────

    public void sauvegarderPiece(Piece p, Appartement a, Niveau n, Batiment b) {
        if (!sauvegardeActive) return;

        sauvegarderAppartementComplet(a, n, b);
    }

    public void sauvegarderDetailsAppartement(Appartement a, Niveau n, Batiment b) {
        if (!sauvegardeActive) return;

        String cheminDossier = getCheminAppartement(a, n, b);
        new File(cheminDossier).mkdirs();

        List<String> csvLines = new ArrayList<>();
        for (Mur m : a.getMurs()) {
            System.out.println("[DEBUG SAVE] Tentative d'écriture du mur: " + m.toString() + " (Instance: " + m.hashCode() + ")");
            String idG = (m.getCoteGauche().getRevetements() != null && !m.getCoteGauche().getRevetements().isEmpty()) ? m.getCoteGauche().getRevetements().get(0).getId() : "VIDE";
            String idD = (m.getCoteDroit().getRevetements() != null && !m.getCoteDroit().getRevetements().isEmpty()) ? m.getCoteDroit().getRevetements().get(0).getId() : "VIDE";
            System.out.println("  -> Données lues pour le fichier - Gauche: " + idG + " | Droit: " + idD);
            csvLines.add(m.toCSV());
        }

        ecrireLignes(cheminDossier + "/murs_appartement.txt", csvLines);
    }

    public void sauvegarderAppartementComplet(Appartement a, Niveau n, Batiment b) {
        if (!sauvegardeActive || a == null || n == null || b == null) return;

        sauvegarderListeAppartements(n, b);
        sauvegarderDetailsAppartement(a, n, b);

        String cheminDossier = getCheminAppartement(a, n, b);
        new File(cheminDossier).mkdirs();

        List<String> lignesPieces = new ArrayList<>();
        Set<String> fichiersPiecesActifs = new HashSet<>();
        for (Piece piece : a.getPieces()) {
            lignesPieces.add(piece.toCSV());
            fichiersPiecesActifs.add(piece.getId() + ".txt");
            sauvegarderDetailsPiece(piece, a, n, b);
        }

        ecrireLignes(cheminDossier + "/pieces.txt", lignesPieces);
        supprimerFichiersPiecesObsoletes(new File(cheminDossier), fichiersPiecesActifs);
    }

    public void sauvegarderListeAppartements(Niveau n, Batiment b) {
        if (!sauvegardeActive || n == null || b == null) return;

        String cheminParent = getCheminNiveau(n, b);
        new File(cheminParent).mkdirs();

        List<String> lignes = new ArrayList<>();
        for (Appartement appartement : n.getAppartements()) {
            lignes.add(appartement.toCSV());
        }
        ecrireLignes(cheminParent + "/appartements.txt", lignes);
    }

    public void rechargerOuverturesAppartement(Appartement a, Niveau n, Batiment b) {
        if (a == null || n == null || b == null || !sauvegardeActive) return;
        File fichier = new File(getCheminAppartement(a, n, b) + "/murs_appartement.txt");
        chargerOuverturesMurs(a.getMurs(), fichier);
        chargerRevetementsMurs(a.getMurs(), fichier);

        // Recharger également les détails de toutes les pièces (Sols & Plafonds)
        if (a.getPieces() != null) {
            for (Piece piece : a.getPieces()) {
                File fichierPiece = new File(getCheminAppartement(a, n, b) + "/" + piece.getId() + ".txt");
                chargerDetailsPiece(piece, fichierPiece);
            }
        }
    }

    public void chargerRevetementsMurs(List<Mur> murs, File fichier) {
        if (murs == null || fichier == null || !fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 9 || !parts[0].equals("MUR")) continue;

                Mur mur = trouverMurParCoordonnees(murs,
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4]),
                        Double.parseDouble(parts[5]));
                if (mur == null) continue;

                CatalogueRevetements cat = getCatalogue();
                if (parts.length > 7) {
                    String idsGauche = parts[7];
                    mur.getCoteGauche().getRevetements().clear();
                    if (!"VIDE".equals(idsGauche)) {
                        for (String id : idsGauche.split(",")) {
                            Revetement r = cat.rechercherParId(id.trim());
                            if (r != null) {
                                mur.getCoteGauche().getRevetements().add(r);
                            }
                        }
                    }
                }
                if (parts.length > 8) {
                    String idsDroit = parts[8];
                    mur.getCoteDroit().getRevetements().clear();
                    if (!"VIDE".equals(idsDroit)) {
                        for (String id : idsDroit.split(",")) {
                            Revetement r = cat.rechercherParId(id.trim());
                            if (r != null) {
                                mur.getCoteDroit().getRevetements().add(r);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement revetements : " + e.getMessage());
        }
    }

    public void chargerDetailsPiece(Piece p, File fichier) {
        if (p == null || fichier == null || !fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            CatalogueRevetements cat = getCatalogue();
            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;
                String[] parts = ligne.split(";");

                if (parts[0].equals("SOL")) {
                    p.getSol().getRevetements().clear();
                    for (int i = 2; i < parts.length - 1; i++) {
                        String id = parts[i];
                        Revetement r = cat.rechercherParId(id.trim());
                        if (r != null) {
                            p.getSol().getRevetements().add(r);
                        }
                    }
                } else if (parts[0].equals("PLAFOND")) {
                    p.getPlafond().getRevetements().clear();
                    for (int i = 2; i < parts.length - 1; i++) {
                        String id = parts[i];
                        Revetement r = cat.rechercherParId(id.trim());
                        if (r != null) {
                            p.getPlafond().getRevetements().add(r);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement détails pièce " + p.getId() + " : " + e.getMessage());
        }
    }

    public void sauvegarderDetailsPiece(Piece p, Appartement a, Niveau n, Batiment b) {
        if (!sauvegardeActive) return;

        String cheminParent = getCheminAppartement(a, n, b);
        new File(cheminParent).mkdirs();
        String cheminFichier = cheminParent + "/" + p.getId() + ".txt";
        List<String> lignes = new ArrayList<>();
        for (Mur m : p.getMurs()) {
            lignes.add(m.toCSV());
        }
        lignes.add(p.getSol().toCSV());
        lignes.add(p.getPlafond().toCSV());
        ecrireLignes(cheminFichier, lignes);
    }

    // ────────────────────────────────────────────
    // Charger les données
    // ────────────────────────────────────────────

    public List<Immeuble> chargerTousBatiments() {
        List<Immeuble> result = new ArrayList<>();

        if (cheminRacine == null || cheminRacine.trim().isEmpty()) {
            return result;
        }

        File fichier = new File(cheminRacine + "/batiments.txt");
        if (!fichier.exists()) return result;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                String[] parts = ligne.split(";");

                // IMMEUBLE;id;nom;nbNiveaux;x1;y1;x2;y2;x3;y3;x4;y4
                if (parts.length < 12 || !parts[0].equals("IMMEUBLE")) continue;

                String id = parts[1];
                String nom = parts[2];

                Point p1 = new Point(Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
                Point p2 = new Point(Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
                Point p3 = new Point(Double.parseDouble(parts[8]), Double.parseDouble(parts[9]));
                Point p4 = new Point(Double.parseDouble(parts[10]), Double.parseDouble(parts[11]));

                Appartement.resetCompteur();
                Piece.resetCompteur();

                AireImmeuble aire = new AireImmeuble(p1);
                aire.setP2(p2);
                aire.setP3(p3);
                aire.valider();

                Immeuble immeuble = new Immeuble(nom, aire);
                immeuble.setId(id);

                chargerNiveaux(immeuble, nom);

                result.add(immeuble);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement batiments : " + e.getMessage());
        }

        return result;
    }

    private void chargerNiveaux(Immeuble immeuble, String nomBatiment) {
        File fichier = new File(cheminRacine + "/" + nomBatiment + "/niveaux.txt");
        if (!fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                String[] parts = ligne.split(";");

                // NIVEAU;id;nbAppartements;hauteurPlafond
                if (parts.length < 4 || !parts[0].equals("NIVEAU")) continue;

                String id = parts[1];
                double hauteurPlafond = Double.parseDouble(parts[3]);

                Niveau niveau = immeuble.ajouterNiveau(hauteurPlafond);
                niveau.setId(id);

                chargerAppartements(niveau, immeuble, nomBatiment, id);
                chargerCouloirs(niveau, nomBatiment, id);
                chargerTremies(niveau, nomBatiment, id);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement niveaux : " + e.getMessage());
        }
    }

    private void chargerAppartements(Niveau niveau, Immeuble immeuble,
                                     String nomBatiment, String idNiveau) {
        File fichier = new File(cheminRacine + "/" + nomBatiment + "/" + idNiveau + "/appartements.txt");
        if (!fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                String[] parts = ligne.split(";");

                // APPARTEMENT;id;numero;nbPieces;hauteurPlafond;surface;x1;y1;x2;y2...
                if (parts.length < 7 || !parts[0].equals("APPARTEMENT")) continue;

                String id = parts[1];

                List<Point> pointsAppart = new ArrayList<>();
                for (int i = 6; i + 1 < parts.length; i += 2) {
                    pointsAppart.add(new Point(
                             Double.parseDouble(parts[i]),
                             Double.parseDouble(parts[i + 1])
                    ));
                }

                if (pointsAppart.size() < 3) continue;

                List<Mur> mursAppart = new ArrayList<>();
                for (int i = 0; i < pointsAppart.size(); i++) {
                    mursAppart.add(new Mur(
                            pointsAppart.get(i),
                            pointsAppart.get((i + 1) % pointsAppart.size())
                    ));
                }

                Appartement appart = niveau.ajouterAppartement(mursAppart);
                appart.setId(id);

                File fichierMurs = new File(cheminRacine + "/" + nomBatiment + "/" + idNiveau
                                + "/" + id + "/murs_appartement.txt");
                chargerOuverturesMurs(appart.getMursDelimiteurs(), fichierMurs);
                chargerRevetementsMurs(appart.getMursDelimiteurs(), fichierMurs);

                chargerPieces(appart, nomBatiment, idNiveau, id);

                chargerOuverturesMurs(appart.getMurs(), fichierMurs);
                chargerRevetementsMurs(appart.getMurs(), fichierMurs);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement appartements : " + e.getMessage());
        }
    }

    private void chargerCouloirs(Niveau niveau, String nomBatiment, String idNiveau) {
        File fichier = new File(cheminRacine + "/" + nomBatiment + "/" + idNiveau + "/couloirs.txt");
        if (!fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                String[] parts = ligne.split(";");

                // COULOIR;id;numero;hauteurPlafond;nbZones;nbPointsZone1;x;y;x;y...
                if (parts.length < 6 || !parts[0].equals("COULOIR")) continue;

                String id = parts[1];
                double hauteurPlafond = Double.parseDouble(parts[3]);
                int nbZones = Integer.parseInt(parts[4]);
                int index = 5;

                Couloir couloir = new Couloir(hauteurPlafond);
                couloir.setId(id);

                for (int z = 0; z < nbZones && index < parts.length; z++) {
                    int nbPoints = Integer.parseInt(parts[index++]);
                    List<Point> points = new ArrayList<>();

                    for (int i = 0; i < nbPoints && index + 1 < parts.length; i++) {
                        points.add(new Point(
                                Double.parseDouble(parts[index++]),
                                Double.parseDouble(parts[index++])
                        ));
                    }

                    if (points.size() >= 3) {
                        List<Mur> mursZone = new ArrayList<>();
                        for (int i = 0; i < points.size(); i++) {
                            mursZone.add(new Mur(
                                    points.get(i),
                                    points.get((i + 1) % points.size()),
                                    hauteurPlafond
                            ));
                        }
                        couloir.ajouterZone(mursZone);
                    }
                }

                if (!couloir.getPolygones().isEmpty()) {
                    niveau.ajouterCouloir(couloir);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement couloirs : " + e.getMessage());
        }
    }

    private void chargerTremies(Niveau niveau, String nomBatiment, String idNiveau) {
        File fichier = new File(cheminRacine + "/" + nomBatiment + "/" + idNiveau + "/tremies.txt");
        if (!fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                String[] parts = ligne.split(";");

                // TREMIE;id;type;x;y;cote
                if (parts.length < 6 || !parts[0].equals("TREMIE")) continue;

                String id = parts[1];
                String type = parts[2];
                Point centre = new Point(
                        Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4])
                );

                Tremie tremie;
                if ("ESCALIER".equals(type)) {
                    tremie = new Escalier(centre);
                } else if ("ASCENSEUR".equals(type)) {
                    tremie = new Ascenseur(centre);
                } else {
                    continue;
                }
                tremie.setId(id);
                niveau.ajouterTremie(tremie);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement tremies : " + e.getMessage());
        }
    }

    private void chargerPieces(Appartement appart, String nomBatiment,
                               String idNiveau, String idAppart) {
        File fichier = new File(cheminRacine + "/" + nomBatiment + "/"
                + idNiveau + "/" + idAppart + "/pieces.txt");
        if (!fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                String[] parts = ligne.split(";");

                // PIECE;id;numero;surface;hauteurPlafond;usage;x1;y1;x2;y2...
                if (parts.length < 7 || !parts[0].equals("PIECE")) continue;

                String id = parts[1];
                String usage = "";
                int indexPremierPoint = 5;
                if (!estDouble(parts[5])) {
                    usage = parts[5];
                    indexPremierPoint = 6;
                }

                List<Point> pointsPiece = new ArrayList<>();
                for (int i = indexPremierPoint; i + 1 < parts.length; i += 2) {
                    pointsPiece.add(new Point(
                            Double.parseDouble(parts[i]),
                            Double.parseDouble(parts[i + 1])
                    ));
                }

                if (pointsPiece.size() < 3) continue;

                List<Mur> mursPiece = new ArrayList<>();
                for (int i = 0; i < pointsPiece.size(); i++) {
                    Mur murCsv = new Mur(
                            pointsPiece.get(i),
                            pointsPiece.get((i + 1) % pointsPiece.size())
                    );
                    Mur murExistant = trouverMurParCoordonnees(appart.getMurs(),
                            murCsv.getPoint1().getX(),
                            murCsv.getPoint1().getY(),
                            murCsv.getPoint2().getX(),
                            murCsv.getPoint2().getY());
                    mursPiece.add(murExistant != null ? murExistant : murCsv);
                }

                Piece piece = appart.ajouterPiece(mursPiece);
                piece.setId(id);
                piece.setUsage(usage);
                File fichierPiece = new File(cheminRacine + "/" + nomBatiment + "/"
                        + idNiveau + "/" + idAppart + "/" + piece.getId() + ".txt");
                chargerDetailsPiece(piece, fichierPiece);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement pièces : " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────
    // Supprimer un bâtiment
    // ────────────────────────────────────────────

    private boolean estDouble(String valeur) {
        if (valeur == null || valeur.isBlank()) return false;
        try {
            Double.parseDouble(valeur);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void chargerOuverturesMurs(List<Mur> murs, File fichier) {
        if (murs == null || fichier == null || !fichier.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 9 || !parts[0].equals("MUR")) continue;

                Mur mur = trouverMurParCoordonnees(murs,
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4]),
                        Double.parseDouble(parts[5]));
                if (mur == null) continue;

                int indexOuvertures = -1;
                for (int i = 9; i < parts.length; i++) {
                    if ("OUVERTURES".equals(parts[i])) {
                        indexOuvertures = i;
                        break;
                    }
                }
                if (indexOuvertures < 0 || indexOuvertures + 1 >= parts.length) continue;

                List<Ouverture> ouvertures = new ArrayList<>();
                int nbOuvertures = Integer.parseInt(parts[indexOuvertures + 1]);
                int index = indexOuvertures + 2;
                for (int i = 0; i < nbOuvertures && index + 2 < parts.length; i++) {
                    String type = parts[index++];
                    double position = Double.parseDouble(parts[index++]);
                    boolean inversee = "1".equals(parts[index++]);
                    if ("PORTE".equals(type)) {
                        Porte porte = new Porte(position);
                        porte.setOrientation(inversee ? 1 : -1);
                        ouvertures.add(porte);
                    } else if ("FENETRE".equals(type)) {
                        ouvertures.add(new Fenetre(position));
                    }
                }
                mur.setListeOuvertures(ouvertures);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement ouvertures : " + e.getMessage());
        }
    }

    private Mur trouverMurParCoordonnees(List<Mur> murs, double x1, double y1, double x2, double y2) {
        double tol = 0.02;
        for (Mur mur : murs) {
            boolean sensDirect =
                    Math.abs(mur.getPoint1().getX() - x1) < tol &&
                    Math.abs(mur.getPoint1().getY() - y1) < tol &&
                    Math.abs(mur.getPoint2().getX() - x2) < tol &&
                    Math.abs(mur.getPoint2().getY() - y2) < tol;
            boolean sensInverse =
                    Math.abs(mur.getPoint1().getX() - x2) < tol &&
                    Math.abs(mur.getPoint1().getY() - y2) < tol &&
                    Math.abs(mur.getPoint2().getX() - x1) < tol &&
                    Math.abs(mur.getPoint2().getY() - y1) < tol;
            if (sensDirect || sensInverse) {
                return mur;
            }
        }
        return null;
    }

    public void supprimerBatiment(Immeuble immeuble) {
        if (!sauvegardeActive) return;

        File dossier = new File(cheminRacine + "/" + immeuble.getNomBatiment());
        supprimerDossierRecursivement(dossier);

        File fichier = new File(cheminRacine + "/batiments.txt");
        if (!fichier.exists()) return;

        List<String> lignes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = br.readLine()) != null) {
                String[] parts = ligne.split(";");

                // On garde toutes les lignes sauf celle dont l'id correspond.
                if (parts.length >= 2 && parts[1].equals(immeuble.getId())) {
                    continue;
                }

                lignes.add(ligne);
            }
        } catch (IOException e) {
            System.err.println("Erreur lecture batiments.txt : " + e.getMessage());
            return;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(fichier, false))) {
            for (String ligne : lignes) {
                pw.println(ligne);
            }
        } catch (IOException e) {
            System.err.println("Erreur réécriture batiments.txt : " + e.getMessage());
        }
    }

    public void supprimerAppartement(Appartement appartement, Niveau niveau, Batiment batiment) {
        if (!sauvegardeActive || appartement == null || niveau == null || batiment == null) return;

        supprimerDossierRecursivement(new File(getCheminAppartement(appartement, niveau, batiment)));
        sauvegarderListeAppartements(niveau, batiment);
        sauvegarderCouloirs(niveau, batiment);
    }

    private void supprimerDossierRecursivement(File f) {
        if (f == null || !f.exists()) return;

        if (f.isDirectory()) {
            File[] enfants = f.listFiles();
            if (enfants != null) {
                for (File enfant : enfants) {
                    supprimerDossierRecursivement(enfant);
                }
            }
        }

        f.delete();
    }

    private void supprimerFichiersPiecesObsoletes(File dossierAppartement, Set<String> fichiersPiecesActifs) {
        if (dossierAppartement == null || !dossierAppartement.isDirectory()) return;

        File[] fichiers = dossierAppartement.listFiles();
        if (fichiers == null) return;

        for (File fichier : fichiers) {
            if (!fichier.isFile()) continue;

            String nom = fichier.getName();
            if (!nom.endsWith(".txt")) continue;
            if ("pieces.txt".equals(nom) || "murs_appartement.txt".equals(nom)) continue;

            if (!fichiersPiecesActifs.contains(nom)) {
                fichier.delete();
            }
        }
    }

    // ────────────────────────────────────────────
    // Utilitaire
    // ────────────────────────────────────────────

    private void ecrireLigne(String cheminFichier, String contenu) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(cheminFichier, true))) {
            pw.println(contenu);
        } catch (IOException e) {
            System.err.println("Erreur écriture " + cheminFichier + " : " + e.getMessage());
        }
    }

    private void ecrireLignes(String cheminFichier, List<String> contenus) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(cheminFichier, false))) {
            for (String contenu : contenus) {
                pw.println(contenu);
            }
        } catch (IOException e) {
            System.err.println("Erreur écriture " + cheminFichier + " : " + e.getMessage());
        }
    }
}
