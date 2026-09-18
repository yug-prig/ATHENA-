package com.library.ui;

import com.library.ui.components.AnimationUtils;
import com.library.ui.components.ToastNotification;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Root Application Shell featuring a collapsible Gen Z dark-mode sidebar,
 * animated page switching, live clock header, and floating toast notification layer.
 */
public class MainView extends StackPane {
    public static final String PAGE_DASHBOARD = "DASHBOARD";
    public static final String PAGE_BOOKS = "BOOKS";
    public static final String PAGE_STUDENTS = "STUDENTS";
    public static final String PAGE_CIRCULATION = "CIRCULATION";

    private final BorderPane mainLayout;
    private final VBox sidebar;
    private Label pageTitleLabel;
    private Label pageSubtitleLabel;
    private Label clockLabel;
    private final StackPane contentContainer;

    private boolean isSidebarCollapsed = false;
    private final Map<String, Button> navButtons = new HashMap<>();
    private final Map<String, Node> views = new HashMap<>();

    private DashboardView dashboardView;
    private BookManagementView bookManagementView;
    private StudentRegistryView studentRegistryView;
    private CirculationView circulationView;

    private String currentPage = PAGE_DASHBOARD;

    public MainView() {
        this.pageTitleLabel = pageTitleLabel;
        this.pageSubtitleLabel = pageSubtitleLabel;
        this.clockLabel = clockLabel;
        getStyleClass().add("app-container");

        mainLayout = new BorderPane();

        // Top Header
        BorderPane header = buildHeader();
        mainLayout.setTop(header);

        // Collapsible Left Sidebar
        sidebar = buildSidebar();
        mainLayout.setLeft(sidebar);

        // Center Content Area
        contentContainer = new StackPane();
        contentContainer.getStyleClass().add("content-area");
        mainLayout.setCenter(contentContainer);

        // Add main layout to the root stack
        getChildren().add(mainLayout);

        // Register this container for floating toast notifications
        ToastNotification.setGlobalContainer(this);

        // Initialize lazy views
        initViews();

        // Navigate to initial dashboard
        navigateTo(PAGE_DASHBOARD);

        // Start live digital clock
        initLiveClock();

        // Welcome toast
        Timeline welcomeTimer = new Timeline(new KeyFrame(Duration.millis(600), e -> {
            ToastNotification.showSuccess("Welcome to Athena ✨", "Library systems online • All vibes checked");
        }));
        welcomeTimer.play();
    }

    private void initViews() {
        dashboardView = new DashboardView(this);
        bookManagementView = new BookManagementView();
        studentRegistryView = new StudentRegistryView();
        circulationView = new CirculationView();

        views.put(PAGE_DASHBOARD, dashboardView);
        views.put(PAGE_BOOKS, bookManagementView);
        views.put(PAGE_STUDENTS, studentRegistryView);
        views.put(PAGE_CIRCULATION, circulationView);
    }

    private BorderPane buildHeader() {
        BorderPane header = new BorderPane();
        header.getStyleClass().add("header-bar");

        // Left: Page Title & Breadcrumb
        VBox titleBox = new VBox(2);
        pageTitleLabel = new Label("Dashboard Overview");
        pageTitleLabel.getStyleClass().add("page-title");

        pageSubtitleLabel = new Label("Real-time telemetry & books currently out in the wild");
        pageSubtitleLabel.getStyleClass().add("page-subtitle");

        titleBox.getChildren().addAll(pageTitleLabel, pageSubtitleLabel);
        header.setLeft(titleBox);

        // Right: Live Clock & Database Status Pill
        HBox rightBox = new HBox(14);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        clockLabel = new Label();
        clockLabel.getStyleClass().add("header-badge");

        Label statusPill = new Label("⚡ SQLite Online");
        statusPill.getStyleClass().addAll("status-pill", "status-pill-available");
        statusPill.setStyle("-fx-font-size: 12px; -fx-padding: 6 14;");

        rightBox.getChildren().addAll(clockLabel, statusPill);
        header.setRight(rightBox);

        return header;
    }

