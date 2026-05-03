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
            gestionnaire.activer(chemin);
            settingsView.getStatusLabel().setStyle("-fx-font-size: 12px; -fx-text-fill: green;");
            settingsView.getStatusLabel().setText("✓ Sauvegarde activée : " + chemin);
        } catch (IllegalArgumentException ex) {
            settingsView.getStatusLabel().setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
            settingsView.getStatusLabel().setText("✗ " + ex.getMessage());
            gestionnaire.desactiver();
        }
    }
}