const App = {
    activeTab: 'dashboard',

    debounce(func, wait = 250) {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func(...args), wait);
        };
    },

    parseLocalDateTime(value) {
        if (!value) return null;
        if (Array.isArray(value)) {
            const [y, m, d, h = 0, min = 0, s = 0] = value;
            return new Date(y, m - 1, d, h, min, s);
        }
        if (typeof value === 'string') {
            const normalized = value.includes('T') ? value : value.replace(' ', 'T');
            const parts = normalized.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/);
            if (parts) {
                return new Date(+parts[1], +parts[2] - 1, +parts[3], +parts[4], +parts[5], +(parts[6] || 0));
            }
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? null : date;
    },

    formatLocalDateTime(value) {
        const date = this.parseLocalDateTime(value);
        if (!date) return '—';
        return date.toLocaleString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    },

    formatCurrency(value, decimals = 2) {
        const num = parseFloat(value);
        if (Number.isNaN(num)) return '—';
        const settings = (typeof AppSettings !== 'undefined') ? AppSettings.load() : {};
        const symbol = settings.currencySymbol || '$';
        return `${symbol}${num.toFixed(decimals)}`;
    },

    init() {
        this.bindEvents();
        this.bindSelectionGuard();
        this.startClock();
        this.checkAuth();
    },

    bindSelectionGuard() {
        document.addEventListener('dblclick', (e) => {
            if (!e.target.closest('input, textarea, select, button, .selectable, .copyable, .receipt-wrapper, .modal-container, .dbb-editable')) {
                e.preventDefault();
                window.getSelection()?.removeAllRanges();
            }
        });
    },

    bindEvents() {
        // Tab switching
        document.querySelectorAll('.menu-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const tab = item.getAttribute('data-tab');
                this.switchTab(tab);
            });
        });

        // Logout (sidebar)
        ['logout-btn'].forEach(id => {
            const btn = document.getElementById(id);
            if (btn) {
                btn.addEventListener('click', () => Auth.logout());
            }
        });

        // Global event when authentication token expires or is required
        window.addEventListener('auth-required', () => {
            this.showLoginOverlay();
        });
    },

    startClock() {
        const clockEl = document.getElementById('clock');
        if (!clockEl) return;

        const updateTime = () => {
            clockEl.textContent = new Date().toLocaleTimeString(undefined, {
                hour: 'numeric',
                minute: '2-digit',
                hour12: true
            });
        };

        updateTime();
        const now = new Date();
        const msToNextMinute = (60 - now.getSeconds()) * 1000 - now.getMilliseconds();
        setTimeout(() => {
            updateTime();
            setInterval(updateTime, 60000);
        }, msToNextMinute);
    },

    checkAuth() {
        const userInfo = API.getUserInfo();
        const tokens = API.getTokens();
        if (tokens.accessToken && userInfo) {
            this.showMainApp(userInfo);
        } else {
            this.showLoginOverlay();
        }
    },

    async showMainApp(user) {
        document.getElementById('login-overlay').style.display = 'none';
        document.getElementById('app-container').style.display = 'flex';
        
        // Update user display details
        document.getElementById('user-fullname').textContent = user.fullName || user.username;
        document.getElementById('user-role').textContent = user.roles ? user.roles[0].replace('ROLE_', '') : 'USER';
        document.getElementById('user-avatar').textContent = (user.fullName || user.username).substring(0, 2).toUpperCase();

        // Check user roles and show/hide tabs accordingly
        const roles = user.roles || [];
        const isAdmin = roles.includes('ROLE_ADMIN');
        const isManager = roles.includes('ROLE_STORE_MANAGER');
        const isPharmacist = roles.includes('ROLE_PHARMACIST');
        const isCashier = roles.includes('ROLE_CASHIER');

        // Apply RBAC filters to sidebar tabs
        document.getElementById('nav-pos').style.display = (isAdmin || isPharmacist || isCashier) ? 'flex' : 'none';
        document.getElementById('nav-inventory').style.display = (isAdmin || isManager || isPharmacist) ? 'flex' : 'none';
        document.getElementById('nav-drugs').style.display = 'flex'; // Visible to all, read-only inside service
        document.getElementById('nav-patients').style.display = 'flex';
        document.getElementById('nav-suppliers').style.display = (isAdmin || isManager) ? 'flex' : 'none';
        document.getElementById('nav-purchase-orders').style.display = (isAdmin || isManager) ? 'flex' : 'none';

        const navMedicines = document.getElementById('nav-medicines');
        if (navMedicines) navMedicines.style.display = 'none';

        // Staff Management - only for Admin
        const navStaff = document.getElementById('nav-staff');
        if (navStaff) navStaff.style.display = isAdmin ? 'flex' : 'none';

        // Backup Database - only for Admin
        const navBackup = document.getElementById('nav-backup');
        if (navBackup) navBackup.style.display = isAdmin ? 'flex' : 'none';

        // Load app settings from server (persisted across restarts)
        if (typeof AppSettings !== 'undefined') {
            try {
                await AppSettings.fetchFromServer();
            } catch (e) {
                console.error('Could not load app settings from server:', e);
            }
        }
        this.switchTab('dashboard');
    },

    showLoginOverlay() {
        document.getElementById('app-container').style.display = 'none';
        document.getElementById('login-overlay').style.display = 'flex';
    },

    switchTab(tabId) {
        if (tabId === 'medicines') {
            tabId = 'drugs';
        }
        this.activeTab = tabId;

        // Update sidebar item states
        document.querySelectorAll('.menu-item').forEach(item => {
            if (item.getAttribute('data-tab') === tabId) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Update active sheet
        document.querySelectorAll('.viewport-sheet').forEach(sheet => {
            if (sheet.id === `sheet-${tabId}`) {
                sheet.classList.add('active');
            } else {
                sheet.classList.remove('active');
            }
        });

        // Update header title
        const formattedTitle = tabId.charAt(0).toUpperCase() + tabId.slice(1).replace('-', ' ');
        document.getElementById('viewport-title').textContent = formattedTitle;

        // Fetch sheet data
        this.loadTabContents(tabId);
    },

    loadTabContents(tabId) {
        try {
            switch(tabId) {
                case 'dashboard':
                    Dashboard.load();
                    break;
                case 'pos':
                    POS.load();
                    break;
                case 'prescriptions':
                    Prescriptions.load();
                    break;
                case 'inventory':
                    Inventory.load();
                    break;
                case 'drugs':
                    Catalog.load();
                    break;
                case 'patients':
                    Patients.load();
                    break;
                case 'suppliers':
                    Suppliers.load();
                    break;
                case 'purchase-orders':
                    Suppliers.loadPOs();
                    break;
                case 'staff':
                    Staff.load();
                    break;
                case 'backup':
                    if (typeof DBBrowser !== 'undefined') {
                        if (!DBBrowser.currentTable) {
                            DBBrowser.loadTable('drugs');
                        } else {
                            DBBrowser.refresh();
                        }
                    }
                    break;
            }
        } catch (e) {
            this.toast(e.message, 'error');
        }
    },

    // Global Notification Toasts
    toast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        let icon = '<svg style="width:20px;height:20px;stroke:currentColor;fill:none;stroke-width:2;" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>'; // Warning icon
        if (type === 'success') {
            icon = '<svg style="width:20px;height:20px;stroke:currentColor;fill:none;stroke-width:2;" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"></polyline></svg>';
        } else if (type === 'error') {
            icon = '<svg style="width:20px;height:20px;stroke:currentColor;fill:none;stroke-width:2;" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>';
        }

        toast.innerHTML = `
            ${icon}
            <span style="font-size:14px; font-weight:600;">${message}</span>
        `;
        container.appendChild(toast);

        setTimeout(() => toast.remove(), 4000);
    },

    // Modal Control
    openModal(title, htmlContent) {
        document.getElementById('modal-title').textContent = title;
        document.getElementById('modal-content').innerHTML = htmlContent;
        document.getElementById('generic-modal').style.display = 'flex';
    },

    closeModal() {
        document.getElementById('generic-modal').style.display = 'none';
        document.getElementById('modal-content').innerHTML = '';
    }
};

document.addEventListener('DOMContentLoaded', () => {
    App.init();
});
