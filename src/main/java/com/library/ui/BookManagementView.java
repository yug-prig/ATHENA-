package com.library.ui;

import com.library.dao.BookDao;
import com.library.model.Book;
import com.library.ui.components.AnimationUtils;
import com.library.ui.components.DialogUtils;
import com.library.ui.components.ToastNotification;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Book Management View: Catalogue browsing, real-time live search,
 * stock tracking, category filtering, and modal CRUD operations with toast feedback.
 */
public class BookManagementView extends VBox {
    private final BookDao bookDao;
    private TableView<Book> bookTable = null;
    private final ObservableList<Book> bookList = FXCollections.observableArrayList();

    private final TextField searchField;
    private final ComboBox<String> categoryFilter;

    public BookManagementView() {
        this.bookDao = new BookDao();

        getStyleClass().add("content-area");
        setSpacing(20);

        // Header controls (Search & Filter + Actions)
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search book stash by title, author, or ISBN...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(360);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterBooks());

        categoryFilter = new ComboBox<>();
        categoryFilter.getStyleClass().add("combo-box");
        categoryFilter.setPrefWidth(200);
        categoryFilter.setOnAction(e -> filterBooks());

        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add New Book");
        btnAdd.getStyleClass().add("btn-primary");
        AnimationUtils.addButtonSpring(btnAdd);
        btnAdd.setOnAction(e -> showBookForm(null));

