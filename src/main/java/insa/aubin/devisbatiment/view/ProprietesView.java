package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.modele.*;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Panneau de propriétés contextuel affiché sous le TreeView dans NavigateurView.
 *
 * Affiche les informations de l'élément sélectionné dans l'arbre :
 *   - Niveau    → hauteur plafond, nb appartements, coût estimé
 *   - Appartement → surface, nb pièces, revêtements appliqués, coût estimé
 *   - Pièce     → surface, hauteur plafond, revêtements (murs/sol/plafond), coût estimé
 *
 * Usage depuis AppControleur :
 *   proprietesView.afficherNiveau(niveau);
 *   proprietesView.afficherAppartement(appartement);
 *   proprietesView.afficherPiece(piece);
 *   proprietesView.effacer();   // quand rien n'est sélectionné
 */
public class ProprietesView extends VBox {

    // -------------------------------------------------------------------------
    // Couleurs de la palette de statut
    // -------------------------------------------------------------------------
    private static final String COULEUR_TITRE        = "#2c3e50";
    private static final String COULEUR_CLE          = "#7f8c8d";
    private static final String COULEUR_VALEUR       = "#2c3e50";
    private static final String COULEUR_COUT         = "#27ae60";
    private static final String COULEUR_FOND_ENTETE  = "#ecf0f1";
    private static final String COULEUR_BADGE_MUR    = "#3498db";
    private static final String COULEUR_BADGE_SOL    = "#e67e22";
    private static final String COULEUR_BADGE_PLAF   = "#9b59b6";
    private static final String COULEUR_AUCUN        = "#bdc3c7";

    // -------------------------------------------------------------------------
    // Conteneur principal scrollable
    // -------------------------------------------------------------------------
    private final VBox contenu;

    public ProprietesView() {
        setSpacing(0);
        setStyle(
                "-fx-background-color: #fafafa;" +
                        "-fx-border-color: #dfe6e9;" +
                        "-fx-border-width: 1 0 0 0;"
        );
        setMinHeight(0);

        // En-tête fixe
        Label entete = new Label("📋  Propriétés");
        entete.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px;" +
                        "-fx-text-fill: " + COULEUR_TITRE + ";" +
                        "-fx-padding: 8 10 8 10;" +
                        "-fx-background-color: " + COULEUR_FOND_ENTETE + ";"
        );
        entete.setMaxWidth(Double.MAX_VALUE);

        contenu = new VBox(6);
        contenu.setPadding(new Insets(8, 10, 10, 10));

