package com.library.dao;

import com.library.db.DatabaseManager;
import com.library.model.DashboardMetrics;
import com.library.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Circulation Transactions.
 * Strictly uses PreparedStatement and transaction management.
 */
public class TransactionDao {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final double DAILY_FINE_RATE = 0.50; // $0.50 per day

    /**
     * Issues a new book loan.
     */
    public boolean issueBook(int bookId, int studentId, int loanDays) {
        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(loanDays > 0 ? loanDays : 14);

        String sql = """
            INSERT INTO transactions (book_id, student_id, issue_date, due_date, return_date, status, fine_amount)
            VALUES (?, ?, ?, ?, NULL, 'ISSUED', 0.0);
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, bookId);
            ps.setInt(2, studentId);
            ps.setString(3, today.format(DATE_FMT));
            ps.setString(4, due.format(DATE_FMT));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error issuing book transaction: " + e.getMessage());
        }
        return false;
    }

    /**
     * Processes a book return, updating return_date, status to 'RETURNED', and recording fine.
     */
    public boolean returnBook(int transactionId, double fineAmount) {
        LocalDate today = LocalDate.now();
        String sql = """
            UPDATE transactions
            SET return_date = ?, status = 'RETURNED', fine_amount = ?
            WHERE id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, today.format(DATE_FMT));
            ps.setDouble(2, Math.max(0.0, fineAmount));
            ps.setInt(3, transactionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error returning book transaction: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks all currently active loans and marks those past their due date as 'OVERDUE'
     * and calculates their accrued fine.
     */
    public void refreshOverdueStatuses() {
        LocalDate today = LocalDate.now();
        String sqlSelect = """
            SELECT id, due_date FROM transactions
            WHERE status IN ('ISSUED', 'OVERDUE');
        """;

        String sqlUpdate = """
            UPDATE transactions
            SET status = ?, fine_amount = ?
            WHERE id = ?;
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
             ResultSet rs = psSelect.executeQuery();
             PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String dueStr = rs.getString("due_date");
                try {
                    LocalDate dueDate = LocalDate.parse(dueStr, DATE_FMT);
                    long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
                    if (overdueDays > 0) {
                        double fine = overdueDays * DAILY_FINE_RATE;
                        psUpdate.setString(1, "OVERDUE");
                        psUpdate.setDouble(2, fine);
                        psUpdate.setInt(3, id);
                        psUpdate.addBatch();
                    }
                } catch (Exception ignored) {
                }
            }
            psUpdate.executeBatch();
        } catch (SQLException e) {
            System.err.println("Error refreshing overdue statuses: " + e.getMessage());
        }
    }

    public Transaction getTransactionById(int id) {
        String sql = """
            SELECT t.*, b.title AS book_title, b.isbn AS book_isbn,
                   s.name AS student_name, s.student_code AS student_code
            FROM transactions t
            LEFT JOIN books b ON t.book_id = b.id
            LEFT JOIN students s ON t.student_id = s.id
            WHERE t.id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTransaction(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching transaction: " + e.getMessage());
        }
        return null;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.*, b.title AS book_title, b.isbn AS book_isbn,
                   s.name AS student_name, s.student_code AS student_code
            FROM transactions t
            LEFT JOIN books b ON t.book_id = b.id
            LEFT JOIN students s ON t.student_id = s.id
            ORDER BY t.id DESC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all transactions: " + e.getMessage());
        }
        return list;
    }

    public List<Transaction> getRecentTransactions(int limit) {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.*, b.title AS book_title, b.isbn AS book_isbn,
                   s.name AS student_name, s.student_code AS student_code
            FROM transactions t
            LEFT JOIN books b ON t.book_id = b.id
            LEFT JOIN students s ON t.student_id = s.id
            ORDER BY t.id DESC
            LIMIT ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recent transactions: " + e.getMessage());
        }
        return list;
    }

    public List<Transaction> getTransactionsByStudent(int studentId) {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.*, b.title AS book_title, b.isbn AS book_isbn,
                   s.name AS student_name, s.student_code AS student_code
            FROM transactions t
            LEFT JOIN books b ON t.book_id = b.id
            LEFT JOIN students s ON t.student_id = s.id
            WHERE t.student_id = ?
            ORDER BY t.id DESC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching transactions by student: " + e.getMessage());
        }
        return list;
    }

    public List<Transaction> searchTransactions(String query, String statusFilter) {
        List<Transaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT t.*, b.title AS book_title, b.isbn AS book_isbn,
                   s.name AS student_name, s.student_code AS student_code
            FROM transactions t
            LEFT JOIN books b ON t.book_id = b.id
            LEFT JOIN students s ON t.student_id = s.id
            WHERE 1=1
        """);
        List<String> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("""
                AND (LOWER(b.title) LIKE ? OR LOWER(b.isbn) LIKE ? 
                     OR LOWER(s.name) LIKE ? OR LOWER(s.student_code) LIKE ?)
            """);
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("All Statuses")) {
            sql.append(" AND UPPER(t.status) = UPPER(?) ");
            params.add(statusFilter.trim());
        }

        sql.append(" ORDER BY t.id DESC;");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching transactions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Aggregates live numbers for Dashboard Stat Cards.
     */
    public DashboardMetrics getDashboardMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();
        refreshOverdueStatuses();

        String sqlMetrics = """
            SELECT 
                (SELECT COALESCE(SUM(total_copies), 0) FROM books) AS total_books,
                (SELECT COUNT(*) FROM students) AS total_members,
                (SELECT COUNT(*) FROM transactions WHERE status IN ('ISSUED', 'OVERDUE')) AS books_issued,
                (SELECT COALESCE(SUM(fine_amount), 0.0) FROM transactions) AS overdue_fines,
                (SELECT COUNT(*) FROM transactions WHERE status = 'OVERDUE') AS overdue_count;
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlMetrics);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                metrics.setTotalBooks(rs.getInt("total_books"));
                metrics.setTotalMembers(rs.getInt("total_members"));
                metrics.setBooksIssued(rs.getInt("books_issued"));
                metrics.setOverdueFines(rs.getDouble("overdue_fines"));
                metrics.setOverdueCount(rs.getInt("overdue_count"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching dashboard metrics: " + e.getMessage());
        }
        return metrics;
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction(
            rs.getInt("id"),
            rs.getInt("book_id"),
            rs.getInt("student_id"),
            rs.getString("issue_date"),
            rs.getString("due_date"),
            rs.getString("return_date"),
            rs.getString("status"),
            rs.getDouble("fine_amount")
        );
        t.setBookTitle(rs.getString("book_title"));
        t.setBookIsbn(rs.getString("book_isbn"));
        t.setStudentName(rs.getString("student_name"));
        t.setStudentCode(rs.getString("student_code"));
        return t;
    }
}
