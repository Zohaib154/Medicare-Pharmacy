-- ============================================================
--  RxPro Pharmacy Management System — Full SQL Schema
--  Database: MySQL 8.x
-- ============================================================

CREATE DATABASE IF NOT EXISTS rxpro_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rxpro_db;

-- ─────────────────────────────────────────────
-- 1. USERS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) UNIQUE,
    contact_number VARCHAR(15),
    license_number VARCHAR(50),
    is_active     TINYINT(1)  NOT NULL DEFAULT 1,
    refresh_token TEXT,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role    VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ─────────────────────────────────────────────
-- 2. DRUG CATALOGUE
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS drugs (
    drug_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    drug_name         VARCHAR(200) NOT NULL,
    generic_name      VARCHAR(200),
    category          VARCHAR(100) NOT NULL,
    manufacturer      VARCHAR(100),
    dosage_form       VARCHAR(50),
    strength          VARCHAR(50),
    schedule_type     VARCHAR(20),
    unit_price        DECIMAL(10,2),
    mrp               DECIMAL(10,2),
    hsn_code          VARCHAR(20),
    gst_percent       DECIMAL(5,2),
    description       TEXT,
    side_effects      TEXT,
    contraindications TEXT,
    is_active         TINYINT(1) NOT NULL DEFAULT 1,
    created_at        DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME   ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_drug_name    (drug_name),
    INDEX idx_drug_category (category),
    INDEX idx_schedule_type (schedule_type)
);

-- ─────────────────────────────────────────────
-- 3. SUPPLIERS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_name      VARCHAR(150) NOT NULL,
    contact_person     VARCHAR(150),
    contact_number     VARCHAR(15),
    email              VARCHAR(150),
    address            TEXT,
    city               VARCHAR(50),
    license_number     VARCHAR(50),
    gst_number         VARCHAR(30),
    bank_name          VARCHAR(50),
    bank_account_no    VARCHAR(50),
    payment_terms      VARCHAR(20),
    outstanding_balance DECIMAL(10,2) DEFAULT 0.00,
    is_active          TINYINT(1) NOT NULL DEFAULT 1,
    notes              TEXT,
    created_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME   ON UPDATE CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────
-- 4. INVENTORY (Batch-level stock)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    drug_id            BIGINT NOT NULL,
    supplier_id        BIGINT,
    batch_number       VARCHAR(50) NOT NULL,
    quantity_in_stock  INT NOT NULL DEFAULT 0,
    reorder_level      INT NOT NULL DEFAULT 50,
    expiry_date        DATE NOT NULL,
    manufacturing_date DATE NOT NULL,
    purchase_price     DECIMAL(10,2),
    selling_price      DECIMAL(10,2),
    storage_location   VARCHAR(50),
    stock_status       ENUM('IN_STOCK','LOW_STOCK','OUT_OF_STOCK','EXPIRED','RECALLED') DEFAULT 'IN_STOCK',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (drug_id)     REFERENCES drugs(drug_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    INDEX idx_drug_stock      (drug_id, stock_status),
    INDEX idx_expiry          (expiry_date),
    INDEX idx_batch           (batch_number)
);

-- ─────────────────────────────────────────────
-- 5. PATIENTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS patients (
    patient_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name           VARCHAR(100) NOT NULL,
    date_of_birth       DATE NOT NULL,
    gender              ENUM('MALE','FEMALE','OTHER') NOT NULL,
    contact_number      VARCHAR(15),
    email               VARCHAR(150),
    address             TEXT,
    blood_group         VARCHAR(10),
    allergies           TEXT,
    chronic_conditions  TEXT,
    current_medications TEXT,
    cnic_number         VARCHAR(20),
    insurance_provider  VARCHAR(50),
    insurance_policy_no VARCHAR(50),
    is_active           TINYINT(1) NOT NULL DEFAULT 1,
    created_at          DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME   ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patient_name (full_name),
    INDEX idx_cnic         (cnic_number)
);

-- ─────────────────────────────────────────────
-- 6. PRESCRIPTIONS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS prescriptions (
    prescription_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    rx_number        VARCHAR(20) NOT NULL UNIQUE,
    patient_id       BIGINT NOT NULL,
    dispensed_by     BIGINT,
    doctor_name      VARCHAR(100) NOT NULL,
    doctor_license_no VARCHAR(100),
    hospital_clinic  VARCHAR(150),
    issue_date       DATE NOT NULL,
    dispensed_date   DATE,
    diagnosis        VARCHAR(100),
    status           ENUM('PENDING','PROCESSING','DISPENSED','PARTIALLY_DISPENSED','CANCELLED','EXPIRED') DEFAULT 'PENDING',
    notes            TEXT,
    total_amount     DECIMAL(10,2) DEFAULT 0.00,
    discount_percent DECIMAL(5,2)  DEFAULT 0.00,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id)   REFERENCES patients(patient_id),
    FOREIGN KEY (dispensed_by) REFERENCES users(user_id),
    INDEX idx_rx_status   (status),
    INDEX idx_rx_date     (issue_date),
    INDEX idx_rx_patient  (patient_id)
);

