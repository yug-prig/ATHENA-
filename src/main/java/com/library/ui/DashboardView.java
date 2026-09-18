package com.library.ui;

import com.library.dao.BookDao;
import com.library.dao.StudentDao;
import com.library.dao.TransactionDao;
import com.library.model.DashboardMetrics;
import com.library.model.Transaction;
import com.library.ui.components.AnimationUtils;
import com.library.ui.components.StatCard;
import com.library.ui.components.ToastNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Gen Z Dashboard View featuring animated number rolling, glowing stat cards,
 * gradient action buttons, and live activity feeds with pill badges.
 */
public class DashboardView extends VBox {
    private final TransactionDao transactionDao;
    private final BookDao bookDao;
    private final StudentDao studentDao;
    private final MainView mainView;

    private StatCard totalBooksCard;
    private StatCard totalMembersCard;
    private StatCard booksIssuedCard;
    private StatCard overdueFinesCard;

    private TableView<Transaction> recentTable;
    private final ObservableList<Transaction> recentTransactions = FXCollections.observableArrayList();

    public DashboardView(MainView mainView) {
        this.mainView = mainView;
        this.transactionDao = new TransactionDao();
        this.bookDao = new BookDao();
        this.studentDao = new StudentDao();

        getStyleClass().add("content-area");
        setSpacing(24);

        buildMetricsSection();
        buildQuickActionsBanner();
        buildRecentActivitySection();

        refreshData();
    }

    private void buildMetricsSection() {
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);

        totalBooksCard = new StatCard("Book Stash", "0", "📚 aesthetic collection", "stat-badge-blue", "✨");
        totalMembersCard = new StatCard("Certified Members", "0", "👾 the gang", "stat-badge-green", "⚡");
        booksIssuedCard = new StatCard("Out In The Wild", "0", "🔥 reading era", "stat-badge-amber", "🚀");
        overdueFinesCard = new StatCard("Fine Stash", "$0.00", "💀 don't ghost us", "stat-badge-red", "💸");

