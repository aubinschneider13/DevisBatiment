package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.SettingsView;

public class SettingsControleur {
    private SettingsView settingsView;
    private GestionnaireSauvegarde gestionnaire;
    
    public SettingsControleur(SettingsView settingsView, GestionnaireSauvegarde gestionnaire) {
        this.settingsView = settingsView;
        this.gestionnaire = gestionnaire;
        configurerToggle();
        configurerChemin();
        restaurerEtat();
    }
    
    private void restaurerEtat() {
        if (gestionnaire.isSauvegardeActive()) {
            settingsView.getToggleSauvegarde().setSelected(true);
            settingsView.getToggleSauvegarde().setText("Activée");
            settingsView.getCheminField().setText(gestionnaire.getCheminRacine());
            settingsView.afficherZoneChemin(true);
            settingsView.getStatusLabel().setStyle("-fx-font-size: 12px; -fx-text-fill: green;");
            settingsView.getStatusLabel().setText("✓ Sauvegarde activée : " + gestionnaire.getCheminRacine());
        }
    }

    private void configurerToggle() {
        settingsView.getToggleSauvegarde().setOnAction(e -> {
            boolean active = settingsView.getToggleSauvegarde().isSelected();
            if (active) {
                settingsView.getToggleSauvegarde().setText("Activée");
                settingsView.afficherZoneChemin(true);
                settingsView.getCheminField().requestFocus();
                settingsView.getStatusLabel().setText("");
            } else {
                settingsView.getToggleSauvegarde().setText("Désactivée");
                settingsView.afficherZoneChemin(false);
                settingsView.getCheminField().clear();
                settingsView.getStatusLabel().setText("");
                gestionnaire.desactiver();
            }
        });
    }

    private void configurerChemin() {
        // Bouton par défaut : se déclenche aussi avec Entrée globale dans la fenêtre
        settingsView.getValiderButton().setDefaultButton(true);

        // Clic sur le bouton
        settingsView.getValiderButton().setOnAction(e -> validerChemin());

        // Entrée dans le TextField : utiliser setOnAction (pas setOnKeyPressed)
        // car setOnKeyPressed est consommé en interne par le TextField
        settingsView.getCheminField().setOnAction(e -> validerChemin());
    }

    private void validerChemin() {
        String chemin = settingsView.getCheminField().getText();
        try {
            // Vérification que le chemin se termine par "Batiments"
            if (chemin == null || !chemin.trim().endsWith("Batiments")) {
                throw new IllegalArgumentException("Le chemin d'accès doit mener au dossier 'Batiments'.");
            }
            gestionnaire.activer(chemin);
            settingsView.getStatusLabel().setStyle("-fx-font-size: 12px; -fx-text-fill: green;");
            settingsView.getStatusLabel().setText("✓ Sauvegarde activée : " + chemin);
            
            // Attendre 0.5 secondes puis fermer
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
            pause.setOnFinished(e -> settingsView.fermerFenetre());
            pause.play();
            
        } catch (IllegalArgumentException ex) {
            settingsView.getStatusLabel().setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
            settingsView.getStatusLabel().setText("✗ " + ex.getMessage());
            gestionnaire.desactiver();
        }
    }
}