package com.library.dao;

import com.library.db.DatabaseManager;
import com.library.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Book entities.
 * All queries strictly use PreparedStatement to ensure SQL injection prevention.
 */
public class BookDao {

    public boolean addBook(Book book) {
        String sql = """
            INSERT INTO books (isbn, title, author, category, total_copies, available_copies)
            VALUES (?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getIsbn().trim());
            ps.setString(2, book.getTitle().trim());
            ps.setString(3, book.getAuthor().trim());
            ps.setString(4, book.getCategory().trim());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        book.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding book: " + e.getMessage());
        }
        return false;
    }

    public boolean updateBook(Book book) {
        String sql = """
            UPDATE books
            SET isbn = ?, title = ?, author = ?, category = ?, total_copies = ?, available_copies = ?
            WHERE id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, book.getIsbn().trim());
            ps.setString(2, book.getTitle().trim());
            ps.setString(3, book.getAuthor().trim());
            ps.setString(4, book.getCategory().trim());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());
            ps.setInt(7, book.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteBook(int id) {
        String sql = "DELETE FROM books WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
        }
        return false;
    }

    public Book getBookById(int id) {
        String sql = "SELECT * FROM books WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBook(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching book by ID: " + e.getMessage());
        }
        return null;
    }

    public Book getBookByIsbn(String isbn) {
        String sql = "SELECT * FROM books WHERE LOWER(isbn) = LOWER(?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBook(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching book by ISBN: " + e.getMessage());
        }
        return null;
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all books: " + e.getMessage());
        }
        return books;
    }

    public List<Book> searchBooks(String query, String category) {
        List<Book> books = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM books WHERE 1=1 ");
        List<String> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(isbn) LIKE ?) ");
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
        }

        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All Categories")) {
            sql.append(" AND LOWER(category) = LOWER(?) ");
            params.add(category.trim());
        }

        sql.append(" ORDER BY title ASC;");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRowToBook(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching books: " + e.getMessage());
        }
        return books;
    }

    public boolean updateAvailableCopies(int bookId, int delta) {
        String sql = """
            UPDATE books
            SET available_copies = available_copies + ?
            WHERE id = ? AND (available_copies + ?) >= 0 AND (available_copies + ?) <= total_copies;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, delta);
            ps.setInt(2, bookId);
            ps.setInt(3, delta);
            ps.setInt(4, delta);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating available copies: " + e.getMessage());
        }
        return false;
    }

    public int getTotalBooksCount() {
        String sql = "SELECT COALESCE(SUM(total_copies), 0) FROM books;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total books count: " + e.getMessage());
        }
        return 0;
    }

    public List<String> getDistinctCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM books ORDER BY category ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String cat = rs.getString(1);
                if (cat != null && !cat.isBlank()) {
                    categories.add(cat);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting distinct categories: " + e.getMessage());
        }
        return categories;
    }

    private Book mapRowToBook(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt("id"),
            rs.getString("isbn"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("category"),
            rs.getInt("total_copies"),
            rs.getInt("available_copies")
        );
    }
}
