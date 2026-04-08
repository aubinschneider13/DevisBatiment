/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package insa.aubin.devisbatiment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

public class Batiment {
    private String idBatiment;
    private String typeBatiment;
    private double nbNiveaux;
    private ArrayList<Niveau> niveaux = new ArrayList<>();
    
    public Batiment(double nbNiveaux){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.idBatiment = "Batiment" + formatter.format(new Date());
        this.nbNiveaux = nbNiveaux;
        this.niveaux = new ArrayList<>();
        
        creerDossier();
    }
    
    private void creerDossier() {
        String chemin = "./" + idBatiment;

        File dossier = new File("C:\\Users\\utilisateur\\Documents\\NetBeansProjects\\DevisBatiment\\src\\main\\java\\insa\\aubin\\sauvegarde");


        if (!dossier.exists()) {
            boolean created = dossier.mkdir();
            if (created) {
                System.out.println("Dossier créé : " + chemin);
            } else {
                System.out.println("Erreur : impossible de créer le dossier.");
            }
        }
    }
    
    
}
