package com.library.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Utility for modern dark-themed modal dialogs and alerts.
 */
public class DialogUtils {

    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showSuccess(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, "✓ " + message);
    }

    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, "⚠ " + message);
    }

    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialogPane(alert.getDialogPane());

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialogPane(alert.getDialogPane());
        alert.showAndWait();
    }

    public static void styleDialogPane(DialogPane pane) {
        try {
            pane.getStylesheets().add(
                DialogUtils.class.getResource("/styles.css").toExternalForm()
            );
        } catch (Exception ignored) {
        }
        pane.setStyle("""
            -fx-background-color: #202030;
            -fx-border-color: #363654;
            -fx-border-width: 1px;
            -fx-border-radius: 12px;
            -fx-background-radius: 12px;
        """);

        for (ButtonType bt : pane.getButtonTypes()) {
            Button btn = (Button) pane.lookupButton(bt);
            if (btn != null) {
                if (bt == ButtonType.OK || bt == ButtonType.YES) {
                    btn.getStyleClass().add("btn-primary");
                } else {
                    btn.getStyleClass().add("btn-secondary");
                }
            }
        }
    }
}
