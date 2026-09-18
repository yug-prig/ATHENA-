# PATEL YUG MAHESHKUMAR
# 25BAI10675
# Athena — Modern Library Management System

A complete, production-ready desktop application built in **Java (JDK 17+)** and **JavaFX** with a custom sleek dark-mode aesthetic (`#1E1E2E` Deep Slate with `#10B981` Emerald Green and `#3B82F6` Electric Blue accents). Backed by an embedded **SQLite** database via JDBC, strict `PreparedStatement` SQL safety, and a layered **DAO (Data Access Object)** architecture.

---

## Key Highlights & Features

### 1. Modern Dark-Mode Dashboard & UX
* **Deep Slate Aesthetic**: Rooted in `#1E1E2E` background, `#252538` elevated panels, and `#363654` structural borders.
* **Collapsible Sidebar**: Smooth navigation sidebar with icon + label display, active route indicator, and toggle button (☰).
* **Live Stat Cards**: Quick metric cards displaying Total Books, Registered Members, Books Currently Issued, and Overdue Fines ($).
* **Custom Styled TableView**: Rounded corners, dark headers, alternating zebra stripes, custom scrollbars, and color-coded status pills:
  * 🟢 **Available / Returned**: Green badge (`#10B981`)
  * 🟠 **Issued / In Use**: Amber badge (`#F59E0B`)
  * 🔴 **Overdue / Out of Stock**: Crimson badge (`#EF4444`)
* **Real-Time Live Search & Multi-Filters**: Instant client-side and database-filtered search without needing to press Enter.
* **Live System Header**: Real-time digital clock and SQLite connection heartbeat indicator.

### 2. Core Functional Modules
* **Dashboard**: High-level telemetry, quick-issue shortcuts, and real-time feed of recent circulation activities.
* **Book Inventory & Catalogue**:
  * Add, update, search, and delete books.
  * Real-time stock tracking (`total_copies` vs `available_copies`).
  * Instant search by Title, Author, or ISBN with Category dropdown filter.
  * Modal forms with comprehensive validation.
* **Member / Student Registry**:
  * Register students with unique Student Codes, email, and department.
  * Search by name, student code, or email with Department filter.
  * Inspect individual borrowing histories and active loans per member.
* **Circulation Desk (Check-Out & Check-In)**:
  * **Check-Out Process**: Validates copy availability in real-time, creates transaction record, sets default 14-day due date, and atomically decrements `available_copies`.
  * **Check-In Process**: Marks transaction as `RETURNED`, increments `available_copies`, and calculates overdue fines at **$0.50 per day past the due date**.

### 3. Database & Architecture
* **Zero Configuration**: Embedded SQLite database (`library.db`) automatically initializes and seeds realistic sample records on first launch.
* **Layered DAO Pattern**:
  * `com.library.db`: Connection management, PRAGMAs, and schema migrations.
  * `com.library.model`: Strongly-typed entity representations (`Book`, `Student`, `Transaction`, `DashboardMetrics`).
  * `com.library.dao`: PreparedStatement data access operations with SQL injection defense.
  * `com.library.service`: Business logic orchestration and atomic database transactions (ACID commit/rollback).
  * `com.library.ui`: Modern JavaFX view controllers and custom components.
  * `com.library.app`: Application bootstrap and lifecycle management.

---

## Project Structure

```
vityarthi-java/
├── pom.xml                                  # Maven dependencies & JavaFX configuration
├── README.md                                # Project documentation & run guide
├── library.db                               # Embedded SQLite database (auto-generated)
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── library/
        │           ├── app/
        │           │   ├── MainApp.java             # JavaFX Application entrypoint
        │           │   └── Launcher.java            # Bootstrap runner (no VM flags needed)
        │           ├── db/
        │           │   └── DatabaseManager.java     # SQLite connection & schema initializer
        │           ├── model/
        │           │   ├── Book.java                # Book entity model
        │           │   ├── Student.java             # Student/Member model
        │           │   ├── Transaction.java         # Circulation loan transaction model
        │           │   └── DashboardMetrics.java    # Aggregated telemetry data
        │           ├── dao/
        │           │   ├── BookDao.java             # Book CRUD with PreparedStatements
        │           │   ├── StudentDao.java          # Student CRUD & loan counts
        │           │   └── TransactionDao.java      # Loan tracking, overdue refresh, fines
        │           ├── service/
        │           │   └── CirculationService.java  # Business logic & atomic checkout/returns
        │           └── ui/
        │               ├── MainView.java            # App layout shell, collapsible sidebar, router
        │               ├── DashboardView.java       # Metric cards & recent transactions table
        │               ├── BookManagementView.java  # Book catalogue CRUD & stock pills
        │               ├── StudentRegistryView.java # Member registration & loan inspector
        │               ├── CirculationView.java     # Check-out desk & check-in fine calculator
        │               └── components/
        │                   ├── StatCard.java        # Reusable metric card component
        │                   └── DialogUtils.java     # Dark-mode styled modal alerts
        └── resources/
            ├── styles.css                           # Full modern dark-mode stylesheet
            └── schema.sql                           # SQLite DDL schema reference
```

---

## Database Schema Reference

```sql
-- Books Table
CREATE TABLE books (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    category TEXT NOT NULL,
    total_copies INTEGER NOT NULL DEFAULT 1,
    available_copies INTEGER NOT NULL DEFAULT 1
);

-- Students / Members Table
CREATE TABLE students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_code TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    department TEXT NOT NULL
);

-- Transactions Table
CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    issue_date TEXT NOT NULL,
    due_date TEXT NOT NULL,
    return_date TEXT,
    status TEXT NOT NULL, -- 'ISSUED', 'RETURNED', 'OVERDUE'
    fine_amount REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);
```

---

## How to Build & Run

### Prerequisites
- **JDK 17 or higher** (Tested with JDK 17, 21, and 26).
- **Apache Maven 3.8+** (or an IDE like IntelliJ IDEA / Eclipse / VS Code with Maven support).

---

### Option A: Running via IntelliJ IDEA (Recommended & Easiest)
1. Open **IntelliJ IDEA**.
2. Select **File → Open** and choose the `vityarthi-java` project folder.
3. IntelliJ will automatically detect `pom.xml` and download all dependencies (JavaFX 21 & SQLite JDBC).
4. Navigate to `src/main/java/com/library/app/Launcher.java`.
5. Click the green **Run** button next to `main()`.

---

### Option B: Running via Maven Command Line
Open a terminal (PowerShell or Command Prompt) in the project directory:

```bash
# 1. Run directly via the JavaFX Maven Plugin:
mvn clean javafx:run

# 2. Or build an executable Fat JAR:
mvn clean package

# 3. Run the generated Fat JAR:
java -jar target/library-management-system-1.0.0.jar
```

---

### Option C: Running via VS Code
1. Open the project folder in **Visual Studio Code**.
2. Install the **Extension Pack for Java** and **JavaFX Support** if prompted.
3. Open `Launcher.java` and click **Run** above `public static void main`.

---

## Sample Data Included
On the first launch, the application automatically seeds:
- **8 Diverse Technical Books** (e.g. *Effective Java*, *Clean Code*, *Design Patterns*, *CLRS*, *Designing Data-Intensive Applications*).
- **5 Registered Members** across Computer Science, Software Engineering, IT, and Data Science.
- **5 Circulation Transactions** demonstrating all statuses:
  - Active 14-day loan in good standing.
  - Active **Overdue loan** accruing fines at **$0.50/day**.
  - Returned book on time with $0.00 fine.
