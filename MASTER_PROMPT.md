# Master Prompt — Desktop Management System (Any Domain)

> **How to use:** Copy this entire prompt into Kiro (or any AI agent). Replace every `[PLACEHOLDER]` with your
> specific values before submitting. The prompt carries the full architecture, design system, and startup
> animation rules from the MediCare reference project so the AI can recreate it for any domain.

---

## THE PROMPT

```
Build a complete, production-ready desktop management system called "[APP_NAME]" for "[DOMAIN]"
(e.g., Hotel Management, School Administration, Clinic, Restaurant, etc.).

════════════════════════════════════════════════════
  TECH STACK — do not deviate from this stack
════════════════════════════════════════════════════

Backend
  • Java 17
  • Spring Boot 3.2.x (spring-boot-starter-parent)
  • Spring Security — stateless JWT (jjwt 0.11.5)
  • Spring Data JPA + Hibernate
  • Primary DB: MySQL 8 (auto-detected on port 3306)
  • Fallback DB: H2 in-memory (activated via spring profile "h2" when MySQL unreachable)
  • Optional: Microsoft SQL Server (auto-detected on port 1433 via Windows Auth)
  • Lombok 1.18.46, MapStruct 1.5.5.Final
  • springdoc-openapi 2.5.0 (Swagger UI at /api/swagger-ui.html)
  • Spring Boot Actuator (only /health exposed)
  • Server context path: /api
  • Server port: random (0) in desktop mode, 8080 in headless/server mode

Desktop Wrapper
  • JavaFX 21.0.1 — WebView wrapping the Spring Boot HTML frontend
  • JavaFX WebView is the ONLY UI window, no native JavaFX controls
  • App starts with a JavaFX loading screen BEFORE Spring Boot is ready (see Startup Animation below)
  • A background daemon thread starts Spring Boot; a monitor thread polls /api/actuator/health
  • Once healthy, the monitor calls Platform.runLater(() -> webView.getEngine().load(frontendUrl))
  • Window: 1366×768 minimum 1024×600, resizable, no native title bar decorations
  • Exposed JS bridge (window.javaBridge): log, error, minimize, maximize, close, savePdf
  • Block navigation away from localhost origin in locationProperty listener

Frontend (served as Spring Boot static resources under src/main/resources/static/)
  • Single HTML file: index.html
  • Vanilla JS only — NO React, Vue, Angular, or any JS framework
  • All API calls use fetch() with Authorization: Bearer <token> header
  • CSS: one file styles.css — design tokens via CSS custom properties (see Design System below)
  • JS: split into logical modules, one file per concern, e.g., auth.js, api.js, app.js,
    dashboard.js, [entity1].js, [entity2].js, etc.

Build
  • Maven 3.9.x (bundled in maven-dist/ if desired)
  • Fat JAR via spring-boot-maven-plugin
  • JavaFX JARs copied to target/javafx-lib/ via maven-dependency-plugin
  • JVM args for JavaFX module path and --add-modules set in pom.xml plugin config

════════════════════════════════════════════════════
  PROJECT STRUCTURE
════════════════════════════════════════════════════

[app-name]/
├── src/main/java/com/[company]/
│   ├── [AppName]Application.java          ← main class, headless/desktop detection, DB auto-detect
│   ├── DesktopApp.java                    ← JavaFX Application, builds loading screen, boots Spring
│   ├── DesktopServerBridge.java           ← static refs: port, apiBaseUrl, indexUrl, healthUrl
│   ├── config/
│   │   ├── DataInitializer.java           ← seeds default users on first run
│   │   ├── DesktopServerReadyListener.java← ApplicationReadyEvent → sets port in DesktopServerBridge
│   │   ├── OpenApiConfig.java
│   │   └── SecurityConfig.java            ← JWT stateless, CORS localhost only
│   ├── controller/                        ← one @RestController per entity/domain
│   ├── dto/                               ← request/response DTOs (Lombok @Data)
│   ├── entity/                            ← JPA @Entity (Lombok @Data/@Builder)
│   ├── exception/                         ← GlobalExceptionHandler, custom exceptions
│   ├── repository/                        ← Spring Data JPA interfaces
│   ├── security/                          ← JwtAuthFilter, JwtUtils, UserDetailsServiceImpl
│   └── service/
│       ├── impl/                          ← concrete service implementations
│       └── [Entity]Service.java           ← interfaces
├── src/main/resources/
│   ├── application.properties             ← MySQL config, JWT secret, logging
│   ├── application-h2.properties          ← H2 fallback (spring.profiles: h2)
│   ├── application-sqlserver.properties   ← SQL Server (spring.profiles: sqlserver)
│   ├── schema.sql                         ← optional seed data, run on H2 profile
│   └── static/
│       ├── index.html
│       ├── assets/logo.png
│       ├── css/styles.css
│       └── js/  (auth.js, api.js, app.js, dashboard.js, [entity].js …)
├── pom.xml
├── run.bat                                ← windows launcher
└── README.md

════════════════════════════════════════════════════
  STARTUP ANIMATION — implement exactly as described
════════════════════════════════════════════════════

When the JavaFX window opens, BEFORE Spring Boot is ready, show a loading screen
built by DesktopApp.buildLoadingHtml(). This is an inline HTML string with embedded CSS.
No external resources are loaded (logo is base64-encoded from the classpath).

Required animation elements (all pure CSS, no JS animations):
  1. Logo image — fade in from opacity 0 to 1 over 0.8 s, ease-out
     CSS: @keyframes fadeIn { from { opacity:0; transform:translateY(-12px) } to { opacity:1; transform:translateY(0) } }
          .logo-img { animation: fadeIn 0.8s ease-out forwards; }

  2. App name title — fade in 0.4 s delay after logo
     CSS: .title { animation: fadeIn 0.6s ease-out 0.4s forwards; opacity:0; }

  3. Subtitle / tagline — fade in 0.6 s delay
     CSS: .subtitle { animation: fadeIn 0.6s ease-out 0.6s forwards; opacity:0; }

  4. Animated progress bar — a sliding shimmer bar (no JS, pure CSS keyframes)
     CSS: @keyframes shimmer {
            0%   { transform: translateX(-100%); }
            100% { transform: translateX(250%); }
          }
          .progress { width:200px; height:3px; background:#d4ead4; border-radius:2px;
                      margin:40px auto 0; overflow:hidden;
                      animation: fadeIn 0.4s ease-out 0.8s forwards; opacity:0; }
          .bar { width:40%; height:100%; background: var(--accent-primary-value);
                 animation: shimmer 1.4s ease-in-out infinite; }

  5. Status text below bar — small text "Starting services…" that pulses
     CSS: @keyframes pulse { 0%,100% { opacity:0.4 } 50% { opacity:1 } }
          .status { font-size:12px; color:#6b7f6b; margin-top:14px;
                    animation: pulse 2s ease-in-out infinite, fadeIn 0.4s ease-out 1.0s forwards;
                    opacity:0; }

Full loading screen HTML structure:
  <body> background: var(--bg-primary-value) [use the exact hex, not a var() since CSS vars won't be loaded]
    <div class="container">
      <img class="logo-img" src="data:image/png;base64,..." />
      <div class="title">[APP_NAME]</div>
      <div class="subtitle">[DOMAIN] Management System</div>
      <div class="progress"><div class="bar"></div></div>
      <div class="status">Starting services…</div>
    </div>
  </body>

Color values to hard-code in the loading screen HTML (copy from the Design System below):
  background: #f0f5f0   (--bg-primary)
  accent:     #3d8b37   (--accent-primary)  ← change to your domain color if not green
  text:       #1a2e1a   (--text-primary)
  muted:      #6b7f6b   (--text-muted)

════════════════════════════════════════════════════
  DESIGN SYSTEM — replicate exactly in styles.css
════════════════════════════════════════════════════

Fonts (load from Google Fonts):
  • Body/UI:  Plus Jakarta Sans  weights 400, 500, 600, 700
  • Monospace: JetBrains Mono     weights 400, 500  (for receipt/code/clock)

CSS Custom Properties (:root):
  --bg-primary:       #f0f5f0   ← light green-tinted page background
  --bg-secondary:     #ffffff   ← sidebar, header, cards
  --bg-card:          #ffffff
  --bg-card-hover:    #f7faf7
  --border-color:     #d4e4d4
  --accent-primary:   #3d8b37   ← primary green  [CHANGE for your domain]
  --accent-secondary: #2d6a2e   ← darker shade of primary
  --accent-green:     #3d8b37
  --accent-blue:      #2563eb
  --accent-orange:    #d97706
  --accent-red:       #dc2626
  --text-primary:     #1a2e1a
  --text-secondary:   #4a5f4a
  --text-muted:       #8a9a8a
  --sidebar-width:    250px
  --header-height:    52px
  --font-sans:        'Plus Jakarta Sans', sans-serif
  --font-mono:        'JetBrains Mono', monospace

Layout (two-column flex, full viewport, no scroll on body):
  body              → overflow: hidden; user-select: none
  .app-container    → display:flex; width:100vw; height:100vh
  .sidebar          → width:var(--sidebar-width); border-right:1px solid var(--border-color); flex-shrink:0
  .main-content     → flex:1; display:flex; flex-direction:column; height:100vh

Sidebar:
  .sidebar-header   → height:52px; logo + app name (font-weight:700, color:var(--accent-primary))
  .sidebar-menu     → flex:1; padding:16px 12px; gap:4px; overflow-y:auto
  .menu-item        → flex row; gap:12px; padding:10px 14px; border-radius:8px; font-size:14px; font-weight:500
  .menu-icon-wrapper→ 32×32px rounded-8 bg:var(--bg-primary) border:1px solid var(--border-color)
  .menu-item.active → bg:#e8f3e8; color:var(--accent-primary); border:1px solid #c8dfc8
  .sidebar-footer   → padding:16px; border-top:1px solid var(--border-color); user avatar + logout

Top Header:
  .top-header       → height:52px; bg:var(--bg-secondary); border-bottom; padding:0 24px; flex space-between
  right side: live clock in JetBrains Mono inside .system-time pill (bg:var(--bg-primary), border, border-radius:6px)

Viewport sheets:
  .viewport-sheet           → display:none; padding:24px; overflow-y:auto
  .viewport-sheet.active    → display:block

Cards:
  .glass-card       → bg:var(--bg-card); border:1px solid var(--border-color); border-radius:10px; padding:20px

Metric cards (4-column grid on dashboard):
  .card-purple  → border-left:3px solid #7c3aed
  .card-blue    → border-left:3px solid #2563eb
  .card-orange  → border-left:3px solid #d97706
  .card-red     → border-left:3px solid #dc2626
  .card-green   → border-left:3px solid var(--accent-primary)

Tables (.premium-table):
  th → uppercase; font-size:12px; font-weight:600; color:var(--text-secondary); border-bottom:2px solid var(--border-color); padding:12px 16px
  td → padding:12px 16px; border-bottom:1px solid var(--border-color); font-size:14px
  tr:hover td → bg:var(--bg-primary)

Buttons:
  .btn-primary  → bg:var(--accent-primary); color:#fff; border-radius:6px; font-weight:600
  .btn-secondary→ bg:var(--bg-primary); border:1px solid var(--border-color)
  .btn-danger   → bg:#fef2f2; border:#fecaca; color:var(--accent-red)
  .btn-sm       → padding:5px 10px; font-size:12px

Badges:
  .badge-green, .badge-orange, .badge-red, .badge-blue, .badge-purple, .badge-muted
  All: padding:3px 8px; border-radius:4px; font-size:11px; font-weight:700; uppercase; light bg + colored border

Forms:
  .form-control → bg:var(--bg-primary); border:1px solid var(--border-color); border-radius:6px; padding:10px 14px
  :focus        → outline:none; border-color:var(--accent-primary)
  label         → font-size:13px; font-weight:600; color:var(--text-secondary)

Login screen:
  .login-overlay → position:fixed; full viewport; bg:var(--bg-primary); z-index:1000; centered
  .login-card    → width:400px; padding:32px; glass-card style
  Logo + app name centered at top, then form, then role quick-login buttons grid

Modals:
  .modal-overlay  → fixed overlay rgba(0,0,0,0.4); z-index:100
  .modal-container→ max-width:600px; max-height:90vh; overflow-y:auto; glass-card style

Toast notifications (bottom-right):
  .toast-container → position:fixed; bottom:24px; right:24px; z-index:1100
  .toast           → min-width:280px; border-left:4px solid; rounded-8; shadow
  Variants: .toast-success (green), .toast-error (red), .toast-warning (orange)

Scrollbars (webkit):
  width/height:6px; thumb:#c8dfc8; track:transparent

════════════════════════════════════════════════════
  AUTHENTICATION & ROLES
════════════════════════════════════════════════════

• JWT stateless. Token stored in memory (JS variable), NOT localStorage/sessionStorage.
• POST /api/auth/login → returns { token, refreshToken, user: { id, username, fullName, roles[] } }
• All API calls: Authorization: Bearer <token> header via a central api.js fetch wrapper.
• Roles (adapt names to domain, keep the ROLE_ prefix for Spring):
    ROLE_ADMIN           → full access (always exists)
    ROLE_[MANAGER]       → operational access
    ROLE_[OPERATOR]      → transactional access (POS equivalent)
  DataInitializer seeds one user per role on first run with known default passwords (log them).
• Sidebar menu items show/hide based on current user's roles (check in JS after login).
• Quick-login buttons on login card for each default role.

════════════════════════════════════════════════════
  BACKEND PATTERNS
════════════════════════════════════════════════════

Every entity follows this pattern:
  Entity      → @Entity, @Table, @Id @GeneratedValue, @CreatedDate/@LastModifiedDate via JPA Auditing
  Repository  → extends JpaRepository; add custom query methods as needed
  Service     → interface + impl class annotated @Service @RequiredArgsConstructor
  Controller  → @RestController @RequestMapping("/[plural-entity]") @RequiredArgsConstructor
  DTOs        → separate Request (for POST/PUT) and Response (for GET) records or @Data classes
  Mapper      → MapStruct @Mapper(componentModel="spring") to convert entity ↔ DTO

GlobalExceptionHandler (@RestControllerAdvice):
  • EntityNotFoundException → 404
  • ConstraintViolationException / MethodArgumentNotValidException → 400 with field errors map
  • AccessDeniedException → 403
  • Generic Exception → 500

Database auto-detection in [AppName]Application.main():
  1. Probe 127.0.0.1:3306 (MySQL) — if reachable, use default application.properties
  2. Probe 127.0.0.1:1433 (SQL Server) — if reachable, try Windows Auth JDBC connection;
     if it connects, set system properties and profile "sqlserver"
  3. Fallback: set spring.profiles.active=h2

DesktopServerReadyListener implements ApplicationListener<ApplicationReadyEvent>:
  • Reads Environment to find the actual bound server port (server.local.port or server.port)
  • Calls DesktopServerBridge.setPort(port) and DesktopServerBridge.setReady(true)

DesktopServerBridge (static utility):
  • static volatile int port
  • static volatile boolean ready
  • static String apiBaseUrl()  → "http://127.0.0.1:" + port + "/api"
  • static String indexUrl()    → apiBaseUrl() + "/"
  • static String healthUrl()   → apiBaseUrl() + "/actuator/health"

application.properties key settings:
  server.servlet.context-path=/api
  server.tomcat.threads.max=20 (single-user desktop, keep it light)
  spring.datasource.hikari.maximum-pool-size=5
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.open-in-view=false
  app.jwt.secret=[256-bit secret — remind user to change in production]
  app.jwt.expiration-ms=86400000

════════════════════════════════════════════════════
  FRONTEND PATTERNS (Vanilla JS modules)
════════════════════════════════════════════════════

api.js:
  const API_BASE = window.location.origin + '/api';
  let authToken = null;
  const Api = {
    setToken(t) { authToken = t; },
    async get(path) { ... },
    async post(path, body) { ... },
    async put(path, body) { ... },
    async delete(path) { ... }
  };

auth.js:
  const Auth = {
    async login(username, password) { ... calls POST /api/auth/login, stores token, shows app },
    quickLogin(username, password) { ... },
    logout() { ... clears token, shows login overlay }
  };

app.js:
  const App = {
    init() { ... bind menu items, start clock, call Auth.checkSession if needed },
    switchTab(tabName) { ... toggle .active on .viewport-sheet and .menu-item },
  };

dashboard.js, [entity].js, etc.:
  Each module exposes load(), openAddModal(), openEditModal(id), delete(id), etc.
  Modals are generic: a single #generic-modal div whose content is replaced dynamically.
  Toasts: Toast.show(message, type) where type = 'success' | 'error' | 'warning'.

Receipt / PDF:
  Build HTML receipt string, open in generic modal, use window.print() or
  window.javaBridge.savePdf(base64, filename) for PDF export.

Clock (top-right):
  setInterval(() => { document.getElementById('clock').textContent = new Date().toLocaleTimeString(); }, 1000);

════════════════════════════════════════════════════
  DOMAIN CUSTOMISATION SLOTS  ← fill these in
════════════════════════════════════════════════════

APP_NAME:          [e.g., "StayPro", "EduTrack", "DineFlow"]
DOMAIN:            [e.g., "Hotel", "School", "Restaurant"]
COMPANY_PACKAGE:   [e.g., com.staypro, com.edutrack]
ACCENT_COLOR:      [e.g., #1d6fa4 for blue hotel theme, #c0392b for red restaurant theme]
ACCENT_SECONDARY:  [darker 15% shade of accent]
BG_PRIMARY:        [light tint of accent color, e.g., #f0f6fb for blue]
TEXT_PRIMARY:      [dark shade matching accent hue, e.g., #1a2a3a]

ROLES:
  ROLE_ADMIN       → [Admin]
  ROLE_[R2]        → [e.g., ROLE_MANAGER, ROLE_RECEPTIONIST, ROLE_TEACHER]
  ROLE_[R3]        → [e.g., ROLE_CASHIER, ROLE_HOUSEKEEPER, ROLE_STUDENT_ADVISOR]

ENTITIES (list each with key fields):
  1. [EntityName]  → [field1: type, field2: type, ...]
  2. [EntityName]  → [...]
  ...

NAV MENU ITEMS (map each to an entity/screen):
  1. Dashboard     → summary KPIs, charts
  2. [Primary Ops] → [e.g., Reservations, Enrollments, Orders]
  3. [Resource]    → [e.g., Rooms, Courses, Menu Items]
  4. [People]      → [e.g., Guests, Students, Customers]
  5. [Supply]      → [e.g., Procurement, Inventory]
  6. [Finance]     → [e.g., Billing, Invoices]
  7. Staff         → user management (ADMIN only)
  8. Database      → DB config + backup (ADMIN only)

DASHBOARD KPI CARDS (4 cards, use accent colors):
  Card 1 (purple border):  [e.g., Today's Revenue / Occupancy Rate]
  Card 2 (blue border):    [e.g., Monthly Revenue / Active Enrollments]
  Card 3 (orange border):  [e.g., Pending Items / Low Stock]
  Card 4 (red border):     [e.g., Alerts / Expiring / Overdue]

════════════════════════════════════════════════════
  ADDITIONAL REQUIREMENTS
════════════════════════════════════════════════════

1. DO NOT use any external CSS framework (no Bootstrap, Tailwind, etc.).
   Use only the design system described above.

2. All SVG icons are inline (stroke-based, no fill, stroke-width:2, 18×18 or 24×24).
   Do not use any icon library.

3. The app must work offline (H2 fallback) — no CDN dependencies at runtime.
   Google Fonts import is acceptable (graceful degrade if offline: system sans-serif).

4. user-select:none on all layout elements; user-select:text only on inputs, textareas,
   selects, and printable receipt content.

5. The loading screen MUST use the startup animations described above — this is non-negotiable.
   The shimmer bar must loop infinitely while Spring Boot starts.

6. Print media query: hide everything except #generic-modal for receipt printing.

7. Error handling: every fetch() call must catch errors and call Toast.show(msg, 'error').
   Never let unhandled promise rejections appear.

8. The run.bat launcher sets:
   --module-path ./target/javafx-lib
   --add-modules javafx.controls,javafx.web,javafx.graphics,javafx.base,javafx.media
   -Dprism.order=sw  (software renderer for compatibility)
   -jar target/[artifact]-1.0.0.jar

9. Generate a complete README.md with: prerequisites, build steps, default credentials,
   DB configuration guide, and how to change the accent color.

10. Include .gitignore covering: target/, *.class, .idea/, .vscode/, *.log, *.jar (except
    in maven-dist/), application-local.properties (for secrets override).
```

