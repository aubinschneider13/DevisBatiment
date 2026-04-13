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
        Mur m1 = new Mur("mur1", 1, 1, 2, 3);
        System.out.println("Longueur du mur : " + m1.calculerLongueur());
    }
}
