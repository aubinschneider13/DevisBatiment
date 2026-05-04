package insa.aubin.devisbatiment.modele;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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