---

## QUICK EXAMPLE — Hotel Management

Replace the slots like this:

| Slot | Value |
|---|---|
| APP_NAME | StayPro |
| DOMAIN | Hotel |
| COMPANY_PACKAGE | com.staypro |
| ACCENT_COLOR | #1d6fa4 |
| ACCENT_SECONDARY | #155a87 |
| BG_PRIMARY | #f0f5fb |
| TEXT_PRIMARY | #1a2535 |
| ROLES | ROLE_ADMIN, ROLE_MANAGER, ROLE_RECEPTIONIST, ROLE_HOUSEKEEPER |
| ENTITIES | Room (number, type, floor, status, pricePerNight), Reservation (guest, room, checkIn, checkOut, status), Guest (name, email, phone, idNumber, nationality), Billing (reservation, amount, paymentMethod, status), HousekeepingTask (room, assignedTo, status, notes) |
| NAV ITEMS | Dashboard, Reservations, Rooms, Guests, Housekeeping, Billing, Staff, Database |
| KPI CARDS | Occupied Rooms Today / Monthly Revenue / Rooms Pending Cleaning / Overdue Checkouts |

---

*This master prompt was reverse-engineered from MediCare Pharmacy Management System v1.0.0.*
*Startup animation section and design system are derived from `DesktopApp.java` and `styles.css`.*
