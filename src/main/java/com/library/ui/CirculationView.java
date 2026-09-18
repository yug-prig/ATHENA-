package com.library.ui;

import com.library.dao.BookDao;
import com.library.dao.StudentDao;
import com.library.dao.TransactionDao;
import com.library.model.Book;
import com.library.model.Student;
import com.library.model.Transaction;
import com.library.service.CirculationService;
import com.library.ui.components.AnimationUtils;
import com.library.ui.components.DialogUtils;
import com.library.ui.components.ToastNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Circulation Desk View: Check-out with stock validation and 14-day due date,
 * check-in with late fee calculation, animated bouncy buttons, and toast notifications.
 */
public class CirculationView extends VBox {
    private final CirculationService circulationService;
    private final TransactionDao transactionDao;
    private final BookDao bookDao;
    private final StudentDao studentDao;

    // Check-out controls
    private ComboBox<Book> bookComboBox;
    private ComboBox<Student> studentComboBox;
    private Label stockIndicatorLabel;
    private Label dueDatePreviewLabel;

    // Table controls
    private final TableView<Transaction> transactionTable;
    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    private final TextField searchField;
    private final ComboBox<String> statusFilter;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CirculationView() {
        this.circulationService = new CirculationService();
        this.transactionDao = new TransactionDao();
        this.bookDao = new BookDao();
        this.studentDao = new StudentDao();

        getStyleClass().add("content-area");
        setSpacing(20);

        buildCheckOutPanel();
        
        // Search & Filter Bar
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search transactions by book title, ISBN, member, or code...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(380);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTransactions());

        statusFilter = new ComboBox<>();
        statusFilter.getStyleClass().add("combo-box");
        statusFilter.getItems().addAll("All Statuses", "ISSUED", "OVERDUE", "RETURNED");
        statusFilter.setValue("All Statuses");
        statusFilter.setOnAction(e -> filterTransactions());

        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button btnReturn = new Button("📥 Return Selected Book");
        btnReturn.getStyleClass().add("btn-success");
        AnimationUtils.addButtonSpring(btnReturn);
        btnReturn.setOnAction(e -> handleProcessReturn());

        Button btnRefresh = new Button("↻");
        btnRefresh.getStyleClass().add("btn-outline");
        AnimationUtils.addButtonSpring(btnRefresh);
        btnRefresh.setOnAction(e -> {
            refreshData();
            ToastNotification.showInfo("Desk Synced", "Circulation desk records updated ✨");
        });

        filterBar.getChildren().addAll(searchField, statusFilter, btnReturn, btnRefresh);

        // Transaction Table
        transactionTable = new TableView<>();
        transactionTable.setItems(transactionList);
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(transactionTable, Priority.ALWAYS);
        transactionTable.setPlaceholder(new Label("No circulation records found ✨"));

