package com.library.ui;

import com.library.dao.StudentDao;
import com.library.dao.TransactionDao;
import com.library.model.Student;
import com.library.model.Transaction;
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
 * Student Registry View: Member registration, department tracking,
 * and active loan inspection with playful Gen Z touches.
 */
public class StudentRegistryView extends VBox {
    private final StudentDao studentDao;
    private final TransactionDao transactionDao;
    private TableView<Student> studentTable = null;
    private final ObservableList<Student> studentList = FXCollections.observableArrayList();

    private final TextField searchField;
    private final ComboBox<String> deptFilter;

    public StudentRegistryView() {
        this.studentDao = new StudentDao();
        this.transactionDao = new TransactionDao();

        getStyleClass().add("content-area");
        setSpacing(20);

        // Header controls (Search & Filter + Action Buttons)
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search the gang by name, student code, or email...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(360);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterStudents());

        deptFilter = new ComboBox<>();
        deptFilter.getStyleClass().add("combo-box");
        deptFilter.setPrefWidth(220);
        deptFilter.setOnAction(e -> filterStudents());

        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button btnAdd = new Button("+ Join Gang ✨");
        btnAdd.getStyleClass().add("btn-primary");
        AnimationUtils.addButtonSpring(btnAdd);
        btnAdd.setOnAction(e -> showStudentForm(null));