    private VBox buildSidebar() {
        VBox side = new VBox();
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(240);
        side.setMinWidth(75);
        side.setSpacing(8);

        // Brand & Toggle Button Header
        HBox brandRow = new HBox();
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.getStyleClass().add("brand-container");

        VBox brandText = new VBox(2);
        Label brandTitle = new Label("✨ ATHENA");
        brandTitle.getStyleClass().add("brand-title");
        brandTitle.setStyle("-fx-text-fill: linear-gradient(to right, #8B5CF6, #EC4899); -fx-font-weight: 900;");

        Label brandSub = new Label("campus library vibes");
        brandSub.getStyleClass().add("brand-subtitle");
        brandText.getChildren().addAll(brandTitle, brandSub);
        HBox.setHgrow(brandText, Priority.ALWAYS);

        Button toggleBtn = new Button("☰");
        toggleBtn.getStyleClass().add("sidebar-toggle-btn");
        AnimationUtils.addButtonSpring(toggleBtn);
        toggleBtn.setOnAction(e -> toggleSidebarCollapse());

        brandRow.getChildren().addAll(brandText, toggleBtn);
        side.getChildren().add(brandRow);

        // Navigation section label
        Label mainNavLabel = new Label("EXPLORE");
        mainNavLabel.getStyleClass().add("nav-section-label");
        side.getChildren().add(mainNavLabel);

        // Nav Buttons
        Button btnDash = createNavButton("🚀", "Dashboard", PAGE_DASHBOARD);
        Button btnBooks = createNavButton("📚", "Book Stash", PAGE_BOOKS);
        Button btnMembers = createNavButton("👾", "The Gang", PAGE_STUDENTS);
        Button btnCirc = createNavButton("⚡", "Circulation Desk", PAGE_CIRCULATION);

        side.getChildren().addAll(btnDash, btnBooks, btnMembers, btnCirc);

        // Spacer to push bottom widget
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        side.getChildren().add(spacer);

        // Bottom system footer
        VBox footerBox = new VBox(4);
        footerBox.setStyle("-fx-padding: 12; -fx-background-color: #16162A; -fx-background-radius: 14px; -fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 14px;");
        Label footerTitle = new Label("✨ 100% Operational");
        footerTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981; -fx-font-weight: 800;");
        Label footerSub = new Label("knowledge hits different");
        footerSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        footerBox.getChildren().addAll(footerTitle, footerSub);
        side.getChildren().add(footerBox);

        return side;
    }

    private Button createNavButton(String icon, String labelText, String pageKey) {
        Button btn = new Button(icon + "   " + labelText);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setUserData(new String[]{icon, labelText});

        AnimationUtils.addButtonSpring(btn);

        btn.setOnAction(e -> navigateTo(pageKey));
        navButtons.put(pageKey, btn);
        return btn;
    }

    private void toggleSidebarCollapse() {
        isSidebarCollapsed = !isSidebarCollapsed;
        double targetWidth = isSidebarCollapsed ? 75.0 : 240.0;

        AnimationUtils.animateSlideWidth(sidebar, targetWidth, () -> {
            if (isSidebarCollapsed) {
                sidebar.getChildren().forEach(node -> {
                    if (node instanceof Label && node.getStyleClass().contains("nav-section-label")) {
                        node.setVisible(false);
                        node.setManaged(false);
                    }
                });
                navButtons.forEach((key, btn) -> {
                    String[] meta = (String[]) btn.getUserData();
                    btn.setText(meta[0]);
                    btn.setAlignment(Pos.CENTER);
                });
            } else {
                sidebar.getChildren().forEach(node -> {
                    if (node instanceof Label && node.getStyleClass().contains("nav-section-label")) {
                        node.setVisible(true);
                        node.setManaged(true);
                    }
                });
                navButtons.forEach((key, btn) -> {
                    String[] meta = (String[]) btn.getUserData();
                    btn.setText(meta[0] + "   " + meta[1]);
                    btn.setAlignment(Pos.CENTER_LEFT);
                });
            }
        });
    }

    public void navigateTo(String pageKey) {
        this.currentPage = pageKey;

        // Update nav highlight classes
        navButtons.forEach((k, btn) -> {
            btn.getStyleClass().remove("nav-button-active");
            if (k.equals(pageKey)) {
                btn.getStyleClass().add("nav-button-active");
            }
        });

        // Switch active view and trigger smooth entrance animation
        Node targetView = views.get(pageKey);
        if (targetView != null) {
            contentContainer.getChildren().setAll(targetView);
            AnimationUtils.fadeInUp(targetView);

            switch (pageKey) {
                case PAGE_DASHBOARD -> {
                    pageTitleLabel.setText("⚡ Dashboard Vibes");
                    pageSubtitleLabel.setText("Real-time library analytics & books currently out in the wild");
                    dashboardView.refreshData();
                }
                case PAGE_BOOKS -> {
                    pageTitleLabel.setText("📚 Book Stash & Catalogue");
                    pageSubtitleLabel.setText("Browse titles, authors, genres, and track available stock");
                    bookManagementView.refreshData();
                }
                case PAGE_STUDENTS -> {
                    pageTitleLabel.setText("👾 The Member Gang");
                    pageSubtitleLabel.setText("Verified student registry, departments, and active loan tracking");
                    studentRegistryView.refreshData();
                }
                case PAGE_CIRCULATION -> {
                    pageTitleLabel.setText("🔄 Circulation & Loan Desk");
                    pageSubtitleLabel.setText("Instant check-outs (14-day era), returns, and overdue fine assessment");
                    circulationView.refreshData();
                }
            }
        }
    }

    private void initLiveClock() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("EEE, MMM dd  •  HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            clockLabel.setText(LocalDateTime.now().format(timeFmt));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
}