CREATE TABLE IF NOT EXISTS prescription_items (
    item_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id      BIGINT NOT NULL,
    drug_id              BIGINT NOT NULL,
    quantity             INT NOT NULL,
    dosage_instructions  VARCHAR(200),
    duration             VARCHAR(50),
    frequency            VARCHAR(200),
    unit_price           DECIMAL(10,2),
    total_price          DECIMAL(10,2),
    is_dispensed         TINYINT(1) DEFAULT 0,
    remarks              TEXT,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id) ON DELETE CASCADE,
    FOREIGN KEY (drug_id)         REFERENCES drugs(drug_id)
);

-- ─────────────────────────────────────────────
-- 7. SALES & BILLING
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sales (
    sale_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_number      VARCHAR(20) NOT NULL UNIQUE,
    patient_id       BIGINT,
    prescription_id  BIGINT,
    sold_by          BIGINT NOT NULL,
    sale_date_time   DATETIME NOT NULL,
    subtotal         DECIMAL(10,2),
    discount_percent DECIMAL(5,2)  DEFAULT 0.00,
    discount_amount  DECIMAL(10,2) DEFAULT 0.00,
    gst_amount       DECIMAL(10,2) DEFAULT 0.00,
    total_amount     DECIMAL(10,2) NOT NULL,
    amount_paid      DECIMAL(10,2) DEFAULT 0.00,
    change_returned  DECIMAL(10,2) DEFAULT 0.00,
    payment_method   ENUM('CASH','CARD','MOBILE_BANKING','INSURANCE','CREDIT') DEFAULT 'CASH',
    status           ENUM('COMPLETED','PENDING','REFUNDED','PARTIAL_REFUND','CANCELLED') DEFAULT 'COMPLETED',
    notes            TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id)      REFERENCES patients(patient_id),
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id),
    FOREIGN KEY (sold_by)         REFERENCES users(user_id),
    INDEX idx_sale_date    (sale_date_time),
    INDEX idx_sale_patient (patient_id),
    INDEX idx_sale_status  (status)
);

CREATE TABLE IF NOT EXISTS sale_items (
    sale_item_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id          BIGINT NOT NULL,
    drug_id          BIGINT NOT NULL,
    inventory_id     BIGINT,
    quantity         INT NOT NULL,
    unit_price       DECIMAL(10,2) NOT NULL,
    discount_percent DECIMAL(5,2)  DEFAULT 0.00,
    gst_amount       DECIMAL(10,2) DEFAULT 0.00,
    total_price      DECIMAL(10,2) NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (sale_id)      REFERENCES sales(sale_id) ON DELETE CASCADE,
    FOREIGN KEY (drug_id)      REFERENCES drugs(drug_id),
    FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id)
);

-- ─────────────────────────────────────────────
-- 8. PURCHASE ORDERS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase_orders (
    order_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_number              VARCHAR(20) NOT NULL UNIQUE,
    supplier_id            BIGINT NOT NULL,
    ordered_by             BIGINT NOT NULL,
    order_date             DATE NOT NULL,
    expected_delivery_date DATE,
    actual_delivery_date   DATE,
    status  ENUM('PENDING','CONFIRMED','SHIPPED','DELIVERED','PARTIALLY_DELIVERED','CANCELLED') DEFAULT 'PENDING',
    total_amount           DECIMAL(10,2) DEFAULT 0.00,
    paid_amount            DECIMAL(10,2) DEFAULT 0.00,
    notes                  TEXT,
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    FOREIGN KEY (ordered_by)  REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    po_item_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id              BIGINT NOT NULL,
    drug_id            BIGINT NOT NULL,
    ordered_quantity   INT NOT NULL,
    received_quantity  INT DEFAULT 0,
    unit_price         DECIMAL(10,2) NOT NULL,
    total_price        DECIMAL(10,2),
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (po_id)    REFERENCES purchase_orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (drug_id)  REFERENCES drugs(drug_id)
);

-- ─────────────────────────────────────────────
-- SAMPLE DATA
-- ─────────────────────────────────────────────
INSERT INTO suppliers (supplier_name, contact_person, contact_number, email, city, license_number, payment_terms) VALUES
('Sun Pharmaceuticals',   'Rajesh Sharma',  '022-4324-5600', 'sales@sunpharma.com',  'Mumbai',  'DL-SUN-001', 'NET-30'),
('Cipla Ltd',             'Vikram Malhotra','021-3568-1234', 'karachi@cipla.com',    'Karachi', 'DL-CIP-002', 'NET-45'),
('GSK Pakistan',          'Sarah Johnson',  '042-111-475787','orders@gsk.pk',        'Lahore',  'DL-GSK-003', 'NET-30'),
('Ranbaxy Laboratories',  'Anil Kumar',     '011-4616-5050', 'orders@ranbaxy.com',   'Delhi',   'DL-RAN-004', 'NET-60');

