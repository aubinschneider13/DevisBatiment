package insa.aubin.devisbatiment.modele;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class Batiment extends ElementDeConstruction {

    private String nomBatiment;
    private String typeBatiment;
    private int nbNiveaux;
    private ArrayList<Niveau> niveaux;

    public Batiment(String nomBatiment, String typeBatiment, int nbNiveaux) {
        super(typeBatiment);
        if (!typeBatiment.equals("Maison") && !typeBatiment.equals("Immeuble")) {
            throw new IllegalArgumentException("typeBatiment doit être 'Maison' ou 'Immeuble'");
        }
        this.nomBatiment = nomBatiment;
        this.typeBatiment = typeBatiment;
        this.nbNiveaux = nbNiveaux;
        this.niveaux = new ArrayList<>();
        creerDossier();
    }
    
    private void creerDossier() {
        String cheminRacine = "C:/Users/haykk/Desktop/DevisBatiment/data/Batiments";
        String cheminDossier = cheminRacine + "/" + this.nomBatiment;

        File dossier = new File(cheminDossier);
        dossier.mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(cheminRacine + "/batiments.txt", true))) {
            pw.println(this.getId() + ";" + this.nomBatiment + ";" + this.typeBatiment);
        } catch (IOException e) {
            System.err.println("Erreur écriture batiments.txt : " + e.getMessage());
        }
    }

    public String getNomBatiment() {
        return nomBatiment;
    }

    public void setNomBatiment(String nomBatiment) {
        this.nomBatiment = nomBatiment;
    }

    public String getTypeBatiment() {
        return typeBatiment;
    }

    public void setTypeBatiment(String typeBatiment) {
        this.typeBatiment = typeBatiment;
    }

    public int getNbNiveaux() {
        return nbNiveaux;
    }

    public void setNbNiveaux(int nbNiveaux) {
        this.nbNiveaux = nbNiveaux;
    }
    
    @Override
    public String toCSV() {
        return getId() + ";" + nomBatiment + ";" + typeBatiment + ";" + nbNiveaux;
    }

    @Override
    public String toString() {
        return typeBatiment + " [id=" + getId() + ", nom=" + nomBatiment + ", niveaux=" + nbNiveaux + "]";
    }
}
