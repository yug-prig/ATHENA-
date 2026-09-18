package com.library.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Modern JavaFX Animation Utilities for snappy, playful, 60fps micro-interactions.
 */
public class AnimationUtils {

    /**
     * Smooth fade-in and slide-up entrance animation for views and cards.
     */
    public static void fadeInUp(Node node) {
        if (node == null) return;
        node.setOpacity(0.0);
        node.setTranslateY(18.0);

        FadeTransition fade = new FadeTransition(Duration.millis(280), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition translate = new TranslateTransition(Duration.millis(280), node);
        translate.setFromY(18.0);
        translate.setToY(0.0);
        translate.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, translate);
        pt.play();
    }

    /**
     * Adds an interactive hover lift/bounce effect (scale up to 1.025 with ease).
     */
    public static void addHoverBounce(Node node) {
        if (node == null) return;

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(160), node);
        scaleIn.setToX(1.022);
        scaleIn.setToY(1.022);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(160), node);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);
        scaleOut.setInterpolator(Interpolator.EASE_BOTH);

        node.setOnMouseEntered(e -> {
            scaleOut.stop();
            scaleIn.playFromStart();
        });

        node.setOnMouseExited(e -> {
            scaleIn.stop();
            scaleOut.playFromStart();
        });
    }

    /**
     * Adds a snappy click bounce/spring to buttons.
     */
    public static void addButtonSpring(Button btn) {
        if (btn == null) return;

        ScaleTransition pressScale = new ScaleTransition(Duration.millis(90), btn);
        pressScale.setToX(0.95);
        pressScale.setToY(0.95);

        ScaleTransition releaseScale = new ScaleTransition(Duration.millis(120), btn);
        releaseScale.setToX(1.0);
        releaseScale.setToY(1.0);
        releaseScale.setInterpolator(Interpolator.EASE_OUT);

        btn.setOnMousePressed(e -> {
            releaseScale.stop();
            pressScale.playFromStart();
        });

        btn.setOnMouseReleased(e -> {
            pressScale.stop();
            releaseScale.playFromStart();
        });
    }

    /**
     * Smoothly counts up an integer value in a label (e.g., 0 -> 42).
     */
    public static void animateNumberCount(Label label, int targetValue, String prefix, String suffix) {
        if (label == null) return;
        if (targetValue <= 0) {
            label.setText(prefix + "0" + suffix);
            return;
        }

        int steps = Math.min(25, targetValue);
        Duration duration = Duration.millis(500);
        double stepTime = duration.toMillis() / steps;

        Timeline timeline = new Timeline();
        for (int i = 0; i <= steps; i++) {
            final int currentVal = (int) Math.round(((double) i / steps) * targetValue);
            KeyFrame kf = new KeyFrame(Duration.millis(i * stepTime), e -> {
                label.setText(prefix + currentVal + suffix);
            });
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    /**
     * Smoothly counts up a floating currency value (e.g., $0.00 -> $5.50).
     */
    public static void animateCurrencyCount(Label label, double targetValue, String prefix) {
        if (label == null) return;
        if (targetValue <= 0.0) {
            label.setText(prefix + "0.00");
            return;
        }

        int steps = 25;
        Duration duration = Duration.millis(550);
        double stepTime = duration.toMillis() / steps;

        Timeline timeline = new Timeline();
        for (int i = 0; i <= steps; i++) {
            final double currentVal = ((double) i / steps) * targetValue;
            KeyFrame kf = new KeyFrame(Duration.millis(i * stepTime), e -> {
                label.setText(String.format("%s%.2f", prefix, currentVal));
            });
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    /**
     * Smoothly animates the width of a region (such as a sidebar).
     */
    public static void animateSlideWidth(Region region, double targetWidth, Runnable onFinished) {
        if (region == null) return;
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(region.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH);
        KeyFrame kf = new KeyFrame(Duration.millis(220), kv);
        timeline.getKeyFrames().add(kf);
        if (onFinished != null) {
            timeline.setOnFinished(e -> onFinished.run());
        }
        timeline.play();
    }
}