        statsRow.getChildren().addAll(totalBooksCard, totalMembersCard, booksIssuedCard, overdueFinesCard);
        getChildren().add(statsRow);
    }

    private void buildQuickActionsBanner() {
        HBox banner = new HBox(16);
        banner.getStyleClass().add("panel-card");
        banner.setAlignment(Pos.CENTER_LEFT);

        VBox textInfo = new VBox(4);
        Label bannerTitle = new Label("✨ Quick Actions & Desk Shortcuts");
        bannerTitle.getStyleClass().add("panel-header-title");

        Label bannerDesc = new Label("Standard 14-day checkout era • $0.50/day late penalty • Zero stress");
        bannerDesc.getStyleClass().add("panel-header-desc");
        textInfo.getChildren().addAll(bannerTitle, bannerDesc);
        HBox.setHgrow(textInfo, Priority.ALWAYS);

        Button btnIssue = new Button("⚡ Quick Issue");
        btnIssue.getStyleClass().addAll("btn-primary");
        AnimationUtils.addButtonSpring(btnIssue);
        btnIssue.setOnAction(e -> mainView.navigateTo(MainView.PAGE_CIRCULATION));

        Button btnAddBook = new Button("📚 Add Book");
        btnAddBook.getStyleClass().addAll("btn-secondary");
        AnimationUtils.addButtonSpring(btnAddBook);
        btnAddBook.setOnAction(e -> mainView.navigateTo(MainView.PAGE_BOOKS));

        Button btnAddMember = new Button("👾 Join Gang");
        btnAddMember.getStyleClass().addAll("btn-secondary");
        AnimationUtils.addButtonSpring(btnAddMember);
        btnAddMember.setOnAction(e -> mainView.navigateTo(MainView.PAGE_STUDENTS));

        Button btnRefresh = new Button("↻ Refresh");
        btnRefresh.getStyleClass().addAll("btn-outline");
        AnimationUtils.addButtonSpring(btnRefresh);
        btnRefresh.setOnAction(e -> {
            refreshData();
            ToastNotification.showInfo("Telemetry Refreshed", "Dashboard metrics are 100% up to date ✨");
        });

        banner.getChildren().addAll(textInfo, btnIssue, btnAddBook, btnAddMember, btnRefresh);
        getChildren().add(banner);
    }

    private void buildRecentActivitySection() {
        VBox container = new VBox(14);
        container.getStyleClass().add("panel-card");
        VBox.setVgrow(container, Priority.ALWAYS);

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(2);
        Label sectionTitle = new Label("Recent Circulation Log");
        sectionTitle.getStyleClass().add("panel-header-title");
        Label sectionDesc = new Label("Live drops of books checked out and returned by members");
        sectionDesc.getStyleClass().add("panel-header-desc");
        titles.getChildren().addAll(sectionTitle, sectionDesc);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button viewAllBtn = new Button("Open Desk →");
        viewAllBtn.getStyleClass().add("btn-outline");
        AnimationUtils.addButtonSpring(viewAllBtn);
        viewAllBtn.setOnAction(e -> mainView.navigateTo(MainView.PAGE_CIRCULATION));

        headerRow.getChildren().addAll(titles, viewAllBtn);

        // Recent Activity Table
        recentTable = new TableView<>();
        recentTable.setItems(recentTransactions);
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(recentTable, Priority.ALWAYS);
        recentTable.setPlaceholder(new Label("No recent activity found. Catalogue is quiet! ✨"));

        TableColumn<Transaction, String> idCol = new TableColumn<>("TX ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        idCol.setMaxWidth(80);
        idCol.setMinWidth(60);

        TableColumn<Transaction, String> bookCol = new TableColumn<>("Book Title & ISBN");
        bookCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getBookTitle() + " (" + (data.getValue().getBookIsbn() != null ? data.getValue().getBookIsbn() : "-") + ")"
        ));

        TableColumn<Transaction, String> studentCol = new TableColumn<>("Member");
        studentCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStudentName() + " [" + (data.getValue().getStudentCode() != null ? data.getValue().getStudentCode() : "-") + "]"
        ));

        TableColumn<Transaction, String> issueDateCol = new TableColumn<>("Issued");
        issueDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIssueDate()));
        issueDateCol.setMaxWidth(110);

        TableColumn<Transaction, String> dueDateCol = new TableColumn<>("Due Date");
        dueDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDueDate()));
        dueDateCol.setMaxWidth(110);

        TableColumn<Transaction, String> statusCol = new TableColumn<>("Vibe / Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setMaxWidth(140);
        statusCol.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label();
                    pill.getStyleClass().add("status-pill");
                    if ("RETURNED".equalsIgnoreCase(status)) {
                        pill.setText("● Returned Clean");
                        pill.getStyleClass().add("status-pill-returned");
                    } else if ("OVERDUE".equalsIgnoreCase(status)) {
                        pill.setText("● Late AF");
                        pill.getStyleClass().add("status-pill-overdue");
                    } else {
                        pill.setText("● In The Wild");
                        pill.getStyleClass().add("status-pill-issued");
                    }
                    setGraphic(pill);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Transaction, String> fineCol = new TableColumn<>("Fine");
        fineCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getFineAmount() > 0 ? String.format("$%.2f", data.getValue().getFineAmount()) : "Zero"
        ));
        fineCol.setMaxWidth(90);

        recentTable.getColumns().addAll(idCol, bookCol, studentCol, issueDateCol, dueDateCol, statusCol, fineCol);

        container.getChildren().addAll(headerRow, recentTable);
        getChildren().add(container);
    }

    public void refreshData() {
        // Refresh metrics with animated counting
        DashboardMetrics metrics = transactionDao.getDashboardMetrics();
        totalBooksCard.setAnimatedInt(metrics.getTotalBooks(), "", " Copies");
        totalMembersCard.setAnimatedInt(metrics.getTotalMembers(), "", " Readers");
        booksIssuedCard.setAnimatedInt(metrics.getBooksIssued(), "", " Active");
        overdueFinesCard.setAnimatedCurrency(metrics.getOverdueFines(), "$");
        overdueFinesCard.setBadgeText(metrics.getOverdueCount() + " Overdue Loans");

        // Refresh recent activity
        List<Transaction> recents = transactionDao.getRecentTransactions(8);
        recentTransactions.setAll(recents);
    }
}
