package com.library.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Manages SQLite database connection and initial schema generation.
 * Operates on a local 'library.db' file.
 */
public class DatabaseManager {
    private static final String DB_FILE = "library.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver: " + e.getMessage());
        }
    }

    /**
     * Obtains a new database connection with foreign key support enabled.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
        }
        return conn;
    }

    /**
     * Initializes the database schema and seeds initial data if required.
     */
    public static void initializeDatabase() {
        String sqlBooks = """
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                isbn TEXT UNIQUE NOT NULL,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                category TEXT NOT NULL,
                total_copies INTEGER NOT NULL DEFAULT 1,
                available_copies INTEGER NOT NULL DEFAULT 1
            );
        """;

        String sqlStudents = """
            CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_code TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                department TEXT NOT NULL
            );
        """;

        String sqlTransactions = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                issue_date TEXT NOT NULL,
                due_date TEXT NOT NULL,
                return_date TEXT,
                status TEXT NOT NULL,
                fine_amount REAL NOT NULL DEFAULT 0.0,
                FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sqlBooks);
            stmt.execute(sqlStudents);
            stmt.execute(sqlTransactions);

            // Seed initial records if empty
            seedInitialDataIfEmpty(conn);

        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Populates the database with realistic sample records if first run.
     */
    private static void seedInitialDataIfEmpty(Connection conn) throws SQLException {
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM books;")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Already populated
                return;
            }
        }

        System.out.println("Seeding database with initial library sample records...");

        conn.setAutoCommit(false);
        try {
            // 1. Insert sample books
            String insertBookSql = """
                INSERT INTO books (isbn, title, author, category, total_copies, available_copies)
                VALUES (?, ?, ?, ?, ?, ?);
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertBookSql)) {
                Object[][] sampleBooks = {
                    {"978-0134685991", "Effective Java (3rd Edition)", "Joshua Bloch", "Computer Science", 6, 4},
                    {"978-0132350884", "Clean Code: Agile Software Craftsmanship", "Robert C. Martin", "Software Engineering", 5, 3},
                    {"978-0201633610", "Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma et al.", "Software Engineering", 4, 3},
                    {"978-0262033848", "Introduction to Algorithms (CLRS)", "Thomas H. Cormen", "Algorithms", 7, 5},
                    {"978-1449331818", "Designing Data-Intensive Applications", "Martin Kleppmann", "Data Systems", 5, 4},
                    {"978-0131103627", "The C Programming Language", "Brian Kernighan, Dennis Ritchie", "Computer Science", 4, 4},
                    {"978-0596007126", "Head First Design Patterns", "Eric Freeman, Elisabeth Robson", "Software Engineering", 5, 4},
                    {"978-1593279509", "Automate the Boring Stuff with Python", "Al Sweigart", "Programming", 3, 3}
                };

                for (Object[] b : sampleBooks) {
                    ps.setString(1, (String) b[0]);
                    ps.setString(2, (String) b[1]);
                    ps.setString(3, (String) b[2]);
                    ps.setString(4, (String) b[3]);
                    ps.setInt(5, (Integer) b[4]);
                    ps.setInt(6, (Integer) b[5]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 2. Insert sample students
            String insertStudentSql = """
                INSERT INTO students (student_code, name, email, department)
                VALUES (?, ?, ?, ?);
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertStudentSql)) {
                String[][] sampleStudents = {
                    {"STU-2024-001", "Alex Rivera", "alex.rivera@university.edu", "Computer Science"},
                    {"STU-2024-002", "Samantha Chen", "s.chen@university.edu", "Software Engineering"},
                    {"STU-2024-003", "Marcus Vance", "marcus.v@university.edu", "Information Technology"},
                    {"STU-2024-004", "Priya Sharma", "priya.sharma@university.edu", "Data Science"},
                    {"STU-2024-005", "David Kim", "dkim@university.edu", "Computer Engineering"}
                };

                for (String[] s : sampleStudents) {
                    ps.setString(1, s[0]);
                    ps.setString(2, s[1]);
                    ps.setString(3, s[2]);
                    ps.setString(4, s[3]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 3. Insert sample transactions
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now();

            String insertTransSql = """
                INSERT INTO transactions (book_id, student_id, issue_date, due_date, return_date, status, fine_amount)
                VALUES (?, ?, ?, ?, ?, ?, ?);
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertTransSql)) {
                // Active normal loan (Book 1 to Student 1, issued 3 days ago, due in 11 days)
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setString(3, today.minusDays(3).format(fmt));
                ps.setString(4, today.plusDays(11).format(fmt));
                ps.setNull(5, java.sql.Types.VARCHAR);
                ps.setString(6, "ISSUED");
                ps.setDouble(7, 0.0);
                ps.addBatch();

                // Active overdue loan (Book 2 to Student 2, issued 20 days ago, due 6 days ago -> 6 overdue days * $0.50 = $3.00)
                ps.setInt(1, 2);
                ps.setInt(2, 2);
                ps.setString(3, today.minusDays(20).format(fmt));
                ps.setString(4, today.minusDays(6).format(fmt));
                ps.setNull(5, java.sql.Types.VARCHAR);
                ps.setString(6, "OVERDUE");
                ps.setDouble(7, 3.00);
                ps.addBatch();

                // Returned loan (Book 3 to Student 3, returned on time)
                ps.setInt(1, 3);
                ps.setInt(2, 3);
                ps.setString(3, today.minusDays(15).format(fmt));
                ps.setString(4, today.minusDays(1).format(fmt));
                ps.setString(5, today.minusDays(2).format(fmt));
                ps.setString(6, "RETURNED");
                ps.setDouble(7, 0.0);
                ps.addBatch();

                // Active loan (Book 4 to Student 4, issued today, due in 14 days)
                ps.setInt(1, 4);
                ps.setInt(2, 4);
                ps.setString(3, today.format(fmt));
                ps.setString(4, today.plusDays(14).format(fmt));
                ps.setNull(5, java.sql.Types.VARCHAR);
                ps.setString(6, "ISSUED");
                ps.setDouble(7, 0.0);
                ps.addBatch();

                // Active overdue loan (Book 5 to Student 5, due 4 days ago -> $2.00)
                ps.setInt(1, 5);
                ps.setInt(2, 5);
                ps.setString(3, today.minusDays(18).format(fmt));
                ps.setString(4, today.minusDays(4).format(fmt));
                ps.setNull(5, java.sql.Types.VARCHAR);
                ps.setString(6, "OVERDUE");
                ps.setDouble(7, 2.00);
                ps.addBatch();

                ps.executeBatch();
            }

            conn.commit();
            System.out.println("Database initialization and seeding completed successfully.");
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