INSERT INTO drugs (drug_name, generic_name, category, manufacturer, dosage_form, strength, schedule_type, unit_price, mrp, gst_percent) VALUES
('Amoxicillin 500mg',   'Amoxicillin',      'Antibiotic',     'Cipla Ltd',             'Capsule', '500mg',    'Schedule H',  10.50, 12.50, 12.00),
('Metformin 850mg',     'Metformin HCl',    'Antidiabetic',   'Sun Pharmaceuticals',   'Tablet',  '850mg',    'Schedule H',   7.00,  8.20, 12.00),
('Atorvastatin 10mg',   'Atorvastatin',     'Cardiovascular', 'Ranbaxy Laboratories',  'Tablet',  '10mg',     'Schedule H',  16.00, 18.75, 12.00),
('Paracetamol 500mg',   'Paracetamol',      'Analgesic',      'GSK Pakistan',          'Tablet',  '500mg',    'OTC',           2.00,  2.50,  5.00),
('Lisinopril 10mg',     'Lisinopril',       'Cardiovascular', 'Lupin Limited',         'Tablet',  '10mg',     'Schedule H',  19.00, 22.00, 12.00),
('Omeprazole 20mg',     'Omeprazole',       'Gastric',        'Sun Pharmaceuticals',   'Capsule', '20mg',     'Schedule H',  12.00, 14.50, 12.00),
('Augmentin 625mg',     'Amoxicillin+Clav', 'Antibiotic',     'GSK Pakistan',          'Tablet',  '625mg',    'Schedule H',  35.00, 42.00, 12.00),
('Clopidogrel 75mg',    'Clopidogrel',      'Antiplatelet',   'Sun Pharmaceuticals',   'Tablet',  '75mg',     'Schedule H',  22.00, 26.00, 12.00),
('Cetirizine 10mg',     'Cetirizine HCl',   'Antihistamine',  'Cipla Ltd',             'Tablet',  '10mg',     'OTC',           4.00,  5.00,  5.00),
('Azithromycin 500mg',  'Azithromycin',     'Antibiotic',     'Cipla Ltd',             'Tablet',  '500mg',    'Schedule H',  28.00, 33.00, 12.00);

INSERT INTO inventory (drug_id, supplier_id, batch_number, quantity_in_stock, reorder_level, expiry_date, manufacturing_date, purchase_price, selling_price, storage_location, stock_status) VALUES
(1,  2, 'AMX-2026-A',   0,   50, '2027-12-31', '2024-01-01', 10.50, 12.50, 'Shelf-A1', 'OUT_OF_STOCK'),
(2,  1, 'MF-2203',      48,  50, '2026-05-29', '2022-03-01',  7.00,  8.20, 'Shelf-B2', 'LOW_STOCK'),
(3,  4, 'ATV-2025-K',   12,  50, '2027-08-15', '2023-08-01', 16.00, 18.75, 'Shelf-C1', 'LOW_STOCK'),
(4,  3, 'PCT-2026-B',  340,  50, '2028-03-31', '2024-03-01',  2.00,  2.50, 'Shelf-D1', 'IN_STOCK'),
(5,  2, 'LIS-2026-D',  185,  50, '2027-09-30', '2024-09-01', 19.00, 22.00, 'Shelf-C2', 'IN_STOCK'),
(6,  1, 'OMP-2026-A',   34,  50, '2027-04-30', '2024-04-01', 12.00, 14.50, 'Shelf-B3', 'LOW_STOCK'),
(7,  3, 'AUG-2026-B',  120,  50, '2027-06-30', '2024-06-01', 35.00, 42.00, 'Shelf-A2', 'IN_STOCK'),
(8,  1, 'CLO-2026-A',   56,  50, '2027-07-31', '2024-07-01', 22.00, 26.00, 'Shelf-C3', 'IN_STOCK'),
(9,  2, 'CET-2026-A',  220,  50, '2028-01-31', '2024-01-01',  4.00,  5.00, 'Shelf-D2', 'IN_STOCK'),
(10, 2, 'AZI-2026-A',   85,  50, '2027-10-31', '2024-10-01', 28.00, 33.00, 'Shelf-A3', 'IN_STOCK');

INSERT INTO patients (full_name, date_of_birth, gender, contact_number, blood_group, chronic_conditions, cnic_number) VALUES
('Fatima Khan',   '1992-03-15', 'FEMALE', '0300-1234567', 'B+', 'Hypertension',    '35202-1234567-8'),
('Ali Raza',      '1968-07-22', 'MALE',   '0321-9876543', 'O+', 'Type 2 Diabetes', '35201-9876543-1'),
('Sara Ahmed',    '1981-11-08', 'FEMALE', '0333-5551234', 'A+', 'Heart Disease',    '35202-5554321-0'),
('Usman Tariq',   '1998-05-30', 'MALE',   '0311-7778888', 'AB+', NULL,             '35201-7778888-2'),
('Zainab Malik',  '1975-09-17', 'FEMALE', '0345-2223333', 'O-', 'Asthma',          '35202-2223333-5');