        TableColumn<Transaction, String> idCol = new TableColumn<>("TX ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        idCol.setMaxWidth(70);

        TableColumn<Transaction, String> bookCol = new TableColumn<>("Book Title");
        bookCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookTitle()));
        bookCol.setMinWidth(180);

        TableColumn<Transaction, String> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookIsbn() != null ? d.getValue().getBookIsbn() : "-"));
        isbnCol.setMaxWidth(130);

        TableColumn<Transaction, String> studentCol = new TableColumn<>("Member Name");
        studentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentName()));
        studentCol.setMinWidth(140);

        TableColumn<Transaction, String> codeCol = new TableColumn<>("Student Code");
        codeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentCode() != null ? d.getValue().getStudentCode() : "-"));
        codeCol.setMaxWidth(120);

        TableColumn<Transaction, String> issueDateCol = new TableColumn<>("Issued Date");
        issueDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIssueDate()));
        issueDateCol.setMaxWidth(105);

        TableColumn<Transaction, String> dueDateCol = new TableColumn<>("Due Date");
        dueDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDueDate()));
        dueDateCol.setMaxWidth(105);

        TableColumn<Transaction, String> returnDateCol = new TableColumn<>("Return Date");
        returnDateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getReturnDate() != null && !d.getValue().getReturnDate().isBlank() ? d.getValue().getReturnDate() : "—"
        ));
        returnDateCol.setMaxWidth(105);

        TableColumn<Transaction, String> statusCol = new TableColumn<>("Vibe / Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
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
                        pill.setText("● Returned");
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

        TableColumn<Transaction, String> fineCol = new TableColumn<>("Late Fee");
        fineCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFineAmount() > 0 ? String.format("$%.2f", d.getValue().getFineAmount()) : "Zero"
        ));
        fineCol.setMaxWidth(90);

        transactionTable.getColumns().addAll(idCol, bookCol, isbnCol, studentCol, codeCol, issueDateCol, dueDateCol, returnDateCol, statusCol, fineCol);

        getChildren().addAll(filterBar, transactionTable);
        refreshData();
    }

    private void buildCheckOutPanel() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("✨ Loan Desk — Check-Out Drop");
        title.getStyleClass().add("panel-header-title");
        Label subtitle = new Label("Issue a book to a registered member with an automatic 14-day checkout era.");
        subtitle.getStyleClass().add("panel-header-desc");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        titleRow.getChildren().add(titleBox);

        // Form elements in responsive layout
        HBox formRow = new HBox(16);
        formRow.setAlignment(Pos.CENTER_LEFT);

        // Book selector
        VBox bookGroup = new VBox(4);
        Label bookLabel = new Label("Select Book to Drop:");
        bookLabel.getStyleClass().add("form-label");
        bookComboBox = new ComboBox<>();
        bookComboBox.getStyleClass().add("combo-box");
        bookComboBox.setPrefWidth(280);
        bookComboBox.setPromptText("Pick a book from stash...");
        bookComboBox.setCellFactory(lv -> new ListCell<Book>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + " (" + item.getAvailableCopies() + " left)");
                }
            }
        });
        bookComboBox.setButtonCell(new ListCell<Book>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + " (" + item.getAvailableCopies() + " left)");
                }
            }
        });

        stockIndicatorLabel = new Label("");
        stockIndicatorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        bookComboBox.setOnAction(e -> updateStockPreview());
        bookGroup.getChildren().addAll(bookLabel, bookComboBox, stockIndicatorLabel);
        HBox.setHgrow(bookGroup, Priority.ALWAYS);

        // Student selector
        VBox studentGroup = new VBox(4);
        Label studentLabel = new Label("Select Gang Member:");
        studentLabel.getStyleClass().add("form-label");
        studentComboBox = new ComboBox<>();
        studentComboBox.getStyleClass().add("combo-box");
        studentComboBox.setPrefWidth(260);
        studentComboBox.setPromptText("Select verified member...");
        studentComboBox.setCellFactory(lv -> new ListCell<Student>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " [" + item.getStudentCode() + "]");
                }
            }
        });
        studentComboBox.setButtonCell(new ListCell<Student>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " [" + item.getStudentCode() + "]");
                }
            }
        });
        Label memberSubtext = new Label("Active reader in good standing ✨");
        memberSubtext.getStyleClass().add("text-muted");
        memberSubtext.setStyle("-fx-font-size: 11px;");
        studentGroup.getChildren().addAll(studentLabel, studentComboBox, memberSubtext);
        HBox.setHgrow(studentGroup, Priority.ALWAYS);

        // Due date preview
        VBox dueGroup = new VBox(4);
        Label dueLabel = new Label("Loan Era:");
        dueLabel.getStyleClass().add("form-label");
        LocalDate due = LocalDate.now().plusDays(14);
        dueDatePreviewLabel = new Label("14 Days (Due: " + due.format(DATE_FMT) + ")");
        dueDatePreviewLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #EC4899; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        dueGroup.getChildren().addAll(dueLabel, dueDatePreviewLabel);

        // Issue button
        Button issueBtn = new Button("Confirm Drop 🚀");
        issueBtn.getStyleClass().addAll("btn-primary");
        issueBtn.setStyle("-fx-font-size: 13px; -fx-padding: 10 20;");
        AnimationUtils.addButtonSpring(issueBtn);
        issueBtn.setOnAction(e -> handleIssueBook());

        VBox btnBox = new VBox(issueBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(16, 0, 0, 0));

        formRow.getChildren().addAll(bookGroup, studentGroup, dueGroup, btnBox);
        card.getChildren().addAll(titleRow, formRow);

        getChildren().add(card);
    }

    private void updateStockPreview() {
        Book b = bookComboBox.getValue();
        if (b != null) {
            if (b.getAvailableCopies() > 0) {
                stockIndicatorLabel.setText("✓ In Stock: " + b.getAvailableCopies() + " of " + b.getTotalCopies() + " copies available ✨");
                stockIndicatorLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px;");
            } else {
                stockIndicatorLabel.setText("✕ Out of Stock: 0 of " + b.getTotalCopies() + " left");
                stockIndicatorLabel.setStyle("-fx-text-fill: #EC4899; -fx-font-size: 11px;");
            }
        } else {
            stockIndicatorLabel.setText("");
        }
    }

    private void handleIssueBook() {
        Book selectedBook = bookComboBox.getValue();
        Student selectedStudent = studentComboBox.getValue();

        if (selectedBook == null) {
            ToastNotification.showError("Pick a Book", "Please select a book to issue");
            return;
        }

        if (selectedStudent == null) {
            ToastNotification.showError("Pick a Member", "Please select a member to receive the book");
            return;
        }

        if (selectedBook.getAvailableCopies() <= 0) {
            ToastNotification.showError("Out of Stock", "All copies of '" + selectedBook.getTitle() + "' are currently checked out");
            return;
        }

        CirculationService.OperationResult res = circulationService.checkoutBook(selectedBook.getId(), selectedStudent.getId(), 14);
        if (res.success()) {
            ToastNotification.showSuccess("Loan Issued! 🚀", res.message());
            bookComboBox.setValue(null);
            studentComboBox.setValue(null);
            stockIndicatorLabel.setText("");
            refreshData();
        } else {
            ToastNotification.showError("Issue Failed", res.message());
        }
    }

    private void handleProcessReturn() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ToastNotification.showError("No Selection", "Please select a loan record from the table to return");
            return;
        }

        if ("RETURNED".equalsIgnoreCase(selected.getStatus())) {
            ToastNotification.showInfo("Already Returned", "This book was already returned on " + selected.getReturnDate() + " ✨");
            return;
        }

        // Calculate potential overdue fine preview
        long overdueDays = selected.getDaysOverdue();
        double fine = overdueDays * TransactionDao.DAILY_FINE_RATE;

        String confirmMsg = "Confirm return of '" + selected.getBookTitle() + "' from " + selected.getStudentName() + "?";
        if (overdueDays > 0) {
            confirmMsg += String.format("\n\n⚠️ OVERDUE NOTICE:\nThis loan is %d day(s) overdue.\nA late fee of $%.2f will be assessed ($0.50/day).", 
                                        overdueDays, fine);
        }

        boolean proceed = DialogUtils.showConfirm("Process Return", confirmMsg);
        if (!proceed) return;

        CirculationService.OperationResult res = circulationService.returnBook(selected.getId());
        if (res.success()) {
            if (res.fine() > 0) {
                ToastNotification.show(
                    "Return Processed 💸",
                    String.format("'%s' returned. Late fine assessed: $%.2f", selected.getBookTitle(), res.fine()),
                    ToastNotification.Type.ERROR
                );
            } else {
                ToastNotification.showSuccess("Returned Clean! ✨", res.message());
            }
            refreshData();
        } else {
            ToastNotification.showError("Return Failed", res.message());
        }
    }

    public void refreshData() {
        // Refresh book and student combo boxes
        List<Book> books = bookDao.getAllBooks();
        bookComboBox.setItems(FXCollections.observableArrayList(books));

        List<Student> students = studentDao.getAllStudents();
        studentComboBox.setItems(FXCollections.observableArrayList(students));

        updateStockPreview();
        filterTransactions();
    }

    private void filterTransactions() {
        String query = searchField.getText();
        String status = statusFilter.getValue();
        List<Transaction> result = transactionDao.searchTransactions(query, status);
        transactionList.setAll(result);
    }
}