        Button btnEdit = new Button("✎ Edit");
        btnEdit.getStyleClass().add("btn-secondary");
        AnimationUtils.addButtonSpring(btnEdit);
        btnEdit.setOnAction(e -> {
            Book selected = bookTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showBookForm(selected);
            } else {
                ToastNotification.showError("No Selection", "Select a book from the table to edit ✨");
            }
        });

        Button btnDelete = new Button("🗑 Delete");
        btnDelete.getStyleClass().add("btn-danger");
        AnimationUtils.addButtonSpring(btnDelete);
        btnDelete.setOnAction(e -> handleDeleteBook());

        Button btnRefresh = new Button("↻");
        btnRefresh.getStyleClass().add("btn-outline");
        AnimationUtils.addButtonSpring(btnRefresh);
        btnRefresh.setOnAction(e -> {
            refreshData();
            ToastNotification.showInfo("Refreshed", "Catalogue synchronized with database");
        });

        topBar.getChildren().addAll(searchField, categoryFilter, btnAdd, btnEdit, btnDelete, btnRefresh);

        // Book Table
        bookTable = new TableView<>();
        bookTable.setItems(bookList);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(bookTable, Priority.ALWAYS);
        bookTable.setPlaceholder(new Label("No books found in the stash matching criteria ✨"));

        TableColumn<Book, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        idCol.setMaxWidth(60);
        idCol.setMinWidth(40);

        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIsbn()));
        isbnCol.setMinWidth(120);

        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setMinWidth(220);

        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuthor()));
        authorCol.setMinWidth(160);

        TableColumn<Book, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        catCol.setMinWidth(130);

        TableColumn<Book, Integer> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getTotalCopies()).asObject());
        totalCol.setMaxWidth(80);

        TableColumn<Book, Integer> availCol = new TableColumn<>("Available");
        availCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAvailableCopies()).asObject());
        availCol.setMaxWidth(90);

        TableColumn<Book, String> statusCol = new TableColumn<>("Availability");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAvailableCopies() > 0 ? "AVAILABLE (" + d.getValue().getAvailableCopies() + ")" : "OUT OF STOCK"
        ));
        statusCol.setMaxWidth(170);
        statusCol.setCellFactory(col -> new TableCell<Book, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label();
                    pill.getStyleClass().add("status-pill");
                    if (item.startsWith("AVAILABLE")) {
                        pill.setText("● " + item);
                        pill.getStyleClass().add("status-pill-available");
                    } else {
                        pill.setText("● " + item);
                        pill.getStyleClass().add("status-pill-overdue");
                    }
                    setGraphic(pill);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        bookTable.getColumns().addAll(idCol, isbnCol, titleCol, authorCol, catCol, totalCol, availCol, statusCol);

        getChildren().addAll(topBar, bookTable);
        refreshData();
    }

    public void refreshData() {
        // Update categories dropdown
        List<String> categories = bookDao.getDistinctCategories();
        String currentSelection = categoryFilter.getValue();
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("All Categories");
        options.addAll(categories);
        categoryFilter.setItems(options);

        if (currentSelection != null && options.contains(currentSelection)) {
            categoryFilter.setValue(currentSelection);
        } else {
            categoryFilter.setValue("All Categories");
        }

        filterBooks();
    }

    private void filterBooks() {
        String query = searchField.getText();
        String category = categoryFilter.getValue();
        List<Book> result = bookDao.searchBooks(query, category);
        bookList.setAll(result);
    }

    private void handleDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ToastNotification.showError("No Selection", "Please pick a book to delete");
            return;
        }

        if (selected.getAvailableCopies() < selected.getTotalCopies()) {
            boolean confirm = DialogUtils.showConfirm("Active Loans Detected",
                    "There are currently " + (selected.getTotalCopies() - selected.getAvailableCopies()) + 
                    " copies of '" + selected.getTitle() + "' out in the wild. Deleting will cascade and remove related transactions. Proceed?");
            if (!confirm) return;
        } else {
            boolean confirm = DialogUtils.showConfirm("Delete Confirmation",
                    "Permanently delete '" + selected.getTitle() + "' from inventory?");
            if (!confirm) return;
        }

        boolean success = bookDao.deleteBook(selected.getId());
        if (success) {
            ToastNotification.showSuccess("Book Deleted", "'" + selected.getTitle() + "' removed from stash ✨");
            refreshData();
        } else {
            ToastNotification.showError("Delete Error", "Could not delete the book. Check DB connectivity.");
        }
    }

    private void showBookForm(Book existing) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(existing == null ? "Add New Book" : "Edit Book #" + existing.getId());

        VBox root = new VBox(16);
        root.getStyleClass().addAll("dialog-pane");
        root.setPadding(new Insets(24));
        root.setPrefWidth(460);

        Label title = new Label(existing == null ? "✨ Register New Book" : "✎ Update Book Details");
        title.getStyleClass().add("panel-header-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        TextField isbnField = new TextField(existing != null ? existing.getIsbn() : "");
        isbnField.setPromptText("e.g. 978-0134685991");

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        titleField.setPromptText("Book Title");

        TextField authorField = new TextField(existing != null ? existing.getAuthor() : "");
        authorField.setPromptText("Author(s)");

        TextField catField = new TextField(existing != null ? existing.getCategory() : "");
        catField.setPromptText("e.g. Computer Science, Fiction, Math");

        TextField totalCopiesField = new TextField(existing != null ? String.valueOf(existing.getTotalCopies()) : "3");
        TextField availCopiesField = new TextField(existing != null ? String.valueOf(existing.getAvailableCopies()) : "3");

        grid.add(new Label("ISBN:"), 0, 0);
        grid.add(isbnField, 1, 0);
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Author:"), 0, 2);
        grid.add(authorField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(catField, 1, 3);
        grid.add(new Label("Total Copies:"), 0, 4);
        grid.add(totalCopiesField, 1, 4);
        grid.add(new Label("Available Copies:"), 0, 5);
        grid.add(availCopiesField, 1, 5);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        AnimationUtils.addButtonSpring(cancelBtn);
        cancelBtn.setOnAction(e -> modal.close());

        Button saveBtn = new Button(existing == null ? "Add to Stash ✨" : "Save Changes");
        saveBtn.getStyleClass().add("btn-primary");
        AnimationUtils.addButtonSpring(saveBtn);
        saveBtn.setOnAction(e -> {
            String isbn = isbnField.getText().trim();
            String bTitle = titleField.getText().trim();
            String author = authorField.getText().trim();
            String cat = catField.getText().trim();

            if (isbn.isEmpty() || bTitle.isEmpty() || author.isEmpty() || cat.isEmpty()) {
                DialogUtils.showError("Validation Error", "All fields must be filled out.");
                return;
            }

            int total, avail;
            try {
                total = Integer.parseInt(totalCopiesField.getText().trim());
                avail = Integer.parseInt(availCopiesField.getText().trim());
                if (total <= 0 || avail < 0 || avail > total) {
                    DialogUtils.showError("Validation Error", "Total copies must be >= 1, and Available copies must be between 0 and Total.");
                    return;
                }
            } catch (NumberFormatException ex) {
                DialogUtils.showError("Invalid Number", "Total and Available copies must be integers.");
                return;
            }

            if (existing == null) {
                Book newBook = new Book(isbn, bTitle, author, cat, total, avail);
                boolean ok = bookDao.addBook(newBook);
                if (ok) {
                    modal.close();
                    refreshData();
                    ToastNotification.showSuccess("Book Stashed! ✨", "'" + bTitle + "' added to inventory");
                } else {
                    DialogUtils.showError("Duplicate or Error", "Failed to add book. Note: ISBN must be unique.");
                }
            } else {
                existing.setIsbn(isbn);
                existing.setTitle(bTitle);
                existing.setAuthor(author);
                existing.setCategory(cat);
                existing.setTotalCopies(total);
                existing.setAvailableCopies(avail);

                boolean ok = bookDao.updateBook(existing);
                if (ok) {
                    modal.close();
                    refreshData();
                    ToastNotification.showSuccess("Updated ✨", "Book changes saved successfully");
                } else {
                    DialogUtils.showError("Update Error", "Could not save changes to the book.");
                }
            }
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(title, grid, btnRow);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception ignored) {
        }
        modal.setScene(scene);
        modal.showAndWait();
    }
}
