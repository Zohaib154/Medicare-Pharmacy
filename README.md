# MediCare — Pharmacy Management System

A complete **Spring Boot 3 + MySQL** backend for pharmacy management, featuring JWT authentication, role-based access control, inventory tracking, prescriptions, billing, and an SQL dashboard.

---

## Tech Stack

| Layer         | Technology                          |
|---------------|-------------------------------------|
| Language      | Java 17                             |
| Framework     | Spring Boot 3.2.5                   |
| Database      | MySQL 8.x                           |
| ORM           | Spring Data JPA / Hibernate         |
| Security      | Spring Security + JWT (jjwt 0.11.5) |
| API Docs      | SpringDoc OpenAPI (Swagger UI)      |
| Build         | Maven 3.9+                          |
| Utilities     | Lombok, MapStruct                   |

---

## Project Structure

Keep the repo lean: source lives under `src/`, build output under `target/` (gitignored). Bundled Maven in `maven-dist/` is for offline `run.bat` only (also gitignored). Static UI assets: `src/main/resources/static/` (CSS, JS, `assets/logo.png`).

```
medicare-system/
├── pom.xml
├── MediCareLauncher.exe                 # Standalone desktop launcher (no console)
├── Launch-MediCare.bat                  # Launcher fallback / helper
├── run.bat                              # Build + desktop launch (console)
├── build-dist.ps1                       # jpackage → target/dist/MediCare/MediCare.exe
└── src/main/java/com/medicare/
    ├── PharmacyApplication.java          # Entry point (desktop by default)
    ├── DesktopApp.java                   # JavaFX WebView shell
    ├── config/
    │   ├── DataInitializer.java          # Seeds default users
    │   ├── OpenApiConfig.java            # Swagger config
    │   └── SecurityConfig.java           # JWT + CORS + RBAC
    ├── controller/
    │   ├── AuthController.java           # POST /auth/login, /register
    │   ├── DashboardController.java      # GET /dashboard
    │   ├── DrugController.java           # CRUD /drugs
    │   ├── InventoryController.java      # /inventory (stock + alerts)
    │   ├── PatientController.java        # CRUD /patients
    │   ├── PrescriptionController.java   # /prescriptions (create, dispense)
    │   ├── PurchaseOrderController.java  # /purchase-orders
    │   ├── SaleController.java           # /sales (POS + refund)
    │   └── SupplierController.java       # CRUD /suppliers
    ├── dto/                              # Request/response DTOs
    ├── entity/                           # JPA entities
    ├── exception/                        # Custom exceptions + global handler
    ├── repository/                       # Spring Data JPA repositories
    ├── security/
    │   ├── JwtUtils.java
    │   ├── JwtAuthFilter.java
    │   └── UserDetailsServiceImpl.java
    └── service/impl/                     # Business logic services
```

---

## Quick Start (Desktop — recommended)

MediCare is a **standalone desktop application**. Double-click to open a native window — no browser, no URLs, no separate server window.

### Option A — Packaged desktop app (production)

After building the distribution:

```powershell
.\build-dist.ps1
```

Then double-click:

```
target\dist\MediCare\MediCare.exe
```

### Option B — Developer desktop launch

From the project root on Windows (preferred):

```
MediCareLauncher.exe
```

Fallback:

```bat
Launch-MediCare.bat
```

or:

```bat
run.bat
```

Both scripts build the JAR if needed and launch the JavaFX desktop shell with the embedded API.

### Headless API / server mode (developers only)

To run only the REST API without the desktop window:

```bash
mvn clean package -DskipTests
java -jar target/medicare-system-1.0.0.jar --headless
```

The API is then available at **http://localhost:8080/api** (for Swagger, integration tests, etc.).

### Database

- **MySQL** (optional): if MySQL is running on port 3306, MediCare uses it automatically.
- **Portable H2 fallback**: if MySQL is not detected, MediCare starts with an embedded H2 database — no manual setup required for desktop use.

To use MySQL explicitly, edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE
```

Create the database if needed:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS medicare_db;"
```

### Build from source

```bash
mvn clean package -DskipTests
```

### Test desktop launch on Windows

1. Run `mvn clean package -DskipTests`
2. Run `run.bat` or `Launch-MediCare.bat`
3. Confirm a window titled **MediCare Pharmacy Management System** opens (no browser)
4. Log in with default credentials below

---

## Default Credentials (auto-created on first run)

| Role            | Username     | Password    |
|-----------------|--------------|-------------|
| Admin           | `admin`      | `admin123`  |
| Pharmacist      | `pharmacist` | `pharma123` |
| Cashier         | `cashier`    | `cash123`   |

