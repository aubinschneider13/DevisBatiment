package insa.aubin.devisbatiment.modele;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class DevisExporter {

    public static String genererTexteDevis(Immeuble immeuble) {
        if (immeuble == null) return "Aucun immeuble sélectionné.";

        StringBuilder sb = new StringBuilder();
        String line = "================================================================================\n";
        String doubleLine = "================================================================================\n";
        
        sb.append(doubleLine);
        sb.append("                  DEVIS ESTIMATIF DE BATIMENT : ").append(immeuble.getNomBatiment().toUpperCase()).append("\n");
        sb.append(doubleLine);
        sb.append(String.format("Bâtiment Type : %s\n", immeuble.getTypeBatiment()));
        sb.append(String.format("Nombre de niveaux : %d\n", immeuble.getNbNiveaux()));
        sb.append(String.format(Locale.US, "Prix global estimé : %,.2f €\n", immeuble.calculerDevisTotal()));
        sb.append(doubleLine).append("\n");

        if (immeuble.getNiveaux() == null || immeuble.getNiveaux().isEmpty()) {
            sb.append("Aucun niveau défini dans ce bâtiment.\n");
            return sb.toString();
        }

        int indexNiv = 0;
        for (Niveau niveau : immeuble.getNiveaux()) {
            sb.append(String.format("NIVEAU %d\n", indexNiv));
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append(String.format("  Hauteur sous plafond : %.2f m\n", niveau.getHauteurPlafond()));
            sb.append(String.format("  Nombre d'appartements : %d\n", niveau.getNbAppartements()));
            sb.append(String.format(Locale.US, "  Sous-total niveau : %,.2f €\n", niveau.calculerDevis()));
            sb.append("--------------------------------------------------------------------------------\n\n");

            if (niveau.getAppartements() == null || niveau.getAppartements().isEmpty()) {
                sb.append("  Aucun appartement dessiné à ce niveau.\n\n");
                indexNiv++;
                continue;
            }

            for (Appartement appart : niveau.getAppartements()) {
                sb.append(String.format("  * %s\n", appart.toString()));
                sb.append(String.format(Locale.US, "    Surface habitable au sol : %.2f m²\n", appart.calculerSurface()));
                sb.append(String.format("    Nombre de pièces : %d\n", appart.getNbPieces()));
                sb.append(String.format(Locale.US, "    Sous-total appartement : %,.2f €\n", appart.calculerDevis()));
                sb.append("    ............................................................................\n");

                if (appart.getPieces() == null || appart.getPieces().isEmpty()) {
                    sb.append("    Aucune pièce définie dans cet appartement.\n\n");
                    continue;
                }

                for (Piece piece : appart.getPieces()) {
                    sb.append(String.format("    - %s\n", piece.toString()));
                    sb.append(String.format(Locale.US, "      Surface au sol : %.2f m²\n", piece.calculerSurfaceTotale()));
                    
                    // Flooring (Sol)
                    Sol sol = piece.getSol();
                    if (sol != null) {
                        sb.append("      > Sol : ");
                        if (sol.getRevetements() == null || sol.getRevetements().isEmpty()) {
                            sb.append("Sans revêtement (0,00 €)\n");
                        } else {
                            for (Revetement r : sol.getRevetements()) {
                                sb.append(String.format(Locale.US, "%s (%.2f €/m²) - %.2f €\n", 
                                    r.getDesignation(), r.getPrixUnitaire(), sol.calculerPrixRevetement()));
                            }
                        }
                    }

                    // Ceiling (Plafond)
                    Plafond plafond = piece.getPlafond();
                    if (plafond != null) {
                        sb.append("      > Plafond : ");
                        if (plafond.getRevetements() == null || plafond.getRevetements().isEmpty()) {
                            sb.append("Sans revêtement (0,00 €)\n");
                        } else {
                            for (Revetement r : plafond.getRevetements()) {
                                sb.append(String.format(Locale.US, "%s (%.2f €/m²) - %.2f €\n", 
                                    r.getDesignation(), r.getPrixUnitaire(), plafond.calculerPrixRevetement()));
                            }
                        }
                    }

                    // Walls (Murs)
                    if (piece.getMurs() != null && !piece.getMurs().isEmpty()) {
                        sb.append("      > Murs de la pièce :\n");
                        int countMur = 1;
                        for (Mur mur : piece.getMurs()) {
                            double netArea = mur.calculerSurfaceNette();
                            sb.append(String.format(Locale.US, "        Mur %d (%s) - Longueur : %.2f m, Hauteur : %.2f m, Surf. Nette : %.2f m²\n",
                                countMur, mur.getTypeMur().toString(), mur.calculerLongueur(), mur.getHauteur(), netArea));
                            
                            if (mur.getListeOuvertures() != null && !mur.getListeOuvertures().isEmpty()) {
                                sb.append("          Ouvertures : ");
                                for (int oi = 0; oi < mur.getListeOuvertures().size(); oi++) {
                                    Ouverture o = mur.getListeOuvertures().get(oi);
                                    String label = o instanceof Porte ? "Porte" : "Fenêtre";
                                    sb.append(String.format(Locale.US, "%s (%.1fx%.1fm à pos. %.2f)", 
                                        label, o.getLargeur(), o.getHauteur(), o.getPositionSurMur()));
                                    if (oi < mur.getListeOuvertures().size() - 1) sb.append(", ");
                                }
                                sb.append("\n");
                            }

                            if (mur.getRevetements() != null && !mur.getRevetements().isEmpty()) {
                                for (Revetement r : mur.getRevetements()) {
                                    sb.append(String.format(Locale.US, "          Revêtement : %s (%.2f €/m²) - %.2f €\n",
                                        r.getDesignation(), r.getPrixUnitaire(), mur.calculerPrixRevetement()));
                                }
                            } else {
                                sb.append("          Revêtement : Sans revêtement (0,00 €)\n");
                            }
                            countMur++;
                        }
                    }
                    sb.append(String.format(Locale.US, "      Total pièce : %,.2f €\n", piece.calculerDevis()));
                    sb.append("    ............................................................................\n");
                }
                sb.append("\n");
            }
            indexNiv++;
        }
        
        sb.append(line);
        sb.append("                     FIN DU RAPPORT DE DEVIS ESTIMATIF\n");
        sb.append(line);
        
        return sb.toString();
    }

    public static void exporterDevisVersFichier(Immeuble immeuble, File fichier) throws IOException {
        String rapport = genererTexteDevis(immeuble);

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(fichier),
                        StandardCharsets.UTF_8
                )
        )) {
            writer.print(rapport);
        }
    }
}
