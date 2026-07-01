# Student Project Guide — MediCare Pharmacy Management System
## BS Computer Science — 4th Semester Project Documentation

---

## 📋 Project Overview

**Project Name:** MediCare Pharmacy Management System  
**Type:** Desktop Enterprise Application  
**Semester:** 4th Semester BS Computer Science  
**Domain:** Healthcare / Pharmacy Management  
**Complexity Level:** Advanced (Full-Stack Desktop Application)

### What This System Does

MediCare is a complete pharmacy management solution that handles:
- **Sales & Billing (POS)** — Process customer purchases, generate receipts
- **Inventory Management** — Track medication batches, expiry dates, stock levels
- **Patient Records** — Maintain patient profiles with medical history
- **Drug Catalogue** — Manage medication database with pricing, categories
- **Supplier Management** — Track suppliers and purchase orders
- **Staff Management** — Role-based access control (Admin, Pharmacist, Cashier)
- **Dashboard Analytics** — Revenue tracking, sales trends, alerts
- **Database Management** — Backup, restore, multi-database support

---

## 🏗️ System Architecture

### Architecture Type: **Hybrid Desktop Application**

```
┌─────────────────────────────────────────────────────────┐
│                    JavaFX Desktop Window                 │
│  ┌───────────────────────────────────────────────────┐  │
│  │            WebView Component (Browser)            │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │       HTML + CSS + JavaScript Frontend      │  │  │
│  │  │  • Single Page Application (SPA)            │  │  │
│  │  │  • Vanilla JavaScript (no React/Vue)        │  │  │
│  │  │  • Communicates via REST API                │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
│         ▲                                   │            │
│         │ HTTP Requests                     │ Responses  │
│         │                                   ▼            │
│  ┌───────────────────────────────────────────────────┐  │
│  │         Spring Boot Backend (Embedded)            │  │
│  │  • REST API Endpoints                             │  │
│  │  • JWT Authentication                             │  │
│  │  • Business Logic                                 │  │
│  │  • Database Access (JPA/Hibernate)                │  │
│  └───────────────────────────────────────────────────┘  │
│                        │                                 │
│                        ▼                                 │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Database Layer                       │  │
│  │  MySQL (Primary) / SQL Server / H2 (Fallback)    │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Why This Architecture?

1. **Desktop Experience** — Native window with JavaFX, not browser-based
2. **Web Technologies** — HTML/CSS/JS easier to design than JavaFX UI
3. **Offline Capable** — Runs completely on local machine, no internet needed
4. **Cross-Platform** — Java runs on Windows, Mac, Linux
5. **Modern Stack** — Uses industry-standard Spring Boot framework

---

## 💻 Technology Stack

### Backend Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 17 | Core programming language |
| **Spring Boot** | 3.2.5 | Backend framework (REST API, dependency injection) |
| **Spring Security** | 3.2.5 | Authentication & authorization |
| **Spring Data JPA** | 3.2.5 | Database abstraction layer |
| **Hibernate** | 6.x | ORM (Object-Relational Mapping) |
| **MySQL** | 8.x | Primary database |
| **H2 Database** | 2.x | In-memory fallback database |
| **JWT (JSON Web Tokens)** | 0.11.5 | Stateless authentication |
| **Lombok** | 1.18.46 | Reduce boilerplate code |
| **MapStruct** | 1.5.5 | DTO mapping |
| **Maven** | 3.9.6 | Build tool & dependency management |

### Frontend Technologies

| Technology | Purpose |
|------------|---------|
| **HTML5** | Structure |
| **CSS3** | Styling (custom design system, no frameworks) |
| **Vanilla JavaScript** | Interactivity (NO React/Vue/Angular) |
| **Fetch API** | HTTP requests to backend |

### Desktop Wrapper

| Technology | Version | Purpose |
|------------|---------|---------|
| **JavaFX** | 21.0.1 | Native window & WebView component |

### Tools & Others

- **Swagger UI** (OpenAPI 3.0) — API documentation
- **Actuator** — Health monitoring
- **Git** — Version control
- **IntelliJ IDEA / VS Code** — IDE

---

## 📂 Project Structure Explained

```
pharmacy-system/
│
├── src/main/java/com/medicare/
│   │
│   ├── PharmacyApplication.java           ← Main entry point
│   │   • Detects if running in desktop or server mode
│   │   • Auto-detects available database (MySQL → SQL Server → H2)
│   │   • Starts Spring Boot
│   │
│   ├── DesktopApp.java                    ← JavaFX window launcher
│   │   • Creates the desktop window
│   │   • Shows loading screen with animation
│   │   • Embeds WebView (browser) inside window
│   │   • Starts Spring Boot in background thread
│   │   • Loads frontend when backend is ready
│   │
│   ├── DesktopServerBridge.java           ← Communication bridge
│   │   • Stores the random port Spring Boot starts on
│   │   • Provides URLs for frontend to connect to backend
│   │
│   ├── config/
│   │   ├── SecurityConfig.java            ← Spring Security setup
│   │   │   • JWT authentication
│   │   │   • Role-based access control (ADMIN, PHARMACIST, CASHIER)
│   │   │   • CORS configuration for localhost only
│   │   │
│   │   ├── DataInitializer.java           ← Seeds default users
│   │   │   • Creates admin/pharmacist/cashier on first run
│   │   │   • Default passwords: admin123, pharma123, cash123
│   │   │
│   │   ├── DesktopServerReadyListener.java ← Detects when Spring Boot ready
│   │   │   • Captures the actual port Spring Boot binds to
│   │   │   • Signals DesktopApp that backend is ready
│   │   │
│   │   └── OpenApiConfig.java             ← Swagger documentation config
│   │
│   ├── controller/                        ← REST API Endpoints
│   │   ├── AuthController.java            → POST /api/auth/login
│   │   ├── DashboardController.java       → GET /api/dashboard/* (stats, charts)
│   │   ├── DrugController.java            → CRUD /api/drugs
│   │   ├── InventoryController.java       → CRUD /api/inventory
│   │   ├── PatientController.java         → CRUD /api/patients
│   │   ├── SaleController.java            → POST /api/sales (POS billing)
│   │   ├── SupplierController.java        → CRUD /api/suppliers
│   │   ├── PurchaseOrderController.java   → CRUD /api/purchase-orders
│   │   ├── UserController.java            → CRUD /api/users (staff management)
│   │   └── BackupController.java          → GET/POST /api/backup (database ops)
│   │
│   ├── dto/                               ← Data Transfer Objects
│   │   • Separate request/response objects for API
│   │   • Example: LoginRequest, LoginResponse, DrugDTO
│   │
│   ├── entity/                            ← Database Models (JPA Entities)
│   │   ├── User.java                      → Users table
│   │   ├── Drug.java                      → Drug catalog
│   │   ├── Inventory.java                 → Stock batches
│   │   ├── Patient.java                   → Patient records
│   │   ├── Sale.java                      → Transactions
│   │   ├── SaleItem.java                  → Line items in a sale
│   │   ├── Supplier.java                  → Supplier directory
│   │   └── PurchaseOrder.java             → Purchase orders
│   │
│   ├── repository/                        ← Database Access Layer
│   │   • Spring Data JPA interfaces
│   │   • Example: DrugRepository extends JpaRepository<Drug, Long>
│   │   • No SQL needed — Spring generates queries automatically
│   │
│   ├── service/                           ← Business Logic
│   │   ├── impl/                          → Implementations
│   │   │   ├── DrugServiceImpl.java
│   │   │   ├── InventoryServiceImpl.java
│   │   │   └── ... (one per entity)
│   │   │
│   │   └── [Entity]Service.java           → Interfaces
│   │
│   ├── security/                          ← Authentication Components
│   │   ├── JwtAuthFilter.java             → Intercepts requests, validates JWT
│   │   ├── JwtUtils.java                  → Generates & parses JWT tokens
│   │   └── UserDetailsServiceImpl.java    → Loads user for Spring Security
│   │
│   └── exception/                         ← Error Handling
│       ├── GlobalExceptionHandler.java    → Catches all exceptions, returns JSON
│       └── [Custom]Exception.java         → Domain-specific exceptions
│
├── src/main/resources/
│   ├── application.properties             ← Main config (MySQL)
│   ├── application-h2.properties          ← H2 fallback config
│   ├── application-sqlserver.properties   ← SQL Server config
│   ├── schema.sql                         ← Sample data (for H2 mode)
│   │
│   └── static/                            ← Frontend Files
│       ├── index.html                     → Single-page app HTML
│       │
│       ├── css/
│       │   └── styles.css                 → All styling (design system)
│       │
│       ├── js/
│       │   ├── api.js                     → Fetch wrapper (HTTP client)
│       │   ├── auth.js                    → Login/logout logic
│       │   ├── app.js                     → Main app initialization, tab switching
│       │   ├── dashboard.js               → Dashboard stats & charts
│       │   ├── catalog.js                 → Drug catalog management
│       │   ├── inventory.js               → Inventory batch management
│       │   ├── pos.js                     → Point of Sale (billing)
│       │   ├── patients.js                → Patient registry
│       │   ├── suppliers.js               → Supplier & purchase orders
│       │   ├── staff.js                   → Staff management
│       │   ├── backup.js                  → Database backup/restore
│       │   └── receipt-pdf.js             → PDF generation for receipts
│       │
│       └── assets/
│           └── logo.png                   → App logo
│
├── pom.xml                                ← Maven build configuration
│   • Defines all dependencies
│   • Build plugins (compiler, Spring Boot, JavaFX module setup)
│
├── run.bat                                ← Windows launcher script
├── README.md                              ← Project overview
└── MASTER_PROMPT.md                       ← AI prompt to recreate for other domains
```

---

## 🔐 How Authentication Works (JWT Flow)

### Step-by-Step Flow

```
┌─────────┐                                    ┌─────────┐
│ Frontend│                                    │ Backend │
│(Browser)│                                    │(Spring) │
└────┬────┘                                    └────┬────┘
     │                                              │
     │  1. POST /api/auth/login                    │
     │     { username: "admin", password: "..." }  │
     ├────────────────────────────────────────────>│
     │                                              │
     │                    2. Check credentials      │
     │                       against database       │
     │                                              ├──┐
     │                    3. Generate JWT token     │  │
     │                       (expires in 24h)       │<─┘
     │                                              │
     │  4. Response                                 │
     │<────────────────────────────────────────────┤
     │     { token: "eyJhbG...",                    │
     │       user: { id, username, roles } }        │
     │                                              │
     │  5. Store token in memory (JS variable)     │
     ├──┐                                           │
     │<─┘                                           │
     │                                              │
     │  6. GET /api/drugs                           │
     │     Header: Authorization: Bearer eyJhbG...  │
     ├────────────────────────────────────────────>│
     │                                              │
     │                    7. JwtAuthFilter          │
     │                       validates token        │
     │                                              ├──┐
     │                    8. Extract user from JWT  │  │
     │                       and set in context     │<─┘
     │                                              │
     │  9. Response (if authorized)                 │
     │<────────────────────────────────────────────┤
     │     [ { drug1 }, { drug2 }, ... ]           │
     │                                              │
```

### Key Concepts

- **Stateless:** Server doesn't store session — all auth info is in the JWT token
- **Token Storage:** In-memory JS variable (not localStorage, more secure)
- **Token Expiry:** 24 hours (configurable in application.properties)
- **Roles:** ADMIN, PHARMACIST, CASHIER — checked on every endpoint

---

## 🗃️ Database Schema (Main Tables)

### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    license_number VARCHAR(50),
    roles VARCHAR(255),  -- stored as comma-separated: ROLE_ADMIN,ROLE_PHARMACIST
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Drugs Table (Catalog)
```sql
CREATE TABLE drugs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    category VARCHAR(100),
    dosage_form VARCHAR(50),  -- tablet, capsule, syrup
    strength VARCHAR(50),     -- 500mg, 10ml
    mrp DECIMAL(10,2),
    gst_percentage DECIMAL(5,2),
    schedule VARCHAR(50),     -- H, H1, X (drug scheduling)
    status VARCHAR(20),       -- ACTIVE, DISCONTINUED
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Inventory Table (Batches)
```sql
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    drug_id BIGINT,
    batch_code VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    expiry_date DATE,
    buying_price DECIMAL(10,2),
    selling_price DECIMAL(10,2),
    shelf_location VARCHAR(50),
    supplier_id BIGINT,
    received_date DATE,
    FOREIGN KEY (drug_id) REFERENCES drugs(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);
```

### Sales Table (Transactions)
```sql
CREATE TABLE sales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_number VARCHAR(50) UNIQUE,
    patient_id BIGINT,
    subtotal DECIMAL(10,2),
    gst_amount DECIMAL(10,2),
    discount_percentage DECIMAL(5,2),
    total_amount DECIMAL(10,2),
    payment_method VARCHAR(50),  -- CASH, CARD, MOBILE_BANKING
    amount_paid DECIMAL(10,2),
    change_returned DECIMAL(10,2),
    cashier_id BIGINT,
    sale_date TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (cashier_id) REFERENCES users(id)
);
```

### Sale Items Table (Line Items)
```sql
CREATE TABLE sale_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_id BIGINT,
    inventory_id BIGINT,
    drug_name VARCHAR(200),
    batch_code VARCHAR(100),
    quantity INT,
    unit_price DECIMAL(10,2),
    line_total DECIMAL(10,2),
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (inventory_id) REFERENCES inventory(id)
);
```

---

## 🎨 How Frontend Works

### Single Page Application (SPA) Pattern

The frontend is a **single HTML file** with multiple "sheets" (views) that show/hide:

```html
<div class="app-container">
  <aside class="sidebar">...</aside>  <!-- Always visible -->
  
  <main class="main-content">
    <header class="top-header">...</header>  <!-- Always visible -->
    
    <!-- Only ONE sheet is visible at a time (class="active") -->
    <section class="viewport-sheet active" id="sheet-dashboard">...</section>
    <section class="viewport-sheet" id="sheet-pos">...</section>
    <section class="viewport-sheet" id="sheet-inventory">...</section>
    <section class="viewport-sheet" id="sheet-drugs">...</section>
    ...
  </main>
</div>
```

### Tab Switching Logic (app.js)

```javascript
const App = {
  switchTab(tabName) {
    // Hide all sheets
    document.querySelectorAll('.viewport-sheet').forEach(sheet => {
      sheet.classList.remove('active');
    });
    
    // Show the clicked sheet
    document.getElementById(`sheet-${tabName}`).classList.add('active');
    
    // Update sidebar menu active state
    document.querySelectorAll('.menu-item').forEach(item => {
      item.classList.remove('active');
    });
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    
    // Load data for that sheet
    if (tabName === 'dashboard') Dashboard.load();
    if (tabName === 'drugs') Catalog.load();
    // ... etc
  }
};
```

### API Communication Pattern (api.js)

```javascript
const Api = {
  async get(path) {
    const response = await fetch(`${API_BASE}${path}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      }
    });
    if (!response.ok) throw new Error('Request failed');
    return await response.json();
  },
  
  async post(path, body) {
    const response = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    });
    if (!response.ok) throw new Error('Request failed');
    return await response.json();
  }
};
```

---

## 🚀 How to Run the Project

### Prerequisites

1. **Java Development Kit (JDK) 17**
   - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/#java17) or [Adoptium](https://adoptium.net/)
   - Verify: `java -version` (should show 17.x.x)

2. **Maven 3.9+** (bundled in `maven-dist/` folder, or install globally)
   - Verify: `mvn -version`

3. **MySQL 8.x** (optional — H2 fallback available)
   - Download from [MySQL Downloads](https://dev.mysql.com/downloads/mysql/)
   - Create database: `CREATE DATABASE medicare_db;`
   - Update `src/main/resources/application.properties` with your MySQL password

4. **IDE** (optional but recommended)
   - IntelliJ IDEA Community Edition (best for Java)
   - VS Code with Java Extension Pack

### Build Steps

#### Option 1: Using IntelliJ IDEA

1. Open IntelliJ IDEA → File → Open → Select `pharmacy-system` folder
2. Wait for Maven to download dependencies (first time takes 5-10 minutes)
3. Right-click `pom.xml` → Maven → Reload Project
4. Right-click `PharmacyApplication.java` → Run

#### Option 2: Using Command Line

```cmd
cd C:\Users\User\Desktop\pharmacy-system

REM Clean and build
mvn clean package

REM Run the application
java --module-path target/javafx-lib ^
     --add-modules javafx.controls,javafx.web,javafx.graphics,javafx.base ^
     -Dprism.order=sw ^
     -jar target/medicare-system-1.0.0.jar
```

#### Option 3: Using run.bat (Windows)

```cmd
cd C:\Users\User\Desktop\pharmacy-system
run.bat
```

### First Launch

1. **Loading Screen** — Shows animated logo and progress bar (5-10 seconds)
2. **Login Screen** — Default users:
   - Username: `admin` / Password: `admin123` (full access)
   - Username: `pharmacist` / Password: `pharma123` (pharmacy operations)
   - Username: `cashier` / Password: `cash123` (POS only)
3. **Dashboard** — Main application loads

### API Documentation (Swagger)

Open browser: `http://localhost:8080/api/swagger-ui.html`

---

## 🎓 Key Learning Outcomes for Students

### 1. **Full-Stack Development**
   - Backend: REST API design, database modeling, business logic
   - Frontend: SPA architecture, DOM manipulation, API integration
   - Integration: How frontend and backend communicate

### 2. **Design Patterns Used**
   - **MVC (Model-View-Controller)** — Separation of concerns
   - **DTO Pattern** — Data Transfer Objects for API layer
   - **Repository Pattern** — Database abstraction
   - **Service Layer Pattern** — Business logic isolation
   - **Dependency Injection** — Spring framework core concept

### 3. **Security Concepts**
   - **JWT Authentication** — Token-based stateless auth
   - **Role-Based Access Control (RBAC)** — Permissions per role
   - **Password Hashing** — BCrypt encryption
   - **CORS** — Cross-Origin Resource Sharing

### 4. **Database Skills**
   - **ORM (Object-Relational Mapping)** — JPA/Hibernate
   - **Database Relationships** — One-to-Many, Many-to-One
   - **Transactions** — ACID properties in sales operations
   - **Query Optimization** — JPA derived queries

### 5. **Software Engineering Principles**
   - **Separation of Concerns** — Layered architecture
   - **DRY (Don't Repeat Yourself)** — Reusable components
   - **SOLID Principles** — Clean code practices
   - **Version Control** — Git workflows

---

## 📊 Project Presentation Tips

### Demo Flow (15-20 minutes)

1. **Introduction (2 min)**
   - Project name, domain, problem statement
   - "MediCare solves inventory, billing, and patient management challenges in pharmacies"

2. **Architecture Overview (3 min)**
   - Show the architecture diagram
   - Explain hybrid desktop approach (JavaFX + Spring Boot + WebView)
   - Mention tech stack

3. **Live Demo (10 min)**
   - Start the application (show loading animation)
   - Login as Admin
   - Navigate through all modules:
     - Dashboard (KPIs, charts)
     - POS (add items, process sale, print receipt)
     - Inventory (show batch tracking)
     - Drug Catalog
     - Patient Records
   - Show role-based access (logout, login as Cashier — limited menu)

4. **Code Walkthrough (3 min)**
   - Show one complete flow: Drug Entity → Repository → Service → Controller
   - Show JWT authentication code in SecurityConfig
   - Show API call in frontend (fetch in api.js)

5. **Q&A (2 min)**
   - Common questions:
     - "Why not use React?" → Vanilla JS is lighter, easier to debug, better for desktop
     - "Database choice?" → MySQL for real deployment, H2 for demo/offline use
     - "Security?" → JWT tokens, role-based access, BCrypt passwords

### Documentation to Submit

1. **Project Report** (15-25 pages)
   - Introduction & Problem Statement
   - Literature Review (existing pharmacy systems)
   - System Analysis & Design
     - Use Case Diagrams
     - Class Diagrams
     - Sequence Diagrams
     - ER Diagrams
   - Implementation Details
     - Technology stack explanation
     - Module descriptions
     - Code snippets (key algorithms)
   - Testing (screenshots of each module)
   - Conclusion & Future Work

2. **User Manual** (5-10 pages)
   - Installation guide
   - How to use each module
   - Screenshots with annotations

3. **Source Code** (GitHub repository)
   - Clean, commented code
   - README with setup instructions
   - .gitignore properly configured

---

## 🔧 Customization Ideas for Your Project

### Make It Unique — Add These Features

1. **SMS Notifications** (Easy)
   - Send SMS to patient when prescription is ready
   - Use Twilio API or similar

2. **Barcode Scanning** (Medium)
   - Scan drug barcodes in POS for faster billing
   - Use ZXing library

3. **Reports Module** (Easy)
   - Daily/Monthly/Yearly sales reports
   - Stock valuation reports
   - Export to PDF/Excel using Apache POI

4. **Dark Mode** (Easy)
   - Toggle between light/dark themes
   - Duplicate CSS variables with dark colors

5. **Multi-Language Support** (Medium)
   - Add language switcher (English/Urdu/Arabic)
   - Use i18n (internationalization)

6. **Medicine Interaction Checker** (Hard)
   - Warn if patient is prescribed conflicting drugs
   - Requires drug interaction database

7. **Prescription Image Upload** (Medium)
   - Allow attaching scanned prescription images
   - Store in database as BLOB or filesystem

8. **Low Stock Email Alerts** (Easy)
   - Send email when stock falls below threshold
   - Use Spring Boot Mail

---

## 📚 Study Resources

### Spring Boot
- [Official Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Baeldung Spring Tutorials](https://www.baeldung.com/spring-boot)
- YouTube: "Spring Boot Tutorial for Beginners" by Amigoscode

### JWT Authentication
- [JWT.io](https://jwt.io/) — Understanding JWT
- [Spring Security JWT Guide](https://www.baeldung.com/spring-security-jwt)

### JavaFX
- [OpenJFX Docs](https://openjfx.io/)
- JavaFX WebView: [Official Tutorial](https://docs.oracle.com/javafx/2/webview/jfxpub-webview.htm)

### JPA/Hibernate
- [Hibernate ORM Docs](https://hibernate.org/orm/documentation/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

## ❓ Common Issues & Troubleshooting

### Issue 1: "java: cannot find symbol" errors
**Solution:** Run `mvn clean install` to regenerate MapStruct/Lombok code

### Issue 2: JavaFX modules not found
**Solution:** Check JVM arguments include:
```
--module-path target/javafx-lib
--add-modules javafx.controls,javafx.web
```

### Issue 3: Can't connect to MySQL
**Solution:** 
- Ensure MySQL service is running
- Check username/password in `application.properties`
- Create database: `CREATE DATABASE medicare_db;`
- Or let app use H2 fallback (no config needed)

### Issue 4: Port 8080 already in use
**Solution:** Change port in `application.properties`:
```properties
server.port=9090
```

### Issue 5: "CORS error" in browser console
**Solution:** Already handled in SecurityConfig (localhost allowed)

---

## 📝 Project Evaluation Rubric (What Professors Look For)

| Criteria | Points | What to Show |
|----------|--------|--------------|
| **Functionality** | 25% | All modules working, no crashes |
| **Code Quality** | 20% | Clean code, proper structure, comments |
| **UI/UX Design** | 15% | Professional look, easy to use |
| **Documentation** | 20% | Complete report, diagrams, user manual |
| **Innovation** | 10% | Unique features beyond basic CRUD |
| **Presentation** | 10% | Clear demo, confident explanation |

---

## 🎯 Your Action Plan

### Week 1: Understanding
- [ ] Read this entire guide
- [ ] Watch Spring Boot & JavaFX tutorials
- [ ] Set up development environment (JDK, Maven, MySQL, IDE)

### Week 2: Setup & First Run
- [ ] Clone/open the project
- [ ] Resolve all Maven dependencies
- [ ] Successfully run the application
- [ ] Test all features

### Week 3: Study the Code
- [ ] Trace one complete flow (e.g., Drug CRUD)
- [ ] Understand how JWT authentication works
- [ ] Understand how API calls work in frontend

### Week 4: Customization
- [ ] Add at least ONE new feature (e.g., Reports or Dark Mode)
- [ ] Change branding (app name, colors, logo)
- [ ] Add your name/university in About section

### Week 5-6: Documentation
- [ ] Write project report with diagrams
- [ ] Create user manual with screenshots
- [ ] Prepare presentation slides
- [ ] Practice demo (time yourself — under 15 min)

### Week 7: Testing & Polish
- [ ] Test all features thoroughly
- [ ] Fix any bugs
- [ ] Clean up code and add comments
- [ ] Finalize documentation

---

## 🏆 Conclusion

This project demonstrates:
✅ Full-stack development skills  
✅ Modern enterprise architecture  
✅ Security best practices  
✅ Professional UI/UX design  
✅ Real-world problem solving  

**You now have:**
- A complete, working desktop application
- Production-quality code to learn from
- A strong project for your semester evaluation
- Portfolio material for job applications

**Good luck with your project! 🚀**

---

*For questions or issues, refer to:*
- `README.md` — Quick start guide
- `MASTER_PROMPT.md` — Recreate for other domains
- Swagger UI: `http://localhost:8080/api/swagger-ui.html` — API docs