---

## API Documentation

In **headless server mode** (`--headless`), open Swagger UI at **http://localhost:8080/api/swagger-ui.html**. The desktop app does not require opening a browser.

### Authentication

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "admin",
  "fullName": "System Administrator",
  "roles": ["ROLE_ADMIN", "ROLE_PHARMACIST", "ROLE_STORE_MANAGER"]
}
```

Use `Authorization: Bearer <accessToken>` on all protected endpoints.

---

## Key API Endpoints

### Dashboard
| Method | Endpoint       | Description              |
|--------|----------------|--------------------------|
| GET    | `/dashboard`   | KPIs, revenue, alerts    |

### Drug Catalogue
| Method | Endpoint              | Description              |
|--------|-----------------------|--------------------------|
| GET    | `/drugs`              | List all drugs (paged)   |
| POST   | `/drugs`              | Add new drug             |
| GET    | `/drugs/{id}`         | Get drug by ID           |
| PUT    | `/drugs/{id}`         | Update drug              |
| DELETE | `/drugs/{id}`         | Soft-delete drug         |
| GET    | `/drugs/search?q=`    | Search drugs             |
| GET    | `/drugs/categories`   | All categories           |

### Inventory
| Method | Endpoint                        | Description              |
|--------|---------------------------------|--------------------------|
| POST   | `/inventory`                    | Add stock batch          |
| GET    | `/inventory/drug/{drugId}`      | Batches for a drug       |
| GET    | `/inventory/alerts/low-stock`   | Low/out-of-stock list    |
| GET    | `/inventory/alerts/expiring`    | Expiring batches         |

### Patients
| Method | Endpoint              | Description              |
|--------|-----------------------|--------------------------|
| GET    | `/patients`           | List patients (paged)    |
| POST   | `/patients`           | Register new patient     |
| GET    | `/patients/{id}`      | Get patient              |
| PUT    | `/patients/{id}`      | Update patient           |
| GET    | `/patients/search?q=` | Search patients          |

### Prescriptions
| Method | Endpoint                              | Description              |
|--------|---------------------------------------|--------------------------|
| POST   | `/prescriptions`                      | Create prescription      |
| GET    | `/prescriptions`                      | List all                 |
| GET    | `/prescriptions/today`                | Today's prescriptions    |
| PUT    | `/prescriptions/{id}/dispense`        | Dispense (deducts stock) |
| PUT    | `/prescriptions/{id}/cancel`          | Cancel                   |

### Sales
| Method | Endpoint                 | Description              |
|--------|--------------------------|--------------------------|
| POST   | `/sales`                 | Create sale / bill       |
| GET    | `/sales`                 | List all sales           |
| GET    | `/sales/bill/{number}`   | Get by bill number       |
| PUT    | `/sales/{id}/refund`     | Refund + restock         |

### Suppliers & Purchase Orders
| Method | Endpoint                       | Description              |
|--------|--------------------------------|--------------------------|
| GET    | `/suppliers`                   | List suppliers           |
| POST   | `/purchase-orders`             | Create PO                |
| PUT    | `/purchase-orders/{id}/receive`| Receive order → inventory|

---

## Role Permissions

| Role            | Drugs | Inventory | Prescriptions | Sales | Suppliers |
|-----------------|-------|-----------|---------------|-------|-----------|
| ADMIN           | R/W   | R/W       | R/W           | R/W   | R/W       |
| STORE_MANAGER   | R/W   | R/W       | R             | R     | R/W       |
| PHARMACIST      | R/W   | R/W       | R/W           | R/W   | R         |
| CASHIER         | R     | R         | R             | R/W   | R         |
| VIEWER          | R     | R         | R             | R     | R         |

---

## Important Business Logic

- **FEFO Stock Deduction** — First Expire, First Out: when dispensing or selling, the earliest-expiring batch is consumed first.
- **Prescription Dispensing** — automatically deducts stock across multiple batches.
- **Purchase Order Receive** — automatically creates new inventory batches with default 30% margin.
- **Scheduled Job** — runs daily at 6AM to mark expired batches.
- **Soft Deletes** — drugs, patients, suppliers are never hard-deleted; `is_active = false`.
- **Audit Timestamps** — all entities carry `created_at` / `updated_at` via JPA Auditing.

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database configured automatically.

---

## Production Checklist

- [ ] Change `app.jwt.secret` to a strong 256-bit Base64 key
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Configure a production MySQL user (not root)
- [ ] Enable HTTPS / SSL termination
- [ ] Set `logging.level.com.medicare=WARN`
- [ ] Change all default passwords immediately
