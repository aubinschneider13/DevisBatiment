/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package insa.aubin.devisbatiment.Model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Batiment {
    private String nomBatiment;
    private String idBatiment;
    private String typeBatiment;
    private double nbNiveaux;
    private ArrayList<Niveau> niveaux = new ArrayList<>();
    
    public Batiment(String nomBatiment , String typeBatiment, double nbNiveaux){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.nomBatiment = nomBatiment;
        this.idBatiment = "Batiment" + formatter.format(new Date());
        this.typeBatiment = typeBatiment;
        this.nbNiveaux = nbNiveaux;
        this.niveaux = new ArrayList<>();
        
        creerDossier();
    }
    
    private void creerDossier() {
        String cheminRacine = "C:/Users/utilisateur/Documents/NetBeansProjects/DevisBatiment/data/Batiments";
        String cheminDossier = cheminRacine + "/" + this.nomBatiment;

        File dossier = new File(cheminDossier);
        dossier.mkdirs();
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(cheminRacine + "/batiments.txt", true))) {
            pw.println(this.idBatiment + ";" + this.nomBatiment + ";" + this.typeBatiment);
        } catch (IOException e) {
            System.err.println("Erreur écriture batiments.txt : " + e.getMessage());
        }
    }
    
    
}
