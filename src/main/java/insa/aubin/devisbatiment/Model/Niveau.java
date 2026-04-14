/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package insa.aubin.devisbatiment.Model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Niveau {
    private String idNiveau;
    private double nbAppartements;
    private double hauteurPlafond;
    ArrayList<Appartement> appartements = new ArrayList<>();
     
    public Niveau(double nbApparements, float hauteurPlafond){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.idNiveau = "Niveau" + formatter.format(new Date());
        this.nbAppartements = nbAppartements;
        this.hauteurPlafond = hauteurPlafond;
        this.appartements = new ArrayList<>();
    }
}
