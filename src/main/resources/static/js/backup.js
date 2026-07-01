// ─────────────────────────────────────────────────────────────────────────────
//  DBBrowser — Live database browser with inline editing
//  All data is read from the REST API and is always in sync with the app.
//  Double-click any editable cell to change a value — saved immediately.
// ─────────────────────────────────────────────────────────────────────────────
const DBBrowser = {
    currentTable: null,
    rows: [],

    tables: {
        'drugs': {
            title:    'Drug Catalogue',
            endpoint: '/drugs/all?size=500&sortBy=drugName',
            idField:  'drugId',
            saveUrl:  (id) => `/drugs/${id}`,
            cols: [
                { key: 'drugId',       label: 'ID',       editable: false },
                { key: 'drugName',     label: 'Name',     editable: true  },
                { key: 'genericName',  label: 'Generic',  editable: true  },
                { key: 'category',     label: 'Category', editable: true  },
                { key: 'dosageForm',   label: 'Form',     editable: true  },
                { key: 'strength',     label: 'Strength', editable: true  },
                { key: 'mrp',          label: 'MRP ($)',  editable: true,  type: 'number' },
                { key: 'gstPercent',   label: 'GST %',    editable: true,  type: 'number' },
                { key: 'scheduleType', label: 'Schedule', editable: true  },
                { key: 'isActive',     label: 'Active',   editable: true,  type: 'bool'   },
            ]
        },
        'suppliers': {
            title:    'Supplier Directory',
            endpoint: '/suppliers/all?size=500',
            idField:  'supplierId',
            saveUrl:  (id) => `/suppliers/${id}`,
            cols: [
                { key: 'supplierId',       label: 'ID',         editable: false },
                { key: 'supplierName',     label: 'Name',       editable: true  },
                { key: 'contactPerson',    label: 'Contact',    editable: true  },
                { key: 'contactNumber',    label: 'Phone',      editable: true  },
                { key: 'email',            label: 'Email',      editable: true  },
                { key: 'city',             label: 'City',       editable: true  },
                { key: 'paymentTerms',     label: 'Terms',      editable: true  },
                { key: 'outstandingBalance', label: 'Balance ($)', editable: true, type: 'number' },
                { key: 'isActive',         label: 'Active',     editable: true,  type: 'bool'   },
            ]
        },
        'patients': {
            title:    'Patient Registry',
            endpoint: '/patients?size=500',
            idField:  'patientId',
            saveUrl:  (id) => `/patients/${id}`,
            cols: [
                { key: 'patientId',     label: 'ID',       editable: false },
                { key: 'fullName',      label: 'Name',     editable: true  },
                { key: 'dateOfBirth',   label: 'DOB',      editable: true  },
                { key: 'gender',        label: 'Gender',   editable: true  },
                { key: 'contactNumber', label: 'Phone',    editable: true  },
                { key: 'email',         label: 'Email',    editable: true  },
                { key: 'bloodGroup',    label: 'Blood',    editable: true  },
                { key: 'allergies',     label: 'Allergies',editable: true  },
                { key: 'isActive',      label: 'Active',   editable: true,  type: 'bool' },
            ]
        },
        'inventory': {
            title:    'Inventory Batches',
            endpoint: '/inventory?size=500',
            idField:  'inventoryId',
            saveUrl:  (id) => `/inventory/${id}`,
            cols: [
                { key: 'inventoryId',     label: 'ID',       editable: false },
                { key: 'drugName',        label: 'Drug',     editable: false },
                { key: 'batchNumber',     label: 'Batch',    editable: true  },
                { key: 'quantityInStock', label: 'Qty',      editable: true,  type: 'number' },
                { key: 'reorderLevel',    label: 'Reorder',  editable: true,  type: 'number' },
                { key: 'expiryDate',      label: 'Expiry',   editable: true  },
                { key: 'purchasePrice',   label: 'Buy ($)',  editable: true,  type: 'number' },
                { key: 'sellingPrice',    label: 'Sell ($)', editable: true,  type: 'number' },
                { key: 'stockStatus',     label: 'Status',   editable: false },
            ]
        },
        'purchase-orders': {
            title:    'Purchase Orders',
            endpoint: '/purchase-orders?size=500',
            idField:  'orderId',
            saveUrl:  null,
            cols: [
                { key: 'orderId',              label: 'ID',       editable: false },
                { key: 'poNumber',             label: 'PO #',     editable: false },
                { key: 'supplierName',         label: 'Supplier', editable: false },
                { key: 'orderDate',            label: 'Date',     editable: false },
                { key: 'status',               label: 'Status',   editable: false },
                { key: 'expectedDeliveryDate', label: 'Delivery', editable: false },
                { key: 'totalAmount',          label: 'Total ($)',editable: false },
                { key: 'orderedByName',        label: 'By',       editable: false },
                { key: 'notes',                label: 'Notes',    editable: false },
            ]
        },
        'users': {
            title:    'Staff Members',
            endpoint: '/users',
            idField:  'userId',
            saveUrl:  (id) => `/users/${id}`,
            cols: [
                { key: 'userId',        label: 'ID',      editable: false },
                { key: 'username',      label: 'Username',editable: false },
                { key: 'fullName',      label: 'Name',    editable: true  },
                { key: 'email',         label: 'Email',   editable: true  },
                { key: 'contactNumber', label: 'Phone',   editable: true  },
                { key: 'isActive',      label: 'Active',  editable: true,  type: 'bool' },
            ]
        }
    },

    // ── Load a table ────────────────────────────────────────────────────────
    async loadTable(tableName) {
        this.currentTable = tableName;
        const def = this.tables[tableName];
        if (!def) return;

        // Highlight the active tab button
        document.querySelectorAll('[id^="dbt-"]').forEach(b => {
            b.className = b.id === 'dbt-' + tableName
                ? 'btn btn-primary btn-sm'
                : 'btn btn-secondary btn-sm';
        });

        const titleEl    = document.getElementById('dbb-table-title');
        const countEl    = document.getElementById('dbb-row-count');
        const gridEl     = document.getElementById('dbb-grid');
        if (!titleEl || !gridEl) return;

        titleEl.textContent = def.title;
        if (countEl) countEl.textContent = '';
        gridEl.innerHTML = `<div style="padding:30px;text-align:center;color:var(--text-muted);">Loading ${def.title}…</div>`;

        try {
            const data  = await API.get(def.endpoint);
            this.rows   = Array.isArray(data) ? data : (data.content || []);
            if (countEl) countEl.textContent = `${this.rows.length} rows`;
            this._renderGrid(def, gridEl);
        } catch (e) {
            gridEl.innerHTML = `<div style="padding:30px;text-align:center;color:var(--accent-red);">Failed to load: ${e.message}</div>`;
        }
    },

    // ── Render the data grid ─────────────────────────────────────────────────
    _renderGrid(def, gridEl) {
        let thead = '<thead><tr style="position:sticky;top:0;z-index:2;">';
        def.cols.forEach(col => {
            thead += `<th style="padding:10px 12px;font-size:12px;font-weight:600;color:var(--text-secondary);white-space:nowrap;background:var(--card-bg);">${col.label}</th>`;
        });
        thead += '</tr></thead>';

        let tbodyHtml = '<tbody>';
        this.rows.forEach((row, rowIdx) => {
            const id = row[def.idField];
            tbodyHtml += `<tr style="border-bottom:1px solid rgba(255,255,255,0.05);">`;
            def.cols.forEach(col => {
                const val     = row[col.key];
                const display = this._display(val, col.type);
                if (col.editable && def.saveUrl) {
                    const safeVal = String(val ?? '').replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
                    tbodyHtml += `<td class="dbb-editable"
                        data-id="${id}"
                        data-field="${col.key}"
                        data-type="${col.type || 'text'}"
                        data-save="${def.saveUrl(id)}"
                        data-rowidx="${rowIdx}"
                        title="Double-click to edit"
                        ondblclick="DBBrowser.startEdit(this)"
                        style="padding:6px 8px;max-width:220px;cursor:pointer;">
                        <span class="dbb-cell-val" style="font-size:13px;">${display}</span>
                    </td>`;
                } else {
                    tbodyHtml += `<td style="padding:6px 8px;max-width:220px;font-size:13px;color:var(--text-secondary);">${display}</td>`;
                }
            });
            tbodyHtml += '</tr>';
        });
        tbodyHtml += '</tbody>';

        gridEl.innerHTML = `
            <table style="width:100%;border-collapse:collapse;font-size:13px;">
                ${thead}
                ${tbodyHtml}
            </table>
            ${def.saveUrl ? '<div style="padding:10px 16px;font-size:11px;color:var(--text-muted);">💡 Double-click any cell to edit inline — saves immediately on Enter or click-away.</div>' : '<div style="padding:10px 16px;font-size:11px;color:var(--text-muted);">This table is read-only.</div>'}
        `;
    },

    // ── Format a cell value for display ──────────────────────────────────────
    _display(val, type) {
        if (val === null || val === undefined) return '<span style="color:var(--text-muted);">—</span>';
        if (type === 'bool') {
            return val
                ? '<span class="badge badge-green" style="font-size:11px;">Yes</span>'
                : '<span class="badge badge-red"   style="font-size:11px;">No</span>';
        }
        if (Array.isArray(val)) {
            return new Date(val[0], val[1] - 1, val[2]).toLocaleDateString();
        }
        const str = String(val);
        if (str.length > 60) return `<span title="${str.replace(/"/g,'&quot;')}">${str.substring(0, 58)}…</span>`;
        return str.replace(/</g, '&lt;').replace(/>/g, '&gt;');
    },

    // ── Inline cell editor ───────────────────────────────────────────────────
    startEdit(td) {
        if (td.querySelector('input, select')) return; // already editing

        const field    = td.dataset.field;
        const type     = td.dataset.type || 'text';
        const saveUrl  = td.dataset.save;
        const rowIdx   = parseInt(td.dataset.rowidx);
        const row      = this.rows[rowIdx] || {};
        const currentVal = row[field];
        const origHtml   = td.innerHTML;

        let input;
        if (type === 'bool') {
            input = document.createElement('select');
            input.className = 'form-control';
            input.style.cssText = 'font-size:13px;padding:3px 6px;height:30px;width:90px;';
            input.innerHTML = `
                <option value="true"  ${currentVal === true  ? 'selected' : ''}>Yes</option>
                <option value="false" ${currentVal === false ? 'selected' : ''}>No</option>
            `;
        } else {
            input = document.createElement('input');
            input.type  = type === 'number' ? 'number' : 'text';
            if (type === 'number') input.step = 'any';
            input.className = 'form-control';
            input.style.cssText = 'font-size:13px;padding:3px 6px;height:30px;min-width:100px;max-width:200px;';
            input.value = currentVal ?? '';
        }

        td.innerHTML = '';
        td.appendChild(input);
        input.focus();
        if (input.type === 'text') { try { input.select(); } catch(e) {} }

        let saved = false;
        const doSave = async () => {
            if (saved) return;
            saved = true;

            let newVal = input.value;
            if (type === 'bool')   newVal = (newVal === 'true');
            if (type === 'number') newVal = parseFloat(newVal);

            // Restore display immediately
            td.innerHTML = origHtml;
            const span = td.querySelector('.dbb-cell-val');

            // No change — nothing to do
            if (String(newVal) === String(currentVal)) return;

            // Optimistic update
            if (row) row[field] = newVal;
            if (span) span.innerHTML = this._display(newVal, type);

            // Build full payload from the row (backend needs all fields for PUT)
            const fullRow = { ...(this.rows[rowIdx] || {}) };
            fullRow[field] = newVal;

            try {
                await API.put(saveUrl, fullRow);
                td.style.outline = '2px solid var(--accent-green)';
                setTimeout(() => { td.style.outline = ''; }, 1000);

                // Propagate change to live modules so they reflect instantly
                if (this.currentTable === 'drugs'     && typeof Catalog   !== 'undefined') Catalog.load();
                if (this.currentTable === 'suppliers'  && typeof Suppliers !== 'undefined') Suppliers.load();
                if (this.currentTable === 'patients'   && typeof Patients  !== 'undefined') Patients.load();
                if (this.currentTable === 'users'      && typeof Staff     !== 'undefined') Staff.load();
                if (this.currentTable === 'inventory'  && typeof Inventory !== 'undefined') Inventory.load();
            } catch (e) {
                td.style.outline = '2px solid var(--accent-red)';
                setTimeout(() => { td.style.outline = ''; }, 1500);
                App.toast(`Save failed: ${e.message}`, 'error');
                // Revert
                if (row) row[field] = currentVal;
                if (span) span.innerHTML = this._display(currentVal, type);
            }
        };

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter')  { e.preventDefault(); doSave(); }
            if (e.key === 'Escape') { saved = true; td.innerHTML = origHtml; }
        });
        input.addEventListener('blur', doSave);
    },

    // ── Refresh current table ─────────────────────────────────────────────────
    refresh() {
        if (this.currentTable) {
            if (this.currentTable === 'connection-settings') {
                this.openConnectionSettings();
            } else {
                this.loadTable(this.currentTable);
            }
        }
    },

    // ── Database Connection Settings ──────────────────────────────────────────
    async openConnectionSettings() {
        this.currentTable = 'connection-settings';

        // Unhighlight table buttons and highlight the settings button
        document.querySelectorAll('[id^="dbt-"]').forEach(b => {
            b.className = b.id === 'dbt-settings'
                ? 'btn btn-primary btn-sm'
                : 'btn btn-secondary btn-sm';
        });

        const titleEl = document.getElementById('dbb-table-title');
        const countEl = document.getElementById('dbb-row-count');
        const gridEl  = document.getElementById('dbb-grid');
        if (!titleEl || !gridEl) return;

        titleEl.textContent = 'Database Connection Profile';
        if (countEl) countEl.textContent = 'Active Settings';
        gridEl.innerHTML = `<div style="padding:30px;text-align:center;color:var(--text-muted);">Loading database connection profile…</div>`;

        try {
            const status = await API.get('/backup/db/status');
            const saved  = status.savedConfig || {};

            gridEl.innerHTML = `
                <div style="padding: 24px; max-width: 650px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; text-align: left;">
                    <!-- Active Status Card -->
                    <div class="glass-card" style="padding: 16px; border: 1px solid var(--accent-green); background: rgba(55, 139, 61, 0.05); text-align:left;">
                        <h4 style="margin:0 0 10px 0; color:var(--accent-green);">✓ Current Active Database</h4>
                        <div style="font-size:13px; line-height:1.6;">
                            <div><strong>Active Profile:</strong> <span style="font-family:var(--font-mono); font-weight:700;">${(status.activeProfile || 'mysql').toUpperCase()}</span></div>
                            <div><strong>Connection URL:</strong> <span style="font-family:var(--font-mono); color:var(--text-secondary); word-break:break-all;">${status.url || 'N/A'}</span></div>
                            <div><strong>Username:</strong> <span style="font-family:var(--font-mono); color:var(--text-secondary);">${status.username || 'N/A'}</span></div>
                        </div>
                    </div>
                    
                    <!-- Connection Form -->
                    <div class="glass-card" style="padding: 20px; display: flex; flex-direction: column; gap: 15px; text-align:left;">
                        <h4 style="margin: 0; color: var(--accent-blue);">Configure Connection Settings</h4>
                        
                        <div class="form-group">
                            <label style="display:block; margin-bottom:5px; font-weight:600; font-size:12px;">Database Server Type</label>
                            <select id="db-conn-type" class="form-control" onchange="DBBrowser.onDbTypeChange(this.value)">
                                <option value="mysql">MySQL Server</option>
                                <option value="sqlserver">Microsoft SQL Server (SQL Auth)</option>
                                <option value="sqlserver-winauth">Microsoft SQL Server (Windows Auth)</option>
                                <option value="h2">Embedded H2 Database (Portable)</option>
                            </select>
                        </div>
                        
                        <div id="db-network-fields">
                            <div class="form-grid" style="display:grid; grid-template-columns:3fr 1fr; gap:10px;">
                                <div class="form-group">
                                    <label style="display:block; margin-bottom:5px; font-weight:600; font-size:12px;">Server Host / IP</label>
                                    <input type="text" id="db-conn-host" class="form-control" placeholder="127.0.0.1" value="${saved.host || ''}">
                                </div>
                                <div class="form-group">
                                    <label style="display:block; margin-bottom:5px; font-weight:600; font-size:12px;">Port</label>
                                    <input type="text" id="db-conn-port" class="form-control" placeholder="3306" value="${saved.port || ''}">
                                </div>
                            </div>
                            
                            <div class="form-group" style="margin-top:10px;">
                                <label style="display:block; margin-bottom:5px; font-weight:600; font-size:12px;">Database Schema Name</label>
                                <input type="text" id="db-conn-name" class="form-control" placeholder="medicare_db" value="${saved.dbName || ''}">
                            </div>
                        </div>
                        
                        <div id="db-credentials-fields" style="margin-top:10px;">
                            <div class="form-grid" style="display:grid; grid-template-columns:1fr 1fr; gap:10px;">
                                <div class="form-group">
                                    <label style="display:block; margin-bottom:5px; font-weight:600; font-size:12px;">Database Username</label>
                                    <input type="text" id="db-conn-user" class="form-control" placeholder="root" value="${saved.username || ''}">
                                </div>
                                <div class="form-group">
                                    <label style="display:block; margin-bottom:5px; font-weight:600; font-size:12px;">Database Password</label>
                                    <input type="password" id="db-conn-pass" class="form-control" placeholder="Enter password (stays hidden)..." value="">
                                </div>
                            </div>
                        </div>
                        
                        <div style="display: flex; gap: 12px; justify-content: flex-end; margin-top: 15px; border-top: 1px solid var(--border-color); padding-top: 15px;">
                            <button class="btn btn-secondary" onclick="DBBrowser.testDbConnection()" id="db-test-btn" style="min-width:140px;">Test Connection</button>
                            <button class="btn btn-primary" onclick="DBBrowser.applyDbConnection()" id="db-save-btn" style="min-width:180px;">Save & Apply Settings</button>
                        </div>
                        
                        <div id="db-test-result" style="display:none; padding:12px; border-radius:6px; font-size:13px; line-height:1.5; margin-top:10px;"></div>
                    </div>
                </div>
            `;

            // Initialize values and triggers
            const currentDb = saved.db || 'mysql';
            document.getElementById('db-conn-type').value = currentDb;
            this.onDbTypeChange(currentDb);
        } catch (e) {
            gridEl.innerHTML = `<div style="padding:30px;text-align:center;color:var(--accent-red);">Failed to load connection status: ${e.message}</div>`;
        }
    },

    onDbTypeChange(type) {
        const networkFields = document.getElementById('db-network-fields');
        const credFields    = document.getElementById('db-credentials-fields');
        const hostInput     = document.getElementById('db-conn-host');
        const portInput     = document.getElementById('db-conn-port');
        const nameInput     = document.getElementById('db-conn-name');
        const userInput     = document.getElementById('db-conn-user');
        
        if (!networkFields || !credFields) return;

        if (type === 'h2') {
            networkFields.style.display = 'none';
            credFields.style.display = 'none';
        } else if (type === 'sqlserver-winauth') {
            networkFields.style.display = 'block';
            credFields.style.display = 'none';
            if (portInput && !portInput.value) portInput.placeholder = '1433';
            if (nameInput && !nameInput.value) nameInput.placeholder = 'PharmacyDB';
        } else { // mysql or sqlserver
            networkFields.style.display = 'block';
            credFields.style.display = 'block';
            if (portInput) portInput.placeholder = (type === 'sqlserver') ? '1433' : '3306';
            if (nameInput) nameInput.placeholder = (type === 'sqlserver') ? 'PharmacyDB' : 'medicare_db';
            if (userInput) userInput.placeholder = (type === 'sqlserver') ? 'sa' : 'root';
        }
    },

    async testDbConnection() {
        const btn = document.getElementById('db-test-btn');
        const resEl = document.getElementById('db-test-result');
        if (!btn || !resEl) return;

        btn.disabled = true;
        btn.textContent = 'Testing connection…';
        resEl.style.display = 'none';

        const payload = {
            db:       document.getElementById('db-conn-type').value,
            host:     document.getElementById('db-conn-host') ? document.getElementById('db-conn-host').value : '',
            port:     document.getElementById('db-conn-port') ? document.getElementById('db-conn-port').value : '',
            dbName:   document.getElementById('db-conn-name') ? document.getElementById('db-conn-name').value : '',
            username: document.getElementById('db-conn-user') ? document.getElementById('db-conn-user').value : '',
            password: document.getElementById('db-conn-pass') ? document.getElementById('db-conn-pass').value : '',
        };

        try {
            const res = await API.post('/backup/db/test', payload);
            resEl.style.display = 'block';
            if (res.success) {
                resEl.style.background = 'rgba(55, 139, 61, 0.1)';
                resEl.style.color = 'var(--accent-green)';
                resEl.style.border = '1px solid var(--accent-green)';
                resEl.innerHTML = `<strong>Success:</strong> ${res.message}`;
            } else {
                resEl.style.background = 'rgba(192, 57, 43, 0.1)';
                resEl.style.color = 'var(--accent-red)';
                resEl.style.border = '1px solid var(--accent-red)';
                resEl.innerHTML = `<strong>Connection Failed:</strong> ${res.message}`;
            }
        } catch (e) {
            resEl.style.display = 'block';
            resEl.style.background = 'rgba(192, 57, 43, 0.1)';
            resEl.style.color = 'var(--accent-red)';
            resEl.style.border = '1px solid var(--accent-red)';
            resEl.innerHTML = `<strong>Error:</strong> ${e.message}`;
        } finally {
            btn.disabled = false;
            btn.textContent = 'Test Connection';
        }
    },

    async applyDbConnection() {
        const btn = document.getElementById('db-save-btn');
        const resEl = document.getElementById('db-test-result');
        if (!btn) return;

        btn.disabled = true;
        btn.textContent = 'Applying settings…';

        const payload = {
            db:       document.getElementById('db-conn-type').value,
            host:     document.getElementById('db-conn-host') ? document.getElementById('db-conn-host').value : '',
            port:     document.getElementById('db-conn-port') ? document.getElementById('db-conn-port').value : '',
            dbName:   document.getElementById('db-conn-name') ? document.getElementById('db-conn-name').value : '',
            username: document.getElementById('db-conn-user') ? document.getElementById('db-conn-user').value : '',
            password: document.getElementById('db-conn-pass') ? document.getElementById('db-conn-pass').value : '',
        };

        try {
            App.toast('Saving settings and restarting background services...', 'info');
            const res = await API.post('/backup/db/switch', payload);
            if (resEl) {
                resEl.style.display = 'block';
                resEl.style.background = 'rgba(55, 139, 61, 0.1)';
                resEl.style.color = 'var(--accent-green)';
                resEl.style.border = '1px solid var(--accent-green)';
                resEl.innerHTML = `<strong>Success:</strong> ${res.message}`;
            }
            App.toast('Restart scheduled in 2 seconds!', 'success');
            // Give 2 seconds to show message and let application exit
            setTimeout(() => {
                if (window.javaBridge && window.javaBridge.close) {
                    window.javaBridge.close();
                }
            }, 2000);
        } catch (e) {
            App.toast('Failed to save connection profile: ' + e.message, 'error');
            btn.disabled = false;
            btn.textContent = 'Save & Apply Settings';
        }
    }
};

const Backup = {
    loadDbStatus() {},
    async createBackup() {
        try {
            App.toast('Generating database backup...', 'info');
            const token = localStorage.getItem('access_token');
            const res = await fetch('/api/backup/download', {
                headers: {
                    'Authorization': token ? `Bearer ${token}` : ''
                }
            });
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const blob = await res.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            const contentDisp = res.headers.get('Content-Disposition');
            let filename = `medicare_db_backup_${new Date().toISOString().slice(0,10)}.sql`;
            if (contentDisp && contentDisp.includes('filename=')) {
                filename = contentDisp.split('filename=')[1].trim();
            }
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
            App.toast('Backup downloaded successfully!', 'success');
        } catch (e) {
            console.error('Backup failed', e);
            App.toast('Backup failed: ' + e.message, 'error');
        }
    }
};
