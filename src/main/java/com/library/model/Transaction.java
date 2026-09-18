package com.library.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Entity representing a circulation loan transaction.
 */
public class Transaction {
    private int id;
    private int bookId;
    private int studentId;
    private String issueDate;
    private String dueDate;
    private String returnDate;
    private String status; // "ISSUED", "RETURNED", "OVERDUE"
    private double fineAmount;

    // Joined view fields for display
    private String bookTitle;
    private String bookIsbn;
    private String studentName;
    private String studentCode;

    public Transaction() {
    }

    public Transaction(int id, int bookId, int studentId, String issueDate, String dueDate, 
                       String returnDate, String status, double fineAmount) {
        this.id = id;
        this.bookId = bookId;
        this.studentId = studentId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.fineAmount = fineAmount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getBookTitle() {
        return bookTitle != null ? bookTitle : ("Book #" + bookId);
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public String getStudentName() {
        return studentName != null ? studentName : ("Student #" + studentId);
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    /**
     * Computes overdue days compared to today (if still issued),
     * or compared to return date if returned.
     */
    public long getDaysOverdue() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate due = LocalDate.parse(dueDate, formatter);
            LocalDate compareDate = (returnDate != null && !returnDate.isBlank()) 
                    ? LocalDate.parse(returnDate, formatter) 
                    : LocalDate.now();

            long days = ChronoUnit.DAYS.between(due, compareDate);
            return Math.max(0, days);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Checks if the transaction is currently overdue.
     */
    public boolean isOverdue() {
        return "OVERDUE".equalsIgnoreCase(status) || 
               ("ISSUED".equalsIgnoreCase(status) && getDaysOverdue() > 0);
    }
}
