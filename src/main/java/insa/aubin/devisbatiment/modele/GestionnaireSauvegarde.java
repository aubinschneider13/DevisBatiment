package insa.aubin.devisbatiment.modele;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GestionnaireSauvegarde {

    private boolean sauvegardeActive;
    private String cheminRacine;

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
    }

    public void desactiver() {
        this.sauvegardeActive = false;
        this.cheminRacine = null;
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