        Button btnEdit = new Button("✎ Edit");
        btnEdit.getStyleClass().add("btn-secondary");
        AnimationUtils.addButtonSpring(btnEdit);
        btnEdit.setOnAction(e -> {
            Student selected = studentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showStudentForm(selected);
            } else {
                ToastNotification.showError("No Selection", "Please select a member to edit ✨");
            }
        });

        Button btnLoans = new Button("📋 View Loans");
        btnLoans.getStyleClass().add("btn-outline");
        AnimationUtils.addButtonSpring(btnLoans);
        btnLoans.setOnAction(e -> {
            Student selected = studentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showStudentLoansDialog(selected);
            } else {
                ToastNotification.showError("No Selection", "Select a member to view their loan history");
            }
        });

        Button btnDelete = new Button("🗑 Delete");
        btnDelete.getStyleClass().add("btn-danger");
        AnimationUtils.addButtonSpring(btnDelete);
        btnDelete.setOnAction(e -> handleDeleteStudent());

        Button btnRefresh = new Button("↻");
        btnRefresh.getStyleClass().add("btn-outline");
        AnimationUtils.addButtonSpring(btnRefresh);
        btnRefresh.setOnAction(e -> {
            refreshData();
            ToastNotification.showInfo("Refreshed", "Student registry synced ✨");
        });

        topBar.getChildren().addAll(searchField, deptFilter, btnAdd, btnEdit, btnLoans, btnDelete, btnRefresh);

        // Students Table
        studentTable = new TableView<>();
        studentTable.setItems(studentList);
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(studentTable, Priority.ALWAYS);
        studentTable.setPlaceholder(new Label("No members found in the gang matching query ✨"));

        TableColumn<Student, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        idCol.setMaxWidth(60);

        TableColumn<Student, String> codeCol = new TableColumn<>("Student Code");
        codeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentCode()));
        codeCol.setMinWidth(140);

        TableColumn<Student, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setMinWidth(180);

        TableColumn<Student, String> emailCol = new TableColumn<>("Email Address");
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        emailCol.setMinWidth(220);

        TableColumn<Student, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDepartment()));
        deptCol.setMinWidth(170);

        TableColumn<Student, String> loansCol = new TableColumn<>("Active Loans");
        loansCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getActiveLoansCount() > 0 ? d.getValue().getActiveLoansCount() + " Active" : "No Loans"
        ));
        loansCol.setMaxWidth(140);
        loansCol.setCellFactory(col -> new TableCell<Student, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label();
                    pill.getStyleClass().add("status-pill");
                    if (item.contains("Active")) {
                        pill.setText("🔥 " + item);
                        pill.getStyleClass().add("status-pill-issued");
                    } else {
                        pill.setText("● Chill (0)");
                        pill.getStyleClass().add("status-pill-empty");
                    }
                    setGraphic(pill);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        studentTable.getColumns().addAll(idCol, codeCol, nameCol, emailCol, deptCol, loansCol);

        getChildren().addAll(topBar, studentTable);
        refreshData();
    }

    public void refreshData() {
        // Refresh department filter
        List<String> depts = studentDao.getDistinctDepartments();
        String currentSelection = deptFilter.getValue();
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("All Departments");
        options.addAll(depts);
        deptFilter.setItems(options);

        if (currentSelection != null && options.contains(currentSelection)) {
            deptFilter.setValue(currentSelection);
        } else {
            deptFilter.setValue("All Departments");
        }

        filterStudents();
    }

    private void filterStudents() {
        String query = searchField.getText();
        String dept = deptFilter.getValue();
        List<Student> result = studentDao.searchStudents(query, dept);
        studentList.setAll(result);
    }

    private void handleDeleteStudent() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ToastNotification.showError("No Selection", "Please select a student to delete");
            return;
        }

        if (selected.getActiveLoansCount() > 0) {
            DialogUtils.showError("Active Loans", "Student currently has " + selected.getActiveLoansCount() + 
                    " active book loan(s). Please return the books before removing the student record.");
            return;
        }

        boolean confirm = DialogUtils.showConfirm("Delete Member", 
                "Are you sure you want to delete member: " + selected.getName() + " (" + selected.getStudentCode() + ")?");
        if (!confirm) return;

        boolean success = studentDao.deleteStudent(selected.getId());
        if (success) {
            ToastNotification.showSuccess("Member Removed", selected.getName() + " removed from registry");
            refreshData();
        } else {
            ToastNotification.showError("Error", "Could not delete student record.");
        }
    }

    private void showStudentForm(Student existing) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(existing == null ? "Register Member" : "Edit Member #" + existing.getId());

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-pane");
        root.setPadding(new Insets(24));
        root.setPrefWidth(440);

        Label title = new Label(existing == null ? "✨ New Gang Member" : "✎ Edit Member Information");
        title.getStyleClass().add("panel-header-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        TextField codeField = new TextField(existing != null ? existing.getStudentCode() : "");
        codeField.setPromptText("e.g. STU-2024-101");

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setPromptText("Full Name");

        TextField emailField = new TextField(existing != null ? existing.getEmail() : "");
        emailField.setPromptText("student@university.edu");

        TextField deptField = new TextField(existing != null ? existing.getDepartment() : "");
        deptField.setPromptText("Department / Major");

        grid.add(new Label("Student Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Department:"), 0, 3);
        grid.add(deptField, 1, 3);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        AnimationUtils.addButtonSpring(cancelBtn);
        cancelBtn.setOnAction(e -> modal.close());

        Button saveBtn = new Button(existing == null ? "Register ✨" : "Save Changes");
        saveBtn.getStyleClass().add("btn-primary");
        AnimationUtils.addButtonSpring(saveBtn);
        saveBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String dept = deptField.getText().trim();

            if (code.isEmpty() || name.isEmpty() || email.isEmpty() || dept.isEmpty()) {
                DialogUtils.showError("Validation Error", "All fields are mandatory.");
                return;
            }

            if (existing == null) {
                Student newStudent = new Student(code, name, email, dept);
                boolean ok = studentDao.addStudent(newStudent);
                if (ok) {
                    modal.close();
                    refreshData();
                    ToastNotification.showSuccess("Welcome to the Gang! ✨", name + " registered successfully");
                } else {
                    DialogUtils.showError("Registration Error", "Failed to register student. Note: Student Code must be unique.");
                }
            } else {
                existing.setStudentCode(code);
                existing.setName(name);
                existing.setEmail(email);
                existing.setDepartment(dept);

                boolean ok = studentDao.updateStudent(existing);
                if (ok) {
                    modal.close();
                    refreshData();
                    ToastNotification.showSuccess("Updated ✨", "Member details saved");
                } else {
                    DialogUtils.showError("Update Error", "Could not save student changes.");
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

    private void showStudentLoansDialog(Student student) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Borrowing History - " + student.getName());

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-pane");
        root.setPadding(new Insets(24));
        root.setPrefWidth(650);
        root.setPrefHeight(450);

        Label title = new Label("Loan History for: " + student.getName() + " (" + student.getStudentCode() + ")");
        title.getStyleClass().add("panel-header-title");

        TableView<Transaction> loansTable = new TableView<>();
        loansTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(loansTable, Priority.ALWAYS);

        TableColumn<Transaction, String> bookCol = new TableColumn<>("Book Title");
        bookCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookTitle()));

        TableColumn<Transaction, String> issueCol = new TableColumn<>("Issue Date");
        issueCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIssueDate()));
        issueCol.setMaxWidth(110);

        TableColumn<Transaction, String> dueCol = new TableColumn<>("Due Date");
        dueCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDueDate()));
        dueCol.setMaxWidth(110);

        TableColumn<Transaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setMaxWidth(130);
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
                        pill.setText("● Active");
                        pill.getStyleClass().add("status-pill-issued");
                    }
                    setGraphic(pill);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Transaction, String> fineCol = new TableColumn<>("Fine");
        fineCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFineAmount() > 0 ? String.format("$%.2f", d.getValue().getFineAmount()) : "$0.00"
        ));
        fineCol.setMaxWidth(90);

        loansTable.getColumns().addAll(bookCol, issueCol, dueCol, statusCol, fineCol);

        List<Transaction> studentLoans = transactionDao.getTransactionsByStudent(student.getId());
        loansTable.setItems(FXCollections.observableArrayList(studentLoans));

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        AnimationUtils.addButtonSpring(closeBtn);
        closeBtn.setOnAction(e -> modal.close());

        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, loansTable, btnRow);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception ignored) {
        }
        modal.setScene(scene);
        modal.showAndWait();
    }
}
