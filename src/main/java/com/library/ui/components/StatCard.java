package com.library.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Reusable Gen Z Stat Card featuring hover bounce micro-interactions,
 * animated number rolling, glowing badge tags, and gradient accent lines.
 */
public class StatCard extends VBox {
    private final Label titleLabel;
    private final Label valueLabel;
    private final Label badgeLabel;
    private final Label iconLabel;

    public StatCard(String title, String initialValue, String badgeText, String badgeStyleClass, String iconGlyph) {
        getStyleClass().add("stat-card");
        setSpacing(12);
        HBox.setHgrow(this, Priority.ALWAYS);

        // Micro-interaction hover bounce
        AnimationUtils.addHoverBounce(this);

        // Top row: Title and Icon
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label(title.toUpperCase());
        titleLabel.getStyleClass().add("stat-card-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        iconLabel = new Label(iconGlyph);
        iconLabel.setStyle("-fx-font-size: 20px;");

        VBox iconWrapper = new VBox(iconLabel);
        iconWrapper.getStyleClass().add("stat-icon-container");
        if ("stat-badge-blue".equals(badgeStyleClass)) {
            iconWrapper.setStyle("-fx-background-color: rgba(139, 92, 246, 0.18);");
        } else if ("stat-badge-green".equals(badgeStyleClass)) {
            iconWrapper.setStyle("-fx-background-color: rgba(16, 185, 129, 0.18);");
        } else if ("stat-badge-amber".equals(badgeStyleClass)) {
            iconWrapper.setStyle("-fx-background-color: rgba(245, 158, 11, 0.18);");
        } else {
            iconWrapper.setStyle("-fx-background-color: rgba(236, 72, 153, 0.18);");
        }

        topRow.getChildren().addAll(titleLabel, iconWrapper);

        // Center row: Big numeric value
        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("stat-card-value");

        // Bottom row: Badge / helper info
        badgeLabel = new Label(badgeText);
        badgeLabel.getStyleClass().addAll("stat-card-badge", badgeStyleClass);

        HBox bottomRow = new HBox(badgeLabel);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(topRow, valueLabel, bottomRow);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setAnimatedInt(int target, String prefix, String suffix) {
        AnimationUtils.animateNumberCount(valueLabel, target, prefix, suffix);
    }

    public void setAnimatedCurrency(double target, String prefix) {
        AnimationUtils.animateCurrencyCount(valueLabel, target, prefix);
    }

    public void setBadgeText(String text) {
        badgeLabel.setText(text);
    }
}
