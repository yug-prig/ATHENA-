package com.library.service;

import com.library.dao.BookDao;
import com.library.dao.StudentDao;
import com.library.dao.TransactionDao;
import com.library.db.DatabaseManager;
import com.library.model.Book;
import com.library.model.Student;
import com.library.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Service orchestrating Circulation operations (checkout, check-in, fine calculations).
 * Ensures atomic transactions and business rule validations.
 */
public class CirculationService {
    private final BookDao bookDao;
    private final StudentDao studentDao;
    private final TransactionDao transactionDao;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CirculationService() {
        this.bookDao = new BookDao();
        this.studentDao = new StudentDao();
        this.transactionDao = new TransactionDao();
    }

    public CirculationService(BookDao bookDao, StudentDao studentDao, TransactionDao transactionDao) {
        this.bookDao = bookDao;
        this.studentDao = studentDao;
        this.transactionDao = transactionDao;
    }

    /**
     * Result wrapper for circulation operations.
     */
    public record OperationResult(boolean success, String message, double fine) {
        public static OperationResult ok(String msg) {
            return new OperationResult(true, msg, 0.0);
        }
        public static OperationResult okWithFine(String msg, double fine) {
            return new OperationResult(true, msg, fine);
        }
        public static OperationResult fail(String msg) {
            return new OperationResult(false, msg, 0.0);
        }
    }

    /**
     * Checks out a book to a student.
     * Validates book copy availability, decrements available_copies, and inserts transaction.
     */
    public OperationResult checkoutBook(int bookId, int studentId, int loanDays) {
        Book book = bookDao.getBookById(bookId);
        if (book == null) {
            return OperationResult.fail("Book not found in library catalogue.");
        }

        if (book.getAvailableCopies() <= 0) {
            return OperationResult.fail("No copies of '" + book.getTitle() + "' are currently available for issue.");
        }

        Student student = studentDao.getStudentById(studentId);
        if (student == null) {
            return OperationResult.fail("Student not registered in registry.");
        }

        LocalDate today = LocalDate.now();
        int days = loanDays > 0 ? loanDays : 14;
        LocalDate due = today.plusDays(days);

        // Execute atomically in a DB transaction
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Decrement available copies
                String decSql = """
                    UPDATE books
                    SET available_copies = available_copies - 1
                    WHERE id = ? AND available_copies > 0;
                """;
                try (PreparedStatement ps = conn.prepareStatement(decSql)) {
                    ps.setInt(1, bookId);
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        return OperationResult.fail("Failed to reserve copy. Out of stock.");
                    }
                }

                // 2. Insert transaction
                String transSql = """
                    INSERT INTO transactions (book_id, student_id, issue_date, due_date, return_date, status, fine_amount)
                    VALUES (?, ?, ?, ?, NULL, 'ISSUED', 0.0);
                """;
                try (PreparedStatement ps = conn.prepareStatement(transSql)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, studentId);
                    ps.setString(3, today.format(DATE_FMT));
                    ps.setString(4, due.format(DATE_FMT));
                    ps.executeUpdate();
                }

                conn.commit();
                return OperationResult.ok("Book '" + book.getTitle() + "' successfully issued to " + 
                                          student.getName() + " (Due: " + due.format(DATE_FMT) + ").");
            } catch (SQLException e) {
                conn.rollback();
                return OperationResult.fail("Database error during check-out: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return OperationResult.fail("Database connection failure: " + e.getMessage());
        }
    }

    /**
     * Checks in / returns a borrowed book.
     * Computes late fees ($0.50/day past due date), increments available_copies,
     * and marks transaction as 'RETURNED'.
     */
    public OperationResult returnBook(int transactionId) {
        Transaction transaction = transactionDao.getTransactionById(transactionId);
        if (transaction == null) {
            return OperationResult.fail("Transaction record #" + transactionId + " not found.");
        }

        if ("RETURNED".equalsIgnoreCase(transaction.getStatus())) {
            return OperationResult.fail("Book has already been returned on " + transaction.getReturnDate() + ".");
        }

        LocalDate today = LocalDate.now();
        double calculatedFine = 0.0;
        long overdueDays = 0;

        try {
            LocalDate dueDate = LocalDate.parse(transaction.getDueDate(), DATE_FMT);
            overdueDays = ChronoUnit.DAYS.between(dueDate, today);
            if (overdueDays > 0) {
                calculatedFine = overdueDays * TransactionDao.DAILY_FINE_RATE;
            }
        } catch (Exception ignored) {
        }

        // Execute atomically in a DB transaction
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Mark transaction as RETURNED
                String updateTransSql = """
                    UPDATE transactions
                    SET return_date = ?, status = 'RETURNED', fine_amount = ?
                    WHERE id = ?;
                """;
                try (PreparedStatement ps = conn.prepareStatement(updateTransSql)) {
                    ps.setString(1, today.format(DATE_FMT));
                    ps.setDouble(2, calculatedFine);
                    ps.setInt(3, transactionId);
                    ps.executeUpdate();
                }

                // 2. Increment available copies of the book
                String incBookSql = """
                    UPDATE books
                    SET available_copies = MIN(total_copies, available_copies + 1)
                    WHERE id = ?;
                """;
                try (PreparedStatement ps = conn.prepareStatement(incBookSql)) {
                    ps.setInt(1, transaction.getBookId());
                    ps.executeUpdate();
                }

                conn.commit();

                String msg = "Book '" + transaction.getBookTitle() + "' returned successfully.";
                if (overdueDays > 0) {
                    msg += String.format(" Overdue by %d days. Late fine assessed: $%.2f ($0.50/day).", 
                                         overdueDays, calculatedFine);
                }
                return OperationResult.okWithFine(msg, calculatedFine);

            } catch (SQLException e) {
                conn.rollback();
                return OperationResult.fail("Database error during return: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return OperationResult.fail("Database connection failure: " + e.getMessage());
        }
    }
}