        // ScrollPane pour permettre le défilement
        ScrollPane scrollPane = new ScrollPane(contenu);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;"
        );

        // Message par défaut
        afficherMessageVide();

        // Le ScrollPane prend toute la place disponible
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(entete, scrollPane);
    }

    // =========================================================================
    // API PUBLIQUE
    // =========================================================================

    /** Vide le panneau (aucune sélection). */
    public void effacer() {
        afficherMessageVide();
    }

    /**
     * Affiche les propriétés d'un Niveau.
     *
     * @param niveau le niveau sélectionné dans le TreeView
     */
    public void afficherNiveau(Niveau niveau) {
        contenu.getChildren().clear();

        // --- Titre ---
        ajouterTitre("🏢  Niveau");

        // --- Infos métriques ---
        ajouterLigne("Hauteur plafond",
                String.format("%.2f m", niveau.getHauteurPlafond()));
        ajouterLigne("Appartements",
                String.valueOf(niveau.getNbAppartements()));

        // Surface totale du niveau (somme des appartements)
        double surfaceTotale = niveau.getAppartements().stream()
                .mapToDouble(Appartement::calculerSurface).sum();
        ajouterLigne("Surface totale", String.format("%.2f m²", surfaceTotale));

        ajouterSeparateur();

        // --- Coût ---
        ajouterCout(niveau.calculerDevis());
    }

    /**
     * Affiche les propriétés d'un Appartement.
     *
     * @param appartement l'appartement sélectionné
     */
    public void afficherAppartement(Appartement appartement) {
        contenu.getChildren().clear();

        // --- Titre ---
        ajouterTitre("🏠  " + appartement);

        // --- Métriques ---
        ajouterLigne("Surface au sol",
                String.format("%.2f m²", appartement.calculerSurface()));
        ajouterLigne("Hauteur plafond",
                String.format("%.2f m", appartement.getHauteurPlafond()));
        ajouterLigne("Nb pièces",
                String.valueOf(appartement.getNbPieces()));

        ajouterSeparateur();

        // --- Revêtements par pièce ---
        if (appartement.getPieces().isEmpty()) {
            ajouterNote("Aucune pièce définie.");
        } else {
            ajouterSousTitre("Revêtements appliqués :");
            for (Piece piece : appartement.getPieces()) {
                ajouterBlocPiece(piece);
            }
        }

        ajouterSeparateur();

        // --- Coût total ---
        ajouterCout(appartement.calculerDevis());
    }

    /**
     * Affiche les propriétés d'une Pièce.
     *
     * @param piece la pièce sélectionnée
     */
    public void afficherPiece(Piece piece) {
        contenu.getChildren().clear();

        // --- Titre ---
        ajouterTitre("🚪  " + piece);

        // --- Métriques ---
        ajouterLigne("Surface au sol",
                String.format("%.2f m²", piece.calculerSurfaceTotale()));
        ajouterLigne("Hauteur plafond",
                String.format("%.2f m", piece.getHauteurPlafond()));
        ajouterLigne("Nb murs", String.valueOf(piece.getMurs().size()));

        ajouterSeparateur();

        // --- Revêtements ---
        ajouterSousTitre("Revêtements :");
        ajouterBlocPiece(piece);

        ajouterSeparateur();

        // --- Coût ---
        ajouterCout(piece.calculerDevis());
    }

    // =========================================================================
    // CONSTRUCTEURS DE LIGNES PRIVÉS
    // =========================================================================

    private void afficherMessageVide() {
        contenu.getChildren().clear();
        Label lbl = new Label("Sélectionnez un élément\ndans l'arbre.");
        lbl.setStyle(
                "-fx-text-fill: " + COULEUR_AUCUN + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-style: italic;"
        );
        lbl.setWrapText(true);
        contenu.getChildren().add(lbl);
    }

    private void ajouterTitre(String texte) {
        Label lbl = new Label(texte);
        lbl.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 12px;" +
                        "-fx-text-fill: " + COULEUR_TITRE + ";"
        );
        lbl.setWrapText(true);
        contenu.getChildren().add(lbl);
    }

    private void ajouterSousTitre(String texte) {
        Label lbl = new Label(texte);
        lbl.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 11px;" +
                        "-fx-text-fill: " + COULEUR_CLE + ";"
        );
        contenu.getChildren().add(lbl);
    }

    /**
     * Ligne clé → valeur sur la même rangée.
     */
    private void ajouterLigne(String cle, String valeur) {
        HBox ligne = new HBox();
        ligne.setSpacing(4);

        Label lblCle = new Label(cle + " :");
        lblCle.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: " + COULEUR_CLE + ";"
        );
        lblCle.setMinWidth(110);

        Label lblVal = new Label(valeur);
        lblVal.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + COULEUR_VALEUR + ";"
        );
        lblVal.setWrapText(true);

        ligne.getChildren().addAll(lblCle, lblVal);
        contenu.getChildren().add(ligne);
    }

    private void ajouterNote(String texte) {
        Label lbl = new Label(texte);
        lbl.setStyle(
                "-fx-font-size: 11px; -fx-font-style: italic;" +
                        "-fx-text-fill: " + COULEUR_AUCUN + ";"
        );
        contenu.getChildren().add(lbl);
    }

    private void ajouterSeparateur() {
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 2 0 2 0;");
        contenu.getChildren().add(sep);
    }

    /**
     * Affiche le coût estimé en grand, en vert.
     */
    private void ajouterCout(double montant) {
        HBox ligne = new HBox();
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setSpacing(6);

        Label lblCle = new Label("💰  Coût estimé :");
        lblCle.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + COULEUR_TITRE + ";"
        );

        Label lblVal = new Label(String.format("%.2f €", montant));
        lblVal.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + COULEUR_COUT + ";"
        );

        ligne.getChildren().addAll(lblCle, lblVal);
        contenu.getChildren().add(ligne);
    }

    // =========================================================================
    // BLOC REVÊTEMENTS D'UNE PIÈCE
    // =========================================================================

    /**
     * Affiche un mini-bloc récapitulatif des revêtements (murs, sol, plafond)
     * pour une pièce donnée. Utilisé à la fois dans afficherPiece() et
     * dans afficherAppartement() (pour chaque pièce de l'appartement).
     */
    private void ajouterBlocPiece(Piece piece) {
        VBox bloc = new VBox(4);
        bloc.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-border-color: #dfe6e9;" +
                        "-fx-border-radius: 4;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 6 8 6 8;"
        );

        // Nom de la pièce (uniquement utile dans la vue appartement)
        Label nomPiece = new Label(piece.toString());
        nomPiece.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 11px;" +
                        "-fx-text-fill: " + COULEUR_TITRE + ";"
        );
        bloc.getChildren().add(nomPiece);

        // --- Murs ---
        // Un mur est "revêtu" s'il a au moins un revêtement dans sa liste
        long nbMursAvecRev = piece.getCotesMurs().stream()
                .filter(cm -> !cm.getRevetements().isEmpty())
                .count();
        String infoMurs = "Aucun";

        if (nbMursAvecRev > 0) {
            // Collecte tous les noms distincts de revêtements posés sur les murs
            long nbDistincts = piece.getCotesMurs().stream()
                    .flatMap(cm -> cm.getRevetements().stream())
                    .map(Revetement::getDesignation)
                    .distinct()
                    .count();
            String premierNom = piece.getCotesMurs().stream()
                    .flatMap(cm -> cm.getRevetements().stream())
                    .map(Revetement::getDesignation)
                    .findFirst().orElse("?");
            infoMurs = nbMursAvecRev + " mur(s) — "
                    + (nbDistincts == 1 ? premierNom : "Variés");
        }
        bloc.getChildren().add(
                creerLigneRevetement("Murs", infoMurs, COULEUR_BADGE_MUR)
        );

        // --- Sol ---
        // Sol peut avoir plusieurs revêtements — on affiche le premier ou "Variés"
        List<Revetement> revsSol = piece.getSol().getRevetements();
        String infoSol;
        if (revsSol.isEmpty()) {
            infoSol = "Aucun";
        } else if (revsSol.stream().map(Revetement::getDesignation).distinct().count() == 1) {
            infoSol = revsSol.get(0).getDesignation();
        } else {
            infoSol = "Variés (" + revsSol.size() + ")";
        }
        bloc.getChildren().add(
                creerLigneRevetement("Sol", infoSol, COULEUR_BADGE_SOL)
        );

        // --- Plafond ---
        List<Revetement> revsPlafond = piece.getPlafond().getRevetements();
        String infoPlafond;
        if (revsPlafond.isEmpty()) {
            infoPlafond = "Aucun";
        } else if (revsPlafond.stream().map(Revetement::getDesignation).distinct().count() == 1) {
            infoPlafond = revsPlafond.get(0).getDesignation();
        } else {
            infoPlafond = "Variés (" + revsPlafond.size() + ")";
        }
        bloc.getChildren().add(
                creerLigneRevetement("Plafond", infoPlafond, COULEUR_BADGE_PLAF)
        );

        contenu.getChildren().add(bloc);
    }

    /**
     * Crée une ligne "badge couleur + type de surface + revêtement".
     *
     * @param typeSurface  "Murs", "Sol" ou "Plafond"
     * @param designation  nom du revêtement ou "Aucun"
     * @param couleurBadge couleur hexadécimale du badge
     */
    private HBox creerLigneRevetement(String typeSurface,
                                      String designation,
                                      String couleurBadge) {
        HBox ligne = new HBox(6);
        ligne.setAlignment(Pos.CENTER_LEFT);

        // Badge coloré (petit carré)
        Rectangle badge = new Rectangle(8, 8);
        badge.setFill(Color.web(couleurBadge));
        badge.setArcWidth(2);
        badge.setArcHeight(2);

        Label lblType = new Label(typeSurface + " :");
        lblType.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: " + COULEUR_CLE + ";"
        );
        lblType.setMinWidth(45);

        boolean aucun = designation.equals("Aucun");
        Label lblDesignation = new Label(designation);
        lblDesignation.setStyle(
                "-fx-font-size: 10px;" +
                        (aucun
                                ? "-fx-text-fill: " + COULEUR_AUCUN + "; -fx-font-style: italic;"
                                : "-fx-text-fill: " + COULEUR_VALEUR + "; -fx-font-weight: bold;")
        );
        lblDesignation.setWrapText(true);

        ligne.getChildren().addAll(badge, lblType, lblDesignation);
        return ligne;
    }
}