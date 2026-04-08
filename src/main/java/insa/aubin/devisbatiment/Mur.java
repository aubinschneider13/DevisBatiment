/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package insa.aubin.devisbatiment;

/*@author Gabriel The Rizzler*/
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

public class Mur {
    
    private String idMur;
    
    public Mur(){
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        
        this.idMur = "Mur" + formatter.format(new Date());
    }
}
