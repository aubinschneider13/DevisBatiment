package insa.aubin.devisbatiment.modele;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class GestionnaireSauvegarde {

    private boolean sauvegardeActive;
    private String cheminRacine;
    private static final String FICHIER_CONFIG = "config.properties";

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
                if (parts[0].equals("actif")) actif = Boolean.parseBoolean(parts[1]);
                if (parts[0].equals("chemin")) chemin = parts[1];
            }
            if (actif && !chemin.isEmpty()) activer(chemin);
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
        ecrireLigne(cheminParent + "/appartements.txt", a.toCSV());
    }

    public String getCheminAppartement(Appartement a, Niveau n, Batiment b) {
        return getCheminNiveau(n, b) + "/" + a.getId();
    }

    // ────────────────────────────────────────────
    // Piece
    // ────────────────────────────────────────────

    public void sauvegarderPiece(Piece p, Appartement a, Niveau n, Batiment b) {
        if (!sauvegardeActive) return;
        String cheminParent = getCheminAppartement(a, n, b);
        String cheminFichier = cheminParent + "/" + p.getId() + ".txt";
        ecrireLigne(cheminParent + "/pieces.txt", p.toCSV());
        for (Mur m : p.getMurs()) {
            ecrireLigne(cheminFichier, m.toCSV());
        }
        ecrireLigne(cheminFichier, p.getSol().toCSV());
        ecrireLigne(cheminFichier, p.getPlafond().toCSV());
    }
    
    // ────────────────────────────────────────────
    // Charger les données
    // ────────────────────────────────────────────
    
    public List<Immeuble> chargerTousBatiments() {
        List<Immeuble> result = new ArrayList<>();
        File fichier = new File(cheminRacine + "/batiments.txt");
        if (!fichier.exists()) return result;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 11 || !parts[0].equals("IMMEUBLE")) continue;

                // IMMEUBLE;id;nom;nbNiveaux;x1;y1;x2;y2;x3;y3;x4;y4
                String id  = parts[1];
                String nom = parts[2];
                Point p1 = new Point(Double.parseDouble(parts[4]),  Double.parseDouble(parts[5]));
                Point p2 = new Point(Double.parseDouble(parts[6]),  Double.parseDouble(parts[7]));
                Point p3 = new Point(Double.parseDouble(parts[8]),  Double.parseDouble(parts[9]));
                Point p4 = new Point(Double.parseDouble(parts[10]), Double.parseDouble(parts[11]));

                AireImmeuble aire = new AireImmeuble(p1);
                aire.setP2(p2);
                aire.setP3(p3);
                aire.valider();

                Immeuble immeuble = new Immeuble(nom, aire);
                immeuble.setId(id);

                // Charger les niveaux
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
                if (parts.length < 4 || !parts[0].equals("NIVEAU")) continue;

                // NIVEAU;id;nbAppartements;hauteurPlafond
                String id             = parts[1];
                double hauteurPlafond = Double.parseDouble(parts[3]);

                Niveau niveau = immeuble.ajouterNiveau(hauteurPlafond);
                niveau.setId(id);

                // Charger les appartements de ce niveau
                chargerAppartements(niveau, immeuble, nomBatiment, id);
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

                String id             = parts[1];
                double hauteurPlafond = Double.parseDouble(parts[4]);

                // Reconstruire les murs depuis les points sauvegardés
                List<Point> pointsAppart = new ArrayList<>();
                for (int i = 6; i + 1 < parts.length; i += 2) {
                    pointsAppart.add(new Point(
                        Double.parseDouble(parts[i]),
                        Double.parseDouble(parts[i + 1])
                    ));
                }

                List<Mur> mursAppart = new ArrayList<>();
                for (int i = 0; i < pointsAppart.size(); i++) {
                    mursAppart.add(new Mur(
                        pointsAppart.get(i),
                        pointsAppart.get((i + 1) % pointsAppart.size())
                    ));
                }

                Appartement appart = niveau.ajouterAppartement(mursAppart);
                appart.setId(id);

                // Charger les pièces
                chargerPieces(appart, nomBatiment, idNiveau, id);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement appartements : " + e.getMessage());
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
                // PIECE;id;numero;surface;hauteurPlafond;x1;y1;x2;y2...
                if (parts.length < 7 || !parts[0].equals("PIECE")) continue;

                String id             = parts[1];
                double hauteurPlafond = Double.parseDouble(parts[4]);

                List<Point> pointsPiece = new ArrayList<>();
                for (int i = 5; i + 1 < parts.length; i += 2) {
                    pointsPiece.add(new Point(
                        Double.parseDouble(parts[i]),
                        Double.parseDouble(parts[i + 1])
                    ));
                }

                List<Mur> mursPiece = new ArrayList<>();
                for (int i = 0; i < pointsPiece.size(); i++) {
                    mursPiece.add(new Mur(
                        pointsPiece.get(i),
                        pointsPiece.get((i + 1) % pointsPiece.size())
                    ));
                }

                Piece piece = appart.ajouterPiece(mursPiece);
                piece.setId(id);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement pièces : " + e.getMessage());
        }
    }
    
    // ────────────────────────────────────────────
    // Supprimer un bâtiment
    // ────────────────────────────────────────────

    public void supprimerBatiment(Immeuble immeuble) {
        if (!sauvegardeActive) return;

        // 1. Supprimer le dossier du bâtiment récursivement
        File dossier = new File(cheminRacine + "/" + immeuble.getNomBatiment());
        supprimerDossierRecursivement(dossier);

        // 2. Retirer la ligne correspondante dans batiments.txt
        File fichier = new File(cheminRacine + "/batiments.txt");
        if (!fichier.exists()) return;

        List<String> lignes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] parts = ligne.split(";");
                // On garde toutes les lignes sauf celle dont l'id correspond
                if (parts.length >= 2 && parts[1].equals(immeuble.getId())) continue;
                lignes.add(ligne);
            }
        } catch (IOException e) {
            System.err.println("Erreur lecture batiments.txt : " + e.getMessage());
            return;
        }

        // Réécrire le fichier sans la ligne supprimée
        try (PrintWriter pw = new PrintWriter(new FileWriter(fichier, false))) {
            for (String l : lignes) pw.println(l);
        } catch (IOException e) {
            System.err.println("Erreur réécriture batiments.txt : " + e.getMessage());
        }
    }

    private void supprimerDossierRecursivement(File f) {
        if (f.isDirectory()) {
            for (File enfant : f.listFiles()) supprimerDossierRecursivement(enfant);
        }
        f.delete();
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
}