/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package insa.aubin.devisbatiment;

/**
 *
 * @author sxbsp
 */
public class DevisBatiment {

    public static void main(String[] args) {
        System.out.println("Hello World ");
        Mur m1 = new Mur("mur1", 1, 1, 2, 3, 7);
        System.out.println("Longueur du mur : " + m1.calculerLongueur());

        Ouverture f1 = new Fenetre("fenetre 1");
        System.out.println("Hauteur de la fenetre 1 : " + f1.getHauteur());
        Ouverture f2 = new Fenetre("fenetre 2");
        Ouverture f3 = new Fenetre("fenetre 3");

        m1.ajouterOuverture(f1);
        m1.ajouterOuverture(f2);
        m1.ajouterOuverture(f3);

        System.out.println("Le nombre d'ouvertures du mur 1 : " + m1.getListeOuvertures().size());

       
    }
}
