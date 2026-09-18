package com.library.model;

/**
 * Metric summary values displayed on the dashboard stat cards.
 */
public class DashboardMetrics {
    private int totalBooks;
    private int totalMembers;
    private int booksIssued;
    private double overdueFines;
    private int overdueCount;

    public DashboardMetrics() {
    }

    public DashboardMetrics(int totalBooks, int totalMembers, int booksIssued, double overdueFines, int overdueCount) {
        this.totalBooks = totalBooks;
        this.totalMembers = totalMembers;
        this.booksIssued = booksIssued;
        this.overdueFines = overdueFines;
        this.overdueCount = overdueCount;
    }

    public int getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(int totalBooks) {
        this.totalBooks = totalBooks;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(int totalMembers) {
        this.totalMembers = totalMembers;
    }

    public int getBooksIssued() {
        return booksIssued;
    }

    public void setBooksIssued(int booksIssued) {
        this.booksIssued = booksIssued;
    }

    public double getOverdueFines() {
        return overdueFines;
    }

    public void setOverdueFines(double overdueFines) {
        this.overdueFines = overdueFines;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }
}
