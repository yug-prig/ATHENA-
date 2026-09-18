package com.library.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Floating glassmorphic Toast notification popup with animated entrance and exit.
 */
public class ToastNotification extends HBox {
    public enum Type {
        SUCCESS("✨", "toast-success"),
        ERROR("🚨", "toast-error"),
        INFO("💡", "toast-info");

        final String icon;
        final String cssClass;

        Type(String icon, String cssClass) {
            this.icon = icon;
            this.cssClass = cssClass;
        }
    }

    private static StackPane globalOverlayContainer;

    public static void setGlobalContainer(StackPane container) {
        globalOverlayContainer = container;
    }

    public static void show(String title, String message, Type type) {
        if (globalOverlayContainer != null) {
            ToastNotification toast = new ToastNotification(title, message, type);
            StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(toast, new Insets(0, 25, 25, 0));
            globalOverlayContainer.getChildren().add(toast);
            toast.play();
        }
    }

    public static void showSuccess(String title, String message) {
        show(title, message, Type.SUCCESS);
    }

    public static void showError(String title, String message) {
        show(title, message, Type.ERROR);
    }

    public static void showInfo(String title, String message) {
        show(title, message, Type.INFO);
    }

    public ToastNotification(String title, String message, Type type) {
        getStyleClass().addAll("toast-container", type.cssClass);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setMaxWidth(380);
        setPrefWidth(360);
        setPadding(new Insets(14, 18, 14, 18));

        Label iconLabel = new Label(type.icon);
        iconLabel.setStyle("-fx-font-size: 22px;");

        VBox textContainer = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1;");

        textContainer.getChildren().addAll(titleLabel, msgLabel);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        getChildren().addAll(iconLabel, textContainer);

        // Hover pause or manual dismiss on click
        setOnMouseClicked(e -> dismiss());
    }

    public void play() {
        setOpacity(0.0);
        setTranslateY(35.0);

        // Entrance
        FadeTransition fadeIn = new FadeTransition(Duration.millis(260), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(260), this);
        slideUp.setFromY(35.0);
        slideUp.setToY(0.0);
        slideUp.setInterpolator(Interpolator.EASE_OUT);

        // Pause
        PauseTransition wait = new PauseTransition(Duration.seconds(3.5));

        // Exit
        FadeTransition fadeOut = new FadeTransition(Duration.millis(240), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition slideDown = new TranslateTransition(Duration.millis(240), this);
        slideDown.setFromY(0.0);
        slideDown.setToY(25.0);
        slideDown.setInterpolator(Interpolator.EASE_IN);

        SequentialTransition seq = new SequentialTransition(
            new javafx.animation.ParallelTransition(fadeIn, slideUp),
            wait,
            new javafx.animation.ParallelTransition(fadeOut, slideDown)
        );

        seq.setOnFinished(e -> {
            if (getParent() instanceof Pane parent) {
                parent.getChildren().remove(this);
            }
        });

        seq.play();
    }

    private void dismiss() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), this);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            if (getParent() instanceof Pane parent) {
                parent.getChildren().remove(this);
            }
        });
        fade.play();
    }
}
