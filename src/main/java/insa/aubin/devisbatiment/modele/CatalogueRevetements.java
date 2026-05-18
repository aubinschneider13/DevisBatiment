package insa.aubin.devisbatiment.modele;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CatalogueRevetements {

    // ObservableList est parfait pour JavaFX : si on ajoute un élément, l'interface se met à jour toute seule
    private ObservableList<Revetement> listeRevetements;
    private String cheminFichier;

    public CatalogueRevetements(String cheminFichier) {
        this.cheminFichier = cheminFichier;
        this.listeRevetements = FXCollections.observableArrayList();
        chargerDepuisFichier();
    }

    // =========================================================================
    // LECTURE DU FICHIER (Format des profs)
    // =========================================================================

    /**
     * Charge les revêtements depuis le fichier texte.
     * Format attendu : idRevetement;designation;pourMur(0/1);pourSol(0/1);pourPlafond(0/1);prixUnitaire
     */
    public void chargerDepuisFichier() {
        File fichier = new File(cheminFichier);
        if (!fichier.exists()) {
            System.out.println("Le fichier catalogue " + cheminFichier + " n'existe pas encore.");
            return;
        }

        listeRevetements.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                // On ignore les lignes vides
                if (ligne.trim().isEmpty()) continue;

                String[] tokens = ligne.split(";");

                // On vérifie qu'on a bien les 6 colonnes et on ignore la ligne d'en-tête
                if (tokens.length >= 6 && !tokens[0].equalsIgnoreCase("idRevetement")) {
                    try {
                        String id = tokens[0].trim();
                        String designation = tokens[1].trim();

                        // Conversion des "1" et "0" en booléens
                        boolean pourMur = tokens[2].trim().equals("1");
                        boolean pourSol = tokens[3].trim().equals("1");
                        boolean pourPlafond = tokens[4].trim().equals("1");

                        double prix = Double.parseDouble(tokens[5].trim());

                        // Création et ajout à la liste
                        Revetement r = new Revetement(id, designation, pourMur, pourSol, pourPlafond, prix);
                        listeRevetements.add(r);

                    } catch (NumberFormatException e) {
                        System.err.println("Erreur de format de nombre sur la ligne : " + ligne);
                    }
                }
            }
            System.out.println(listeRevetements.size() + " revêtements chargés avec succès.");
        } catch (IOException e) {
            System.err.println("Erreur de lecture du catalogue : " + e.getMessage());
        }
    }

    // =========================================================================
    // SAUVEGARDE ET AJOUT
    // =========================================================================

    /**
     * Ajoute un nouveau revêtement à la liste en mémoire et l'écrit à la fin du fichier.
     */
    public void ajouterEtSauvegarderRevetement(Revetement r) {
        if (r == null) return;

        // 1. Ajout en mémoire (mettra à jour l'interface JavaFX)
        listeRevetements.add(r);

        // 2. Écriture physique dans le fichier (mode "append" = true pour ajouter à la fin)
        try (PrintWriter pw = new PrintWriter(new FileWriter(cheminFichier, true))) {
            pw.println(r.toCSV());
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde du revêtement : " + e.getMessage());
        }
    }

    // =========================================================================
    // GETTERS & FILTRES POUR L'INTERFACE GRAPHIQUE (JavaFX)
    // =========================================================================

    /**
     * Retourne la liste complète (observable pour les ListView/ComboBox JavaFX)
     */
    public ObservableList<Revetement> getListeRevetements() {
        return listeRevetements;
    }

    /**
     * Retourne uniquement les revêtements applicables sur un MUR
     */
    public ObservableList<Revetement> getRevetementsPourMur() {
        return listeRevetements.filtered(Revetement::isPourMur);
    }

    /**
     * Retourne uniquement les revêtements applicables sur un SOL
     */
    public ObservableList<Revetement> getRevetementsPourSol() {
        return listeRevetements.filtered(Revetement::isPourSol);
    }

    /**
     * Retourne uniquement les revêtements applicables sur un PLAFOND
     */
    public ObservableList<Revetement> getRevetementsPourPlafond() {
        return listeRevetements.filtered(Revetement::isPourPlafond);
    }

    /**
     * Recherche un revêtement par son ID
     */
    public Revetement rechercherParId(String id) {
        for (Revetement r : listeRevetements) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }
}