package insa.aubin.devisbatiment.modele;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class DevisExporter {

    public static String genererTexteDevis(Batiment immeuble) {
        if (immeuble == null) return "Aucun immeuble s\u00e9lectionn\u00e9.";

        StringBuilder sb = new StringBuilder();
        String line = "================================================================================\n";
        String doubleLine = "================================================================================\n";

        sb.append(doubleLine);
        sb.append("                  DEVIS ESTIMATIF DE BATIMENT : ").append(immeuble.getNomBatiment().toUpperCase()).append("\n");
        sb.append(doubleLine);
        sb.append(String.format("B\u00e2timent Type : %s\n", immeuble.getTypeBatiment()));
        sb.append(String.format("Nombre de niveaux : %d\n", immeuble.getNbNiveaux()));
        sb.append(String.format(Locale.US, "Prix global estim\u00e9 : %,.2f \u20ac\n", immeuble.calculerDevisTotal()));
        sb.append(doubleLine).append("\n");

        if (immeuble.getNiveaux() == null || immeuble.getNiveaux().isEmpty()) {
            sb.append("Aucun niveau d\u00e9fini dans ce b\u00e2timent.\n");
            return sb.toString();
        }

        int indexNiv = 0;
        for (Niveau niveau : immeuble.getNiveaux()) {
            sb.append(String.format("NIVEAU %d\n", indexNiv));
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append(String.format("  Hauteur sous plafond : %.2f m\n", niveau.getHauteurPlafond()));
            sb.append(String.format("  Nombre d'appartements : %d\n", niveau.getNbAppartements()));
            if (niveau.getTremies() != null && !niveau.getTremies().isEmpty()) {
                int nbEscaliers = 0;
                int nbAscenseurs = 0;
                for (Tremie tremie : niveau.getTremies()) {
                    if (tremie instanceof Escalier) nbEscaliers++;
                    else if (tremie instanceof Ascenseur) nbAscenseurs++;
                }
                sb.append(String.format("  Tr\u00e9mies : %d escalier(s), %d ascenseur(s)\n", nbEscaliers, nbAscenseurs));
                sb.append(String.format(Locale.US, "  Co\u00fbt total des tr\u00e9mies : %,.2f \u20ac\n", niveau.calculerPrixTremies()));
            }
            sb.append(String.format(Locale.US, "  Sous-total niveau : %,.2f \u20ac\n", niveau.calculerDevis()));
            sb.append("--------------------------------------------------------------------------------\n\n");

            if (niveau.getAppartements() == null || niveau.getAppartements().isEmpty()) {
                sb.append("  Aucun appartement dessin\u00e9 \u00e0 ce niveau.\n\n");
                indexNiv++;
                continue;
            }

            for (Appartement appart : niveau.getAppartements()) {
                sb.append(String.format("  * %s\n", appart.toString()));
                sb.append(String.format(Locale.US, "    Surface habitable au sol : %.2f m\u00b2\n", appart.calculerSurface()));
                sb.append(String.format("    Nombre de pi\u00e8ces : %d\n", appart.getNbPieces()));

                // --- AJOUT : Décompte et tarification des menuiseries de l'appartement ---
                int nbPortes = 0;
                int nbFenêtres = 0;
                double totalMenuiseries = 0;

                if (appart.getMurs() != null) {
                    for (Mur m : appart.getMurs()) {
                        if (m.getListeOuvertures() != null) {
                            for (Ouverture o : m.getListeOuvertures()) {
                                totalMenuiseries += o.getPrixForfaitaire();
                                if (o instanceof Porte) nbPortes++;
                                else nbFenêtres++;
                            }
                        }
                    }
                }

                sb.append(String.format("    Menuiseries install\u00e9es : %d porte(s), %d fen\u00eatre(s)\n", nbPortes, nbFenêtres));
                sb.append(String.format(Locale.US, "    Co\u00fbt total des menuiseries : %,.2f \u20ac\n", totalMenuiseries));
                double totalTremiesAppartement = appart.calculerPrixTremies(niveau.getTremies());
                if (totalTremiesAppartement > 0) {
                    sb.append(String.format(Locale.US, "    Co\u00fbt des tr\u00e9mies associ\u00e9es : %,.2f \u20ac\n", totalTremiesAppartement));
                }
                // ────────────────────────────────────────────────────────────────────────

                sb.append(String.format(Locale.US, "    Sous-total appartement (Pi\u00e8ces + Menuiseries + Tr\u00e9mies) : %,.2f \u20ac\n",
                        appart.calculerDevis(niveau.getTremies())));
                sb.append("    ............................................................................\n");

                if (appart.getPieces() == null || appart.getPieces().isEmpty()) {
                    sb.append("    Aucune pi\u00e8ce d\u00e9finie dans cet appartement.\n\n");
                    continue;
                }

                for (Piece piece : appart.getPieces()) {
                    sb.append(String.format("    - %s\n", piece.toString()));
                    sb.append(String.format(Locale.US, "      Surface au sol : %.2f m\u00b2\n", piece.calculerSurfaceTotale()));

                    // Flooring (Sol)
                    Sol sol = piece.getSol();
                    if (sol != null) {
                        sb.append("      > Sol : ");
                        if (sol.getRevetements() == null || sol.getRevetements().isEmpty()) {
                            sb.append("Sans rev\u00eatement (0,00 \u20ac)\n");
                        } else {
                            for (Revetement r : sol.getRevetements()) {
                                sb.append(String.format(Locale.US, "%s (%.2f \u20ac/m\u00b2) - %.2f \u20ac\n",
                                        r.getDesignation(), r.getPrixUnitaire(), sol.calculerPrixRevetement()));
                            }
                        }
                    }

                    // Ceiling (Plafond)
                    Plafond plafond = piece.getPlafond();
                    if (plafond != null) {
                        sb.append("      > Plafond : ");
                        if (plafond.getRevetements() == null || plafond.getRevetements().isEmpty()) {
                            sb.append("Sans rev\u00eatement (0,00 \u20ac)\n");
                        } else {
                            for (Revetement r : plafond.getRevetements()) {
                                sb.append(String.format(Locale.US, "%s (%.2f \u20ac/m\u00b2) - %.2f \u20ac\n",
                                        r.getDesignation(), r.getPrixUnitaire(), plafond.calculerPrixRevetement()));
                            }
                        }
                    }

                    // Walls (Murs)
                    if (piece.getMurs() != null && !piece.getMurs().isEmpty()) {
                        sb.append("      > Murs de la pi\u00e8ce :\n");
                        int countMur = 1;
                        for (CoteMur cm : piece.getCotesMurs()) {
                            Mur mur = cm.getMurParent();
                            double netArea = cm.calculerSurface();
                            String sideLabel = (cm == mur.getCoteGauche()) ? "Gauche" : "Droit";
                            sb.append(String.format(Locale.US, "        Mur %d (%s, C\u00f4t\u00e9 %s) - Longueur : %.2f m, Hauteur : %.2f m, Surf. Nette : %.2f m\u00b2\n",
                                    countMur, mur.getTypeMur().toString(), sideLabel, mur.calculerLongueur(), mur.getHauteur(), netArea));

                            if (mur.getListeOuvertures() != null && !mur.getListeOuvertures().isEmpty()) {
                                sb.append("          Ouvertures : ");
                                for (int oi = 0; oi < mur.getListeOuvertures().size(); oi++) {
                                    Ouverture o = mur.getListeOuvertures().get(oi);
                                    String label = o instanceof Porte ? "Porte" : "Fen\u00eatre";
                                    sb.append(String.format(Locale.US, "%s (%.1fx%.1fm \u00e0 pos. %.2f)",
                                            label, o.getLargeur(), o.getHauteur(), o.getPositionSurMur()));
                                    if (oi < mur.getListeOuvertures().size() - 1) sb.append(", ");
                                }
                                sb.append("\n");
                            }

                            if (cm.getRevetements() != null && !cm.getRevetements().isEmpty()) {
                                for (Revetement r : cm.getRevetements()) {
                                    sb.append(String.format(Locale.US, "          Rev\u00eatement : %s (%.2f \u20ac/m\u00b2) - %.2f \u20ac\n",
                                            r.getDesignation(), r.getPrixUnitaire(), cm.calculerPrixRevetement()));
                                }
                            } else {
                                sb.append("          Rev\u00eatement : Sans rev\u00eatement (0,00 \u20ac)\n");
                            }
                            countMur++;
                        }
                    }
                    sb.append(String.format(Locale.US, "      Total pi\u00e8ce : %,.2f \u20ac\n", piece.calculerDevis()));
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

    public static void exporterDevisVersFichier(Batiment immeuble, File fichier) throws IOException {
        String rapport = genererTexteDevis(immeuble);

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(fichier),
                        StandardCharsets.UTF_8
                )
        )) {
            // Écriture du BOM UTF-8 pour forcer la reconnaissance automatique par Windows/Notepad++
            writer.print('\uFEFF');
            writer.print(rapport);
        }
    }
}
