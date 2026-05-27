package insa.aubin.devisbatiment.modele;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    // LECTURE DU FICHIER
    // =========================================================================

    /**
     * Charge les revêtements depuis le fichier texte.
     * Format attendu : idRevetement;designation;pourMur(0/1);pourSol(0/1);pourPlafond(0/1);prixUnitaire
     */
    public void chargerDepuisFichier() {
        File fichier = new File(cheminFichier);
        if (!fichier.exists()) {
            System.out.println("Le fichier catalogue " + cheminFichier + " n'existe pas.");
            return;
        }

        listeRevetements.clear();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fichier), StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;

                // IMPORTANT : Le -1 force Java à garder les colonnes vides à la fin !
                String[] tokens = ligne.split(";", -1);

                // On s'assure qu'on a bien nos 12 colonnes et qu'on ignore l'en-tête
                if (tokens.length >= 12 && !tokens[0].equalsIgnoreCase("Classe")) {
                    try {
                        String classeRevetement = tokens[0].trim().toLowerCase();
                        String id = tokens[1].trim();
                        String designation = tokens[2].trim();

                        boolean pourMur = tokens[3].trim().equals("1");
                        boolean pourSol = tokens[4].trim().equals("1");
                        boolean pourPlafond = tokens[5].trim().equals("1");

                        double prix = Double.parseDouble(tokens[6].trim().replace(",", "."));

                        // Extraction des attributs spécifiques (colonnes 7 à 11)
                        String couleur     = tokens[7].trim();
                        String finition    = tokens[8].trim();
                        String dimension   = tokens[9].trim();
                        String essenceBois = tokens[10].trim();
                        String matiere     = tokens[11].trim();

                        Revetement r;

                        // C'EST ICI QUE LA MAGIE OPÈRE : on crée le bon objet selon le mot de la colonne 0
                        switch (classeRevetement) {
                            case "peinture":
                                r = new Peinture(id, designation, pourMur, pourSol, pourPlafond, prix, couleur, finition);
                                break;
                            case "carrelage":
                                r = new Carrelage(id, designation, pourMur, pourSol, pourPlafond, prix, dimension, matiere);
                                break;
                            case "parquet":
                                r = new Parquet(id, designation, pourMur, pourSol, pourPlafond, prix, essenceBois, finition);
                                break;
                            case "isolant":
                                Isolant.TypeIsolant typeIso = Isolant.TypeIsolant.fromString(couleur);
                                double epais = Double.parseDouble(finition.isEmpty() ? "0.0" : finition.replace(",", "."));
                                r = new Isolant(id, designation, pourMur, pourSol, pourPlafond, prix, typeIso, epais);
                                break;
                            default:
                                // Repli de sécurité pour un revêtement générique
                                r = new Revetement(id, designation, pourMur, pourSol, pourPlafond, prix);
                                break;
                        }

                        listeRevetements.add(r);

                    } catch (NumberFormatException e) {
                        System.err.println("Erreur de format (prix) sur la ligne : " + ligne);
                    }
                }
            }
            System.out.println(listeRevetements.size() + " matériaux chargés avec succès.");
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
    // --- NOUVEAU : Sauvegarde propre qui réécrit le fichier ---
    public void ajouterEtSauvegarderRevetement(Revetement r) {
        if (r == null) return;
        listeRevetements.add(r);

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cheminFichier, false), StandardCharsets.UTF_8))) {
            // On écrit l'en-tête en premier
            pw.println("Classe;idRevetement;designation;pourMur;pourSol;pourPlafond;prixUnitaire;couleur;finition;dimension;essenceBois;matiere");
            // On réécrit tous les matériaux
            for (Revetement rev : listeRevetements) {
                pw.println(rev.toCSV());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    // --- NOUVEAU : Générateur d'ID formaté "REVxxx" ---
    public String genererNouvelId() {
        int maxId = 0;
        for (Revetement r : listeRevetements) {
            try {
                // On enlève "REV" pour ne garder que le chiffre
                String idStr = r.getId().toUpperCase().replace("REV", "");
                int idNum = Integer.parseInt(idStr);
                if (idNum > maxId) {
                    maxId = idNum;
                }
            } catch (Exception e) {
                // Ignore si un ID ne respecte pas le format
            }
        }
        // Retourne REV suivi de 3 chiffres (ex: REV008)
        return String.format("REV%03d", maxId + 1);
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
