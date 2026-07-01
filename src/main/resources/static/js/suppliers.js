const Suppliers = {
    suppliers: [],
    pos: [],
    drugs: [],
    selectedPOItems: [],

    async load() {
        await this.fetchSuppliers();
    },

    // ─────────────────────────────────────────────
    // SUPPLIERS
    // ─────────────────────────────────────────────

    async fetchSuppliers() {
        try {
            const data = await API.get('/suppliers/all?size=200');
            const all = data.content || data || [];
            // Active first (newest at top), inactive pushed to bottom
            this.suppliers = all.sort((a, b) => {
                const aActive = a.isActive !== false;
                const bActive = b.isActive !== false;
                if (aActive !== bActive) return aActive ? -1 : 1;
                return (b.supplierId || 0) - (a.supplierId || 0);
            });
            this.renderList();
        } catch (e) {
            // Fallback to active-only
            try {
                const data2 = await API.get('/suppliers?size=200');
                this.suppliers = (data2.content || []).sort((a, b) => (b.supplierId || 0) - (a.supplierId || 0));
                this.renderList();
            } catch (e2) {
                App.toast('Failed to load supplier directories.', 'error');
            }
        }
    },

    renderList() {
        const tbody = document.getElementById('suppliers-table-body');
        tbody.innerHTML = '';

        if (this.suppliers.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;color:var(--text-muted);padding:30px;">No suppliers registered in directory.</td></tr>`;
            return;
        }

        const fmt = (val) => App.formatCurrency(val);

        this.suppliers.forEach(s => {
            const tr = document.createElement('tr');
            const inactive = s.isActive === false;
            if (inactive) tr.style.opacity = '0.55';

            tr.innerHTML = `
                <td>
                    <div style="font-weight:600;">${s.supplierName}</div>
                    ${inactive ? '<span class="badge badge-red" style="font-size:10px;margin-top:2px;">INACTIVE</span>' : ''}
                </td>
                <td>${s.contactPerson || 'N/A'}</td>
                <td style="font-family:var(--font-mono);">${s.contactNumber || 'N/A'}</td>
                <td><span style="font-size:13px;color:var(--text-secondary);">${s.email || 'N/A'}</span></td>
                <td><span class="badge badge-muted">${s.city || 'N/A'}</span></td>
                <td style="font-family:var(--font-mono);font-weight:600;color:var(--accent-red);">${fmt(s.outstandingBalance)}</td>
                <td><span class="badge badge-blue">${s.paymentTerms || 'COD'}</span></td>
                <td><span class="badge ${inactive ? 'badge-red' : 'badge-green'}">${inactive ? 'Inactive' : 'Active'}</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="Suppliers.openEditModal(${s.supplierId})">Edit</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    },

    openAddModal() {
        this.openSupplierFormModal();
    },

    openSupplierFormModal(sup = null) {
        const isEdit = sup !== null;
        const title  = isEdit ? 'Edit Supplier Record' : 'Register New Supplier';

        const html = `
            <form id="supplier-form" style="display:flex;flex-direction:column;gap:20px;">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Supplier / Vendor Name</label>
                        <input type="text" id="sup-name" class="form-control" placeholder="e.g. Acme Pharma" value="${sup ? sup.supplierName : ''}" required>
                    </div>
                    <div class="form-group">
                        <label>Contact Person</label>
                        <input type="text" id="sup-contact" class="form-control" placeholder="e.g. Jane Doe" value="${sup ? sup.contactPerson || '' : ''}" required>
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>Contact Phone</label>
                        <input type="text" id="sup-phone" class="form-control" placeholder="+1 555 123 456" value="${sup ? sup.contactNumber || '' : ''}" required>
                    </div>
                    <div class="form-group">
                        <label>Email Address</label>
                        <input type="email" id="sup-email" class="form-control" placeholder="sales@vendor.com" value="${sup ? sup.email || '' : ''}">
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>City / Location</label>
                        <input type="text" id="sup-city" class="form-control" placeholder="e.g. New York" value="${sup ? sup.city || '' : ''}" required>
                    </div>
                    <div class="form-group">
                        <label>Payment Terms</label>
                        <select id="sup-terms" class="form-control">
                            <option value="NET_30" ${sup && sup.paymentTerms === 'NET_30' ? 'selected' : ''}>Net 30 days</option>
                            <option value="NET_15" ${sup && sup.paymentTerms === 'NET_15' ? 'selected' : ''}>Net 15 days</option>
                            <option value="COD"    ${sup && sup.paymentTerms === 'COD'    ? 'selected' : ''}>Cash on Delivery</option>
                            <option value="ADVANCE"${sup && sup.paymentTerms === 'ADVANCE'? 'selected' : ''}>Advance Payment</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label>Vendor Address</label>
                    <textarea id="sup-address" class="form-control" rows="2" placeholder="Complete warehouse / office location...">${sup ? sup.address || '' : ''}</textarea>
                </div>
                ${isEdit ? `
                <div class="form-group">
                    <label>Status</label>
                    <select id="sup-status" class="form-control">
                        <option value="true"  ${sup.isActive !== false ? 'selected' : ''}>Active</option>
                        <option value="false" ${sup.isActive === false  ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>` : ''}
                <div style="display:flex;justify-content:flex-end;gap:12px;border-top:1px solid var(--border-color);padding-top:15px;">
                    <button type="button" class="btn btn-secondary" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'UPDATE SUPPLIER' : 'REGISTER SUPPLIER'}</button>
                </div>
            </form>
        `;

        App.openModal(title, html);
        document.getElementById('supplier-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.submitSupplier(isEdit ? sup.supplierId : null);
        });
    },

    async openEditModal(supplierId) {
        const sup = this.suppliers.find(s => s.supplierId === supplierId);
        if (sup) this.openSupplierFormModal(sup);
    },

    async submitSupplier(supplierId) {
        const supplierName  = document.getElementById('sup-name').value;
        const contactPerson = document.getElementById('sup-contact').value;
        const contactNumber = document.getElementById('sup-phone').value;
        const email         = document.getElementById('sup-email').value;
        const city          = document.getElementById('sup-city').value;
        const paymentTerms  = document.getElementById('sup-terms').value;
        const address       = document.getElementById('sup-address').value;
        const statusEl      = document.getElementById('sup-status');
        const isActive      = statusEl ? statusEl.value === 'true' : true;

        const payload = { supplierName, contactPerson, contactNumber, email, city, paymentTerms, address, isActive, outstandingBalance: 0.0 };

        try {
            if (supplierId) {
                const original = this.suppliers.find(s => s.supplierId === supplierId);
                await API.put(`/suppliers/${supplierId}`, payload);
                if (original && original.isActive !== false && !isActive) {
                    App.toast(`Supplier "${supplierName}" marked as Inactive.`, 'warning');
                } else {
                    App.toast(`Supplier ${supplierName} updated successfully.`, 'success');
                }
            } else {
                await API.post('/suppliers', payload);
                App.toast(`Supplier ${supplierName} registered.`, 'success');
            }
            App.closeModal();
            await this.load();
        } catch (e) {
            App.toast(`Saving supplier failed: ${e.message}`, 'error');
        }
    },

    // ─────────────────────────────────────────────
    // PURCHASE ORDERS
    // ─────────────────────────────────────────────

    async loadPOs() {
        try {
            const data = await API.get('/purchase-orders?size=100');
            this.pos = data.content || [];
            this.renderPOList();
        } catch (e) {
            App.toast('Failed to load purchase orders.', 'error');
        }
    },

    PO_STATUSES: ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'PARTIALLY_DELIVERED', 'CANCELLED'],

    poStatusBadge(status) {
        const s = (status || '').toUpperCase();
        if (s === 'CANCELLED')           return 'badge-red';
        if (s === 'DELIVERED')           return 'badge-green';
        if (s === 'PARTIALLY_DELIVERED') return 'badge-green';
        return 'badge-orange';
    },

    fmtDate(val) {
        if (!val) return 'N/A';
        if (Array.isArray(val)) return new Date(val[0], val[1] - 1, val[2]).toLocaleDateString();
        return new Date(val).toLocaleDateString();
    },

    renderPOList() {
        const tbody = document.getElementById('po-table-body');
        tbody.innerHTML = '';

        if (this.pos.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--text-muted);padding:30px;">No Purchase Orders dispatched yet.</td></tr>`;
            return;
        }

        const fmt = (val) => App.formatCurrency(parseFloat(val) || 0);

        this.pos.forEach(p => {
            const statusBadge = this.poStatusBadge(p.status);

            // Build status dropdown options
            const statusOptions = this.PO_STATUSES.map(s =>
                `<option value="${s}" ${s === p.status ? 'selected' : ''}>${s.replace('_', ' ')}</option>`
            ).join('');

            // Summary row
            const tr = document.createElement('tr');
            tr.id = `po-row-${p.orderId}`;
            tr.innerHTML = `
                <td style="font-family:var(--font-mono);font-weight:700;color:var(--accent-blue);">${p.poNumber}</td>
                <td style="font-weight:600;" title="${p.supplierName || 'N/A'}">${p.supplierName || 'N/A'}</td>
                <td style="font-family:var(--font-mono);white-space:nowrap;">${this.fmtDate(p.orderDate)}</td>
                <td>
                    <select class="po-status-select" data-po-id="${p.orderId}" data-current="${p.status}"
                        style="background:var(--bg-primary);border:1px solid var(--border-color);border-radius:6px;
                               padding:4px 8px;font-size:12px;font-weight:600;color:var(--text-primary);cursor:pointer;
                               min-width:155px;">
                        ${statusOptions}
                    </select>
                </td>
                <td style="font-family:var(--font-mono);font-weight:700;white-space:nowrap;">${fmt(p.totalAmount)}</td>
                <td style="white-space:nowrap;">
                    <button class="btn btn-secondary btn-sm" onclick="Suppliers.togglePODetails(${p.orderId}, this)" data-open="false">▶ Inspect</button>
                </td>
            `;
            tbody.appendChild(tr);

            // Collapsible detail row
            const detailTr = document.createElement('tr');
            detailTr.id = `po-detail-${p.orderId}`;
            detailTr.style.display = 'none';
            detailTr.innerHTML = `
                <td colspan="6" style="padding:0;background:rgba(0,0,0,0.12);">
                    <div id="po-detail-content-${p.orderId}" style="padding:16px 24px;">
                        <span style="color:var(--text-muted);font-size:13px;">Loading…</span>
                    </div>
                </td>
            `;
            tbody.appendChild(detailTr);
        });

        // Attach change listener to all status dropdowns
        tbody.querySelectorAll('.po-status-select').forEach(sel => {
            sel.addEventListener('change', (e) => {
                const poId      = parseInt(e.target.dataset.poId);
                const newStatus = e.target.value;
                const oldStatus = e.target.dataset.current;
                this.changePoStatus(poId, newStatus, oldStatus, e.target);
            });
        });
    },

    async changePoStatus(poId, newStatus, oldStatus, selectEl) {
        // Optimistically update the UI immediately
        selectEl.disabled = true;
        selectEl.style.opacity = '0.6';
        selectEl.dataset.current = newStatus;

        try {
            await API.put(`/purchase-orders/${poId}/status?status=${encodeURIComponent(newStatus)}`);
            App.toast(`Status updated to ${newStatus.replace('_', ' ')}.`, 'success');

            // If changed to DELIVERED, also trigger inventory creation in background
            if (newStatus === 'DELIVERED') {
                try {
                    await API.put(`/purchase-orders/${poId}/receive`);
                    App.toast('Inventory batches created automatically.', 'success');
                } catch (invErr) {
                    // Inventory creation failure doesn't block the status change
                    App.toast(`Status saved. Inventory note: ${invErr.message}`, 'warning');
                }
            }

            // Reload to sync everything
            await this.loadPOs();
            if (typeof Dashboard !== 'undefined') Dashboard.load();

        } catch (e) {
            // Revert the dropdown on failure
            selectEl.value = oldStatus;
            selectEl.dataset.current = oldStatus;
            selectEl.disabled = false;
            selectEl.style.opacity = '1';
            App.toast(`Failed to update status: ${e.message}`, 'error');
        }
    },

    async togglePODetails(poId, btn) {
        const detailRow = document.getElementById(`po-detail-${poId}`);
        const isOpen    = btn.getAttribute('data-open') === 'true';

        if (isOpen) {
            detailRow.style.display = 'none';
            btn.setAttribute('data-open', 'false');
            btn.textContent = '▶ Inspect';
            return;
        }

        detailRow.style.display = 'table-row';
        btn.setAttribute('data-open', 'true');
        btn.textContent = '▼ Collapse';

        const contentDiv = document.getElementById(`po-detail-content-${poId}`);
        if (contentDiv.dataset.loaded === 'true') return;

        try {
            const po  = await API.get(`/purchase-orders/${poId}`);
            const fmt = (val) => App.formatCurrency(parseFloat(val) || 0);
            const statusBadge = this.poStatusBadge(po.status);

            let itemRows = '';
            (po.items || []).forEach(item => {
                const up  = parseFloat(item.unitPrice) || 0;
                const qty = parseInt(item.orderedQuantity) || 0;
                itemRows += `
                    <tr>
                        <td style="font-weight:600;padding:6px 10px;">${item.drugName}</td>
                        <td style="padding:6px 10px;">${qty} units</td>
                        <td style="font-family:var(--font-mono);padding:6px 10px;">${fmt(up)}</td>
                        <td style="font-family:var(--font-mono);font-weight:700;padding:6px 10px;">${fmt(qty * up)}</td>
                    </tr>
                `;
            });

            contentDiv.innerHTML = `
                <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;font-size:13px;margin-bottom:14px;">
                    <div><span style="color:var(--text-secondary);">PO Number:</span> <strong style="color:var(--accent-blue);">${po.poNumber}</strong></div>
                    <div><span style="color:var(--text-secondary);">Supplier:</span> <strong>${po.supplierName || 'N/A'}</strong></div>
                    <div><span style="color:var(--text-secondary);">Status:</span> <span class="badge ${statusBadge}">${po.status}</span></div>
                    <div><span style="color:var(--text-secondary);">Order Date:</span> ${this.fmtDate(po.orderDate)}</div>
                    <div><span style="color:var(--text-secondary);">Expected Delivery:</span> ${this.fmtDate(po.expectedDeliveryDate)}</div>
                    <div><span style="color:var(--text-secondary);">Ordered By:</span> ${po.orderedByName || 'System'}</div>
                    <div><span style="color:var(--text-secondary);">Total:</span> <strong style="color:var(--accent-green);">${fmt(po.totalAmount)}</strong></div>
                    ${po.actualDeliveryDate ? `<div><span style="color:var(--text-secondary);">Delivered On:</span> ${this.fmtDate(po.actualDeliveryDate)}</div>` : ''}
                    ${po.notes ? `<div style="grid-column:span 2;"><span style="color:var(--text-secondary);">Notes:</span> ${po.notes}</div>` : ''}
                </div>
                <table style="width:100%;font-size:13px;border-collapse:collapse;">
                    <thead>
                        <tr style="border-bottom:1px solid var(--border-color);">
                            <th style="text-align:left;padding:6px 10px;color:var(--text-secondary);font-weight:600;">Medication</th>
                            <th style="text-align:left;padding:6px 10px;color:var(--text-secondary);font-weight:600;">Qty</th>
                            <th style="text-align:left;padding:6px 10px;color:var(--text-secondary);font-weight:600;">Unit Cost</th>
                            <th style="text-align:left;padding:6px 10px;color:var(--text-secondary);font-weight:600;">Total</th>
                        </tr>
                    </thead>
                    <tbody>${itemRows || '<tr><td colspan="4" style="padding:10px;color:var(--text-muted);">No items found.</td></tr>'}</tbody>
                </table>
            `;
            contentDiv.dataset.loaded = 'true';
        } catch (e) {
            contentDiv.innerHTML = `<span style="color:var(--accent-red);">Failed to load details: ${e.message}</span>`;
        }
    },

    async openPOModal() {
        try {
            const sData = await API.get('/suppliers?size=100');
            const suppliersList = (sData.content || []).filter(s => s.isActive !== false);

            const dData = await API.get('/drugs?size=100');
            this.drugs = dData.content || [];

            if (suppliersList.length === 0) {
                App.toast('Please register at least one active supplier before creating a PO.', 'warning');
                return;
            }

            let supplierOptions = suppliersList.map(s => `<option value="${s.supplierId}">${s.supplierName}</option>`).join('');
            this.selectedPOItems = [];

            const html = `
                <div style="display:flex;flex-direction:column;gap:20px;">
                    <form id="po-create-form" style="display:flex;flex-direction:column;gap:15px;">
                        <div class="form-grid">
                            <div class="form-group">
                                <label>Target Supplier</label>
                                <select id="po-supplier-id" class="form-control" required>${supplierOptions}</select>
                            </div>
                            <div class="form-group">
                                <label>Estimated Delivery Date</label>
                                <input type="date" id="po-delivery-date" class="form-control" required value="${new Date(Date.now() + 7*24*60*60*1000).toISOString().split('T')[0]}">
                            </div>
                        </div>
                        <div class="form-group">
                            <label>Notes / Instructions</label>
                            <input type="text" id="po-notes" class="form-control" placeholder="Shipping terms, urgent requirements...">
                        </div>
                    </form>

                    <div style="border-top:1px solid var(--border-color);padding-top:15px;">
                        <h4 style="color:var(--accent-purple);margin-bottom:10px;">Add Medications</h4>
                        <div class="form-grid" style="align-items:flex-end;gap:10px;">
                            <div class="form-group" style="flex:2;">
                                <label>Drug</label>
                                <select id="po-add-drug-id" class="form-control"></select>
                            </div>
                            <div class="form-group" style="flex:1;">
                                <label>Quantity</label>
                                <input type="number" id="po-add-qty" class="form-control" min="1" value="100">
                            </div>
                            <div class="form-group" style="flex:1;">
                                <label>Unit Price ($)</label>
                                <input type="number" step="0.01" id="po-add-price" class="form-control" placeholder="0.00">
                            </div>
                            <button type="button" class="btn btn-primary" onclick="Suppliers.addPOItemRow()" style="height:42px;">Add</button>
                        </div>
                        <div class="table-container" style="margin-top:15px;max-height:180px;overflow-y:auto;">
                            <table class="premium-table" style="font-size:13px;">
                                <thead><tr><th>Medication</th><th>Qty</th><th>Unit Cost</th><th>Total</th><th>Remove</th></tr></thead>
                                <tbody id="po-builder-rows">
                                    <tr><td colspan="5" style="text-align:center;color:var(--text-muted);">No items added yet.</td></tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div style="display:flex;justify-content:flex-end;gap:12px;border-top:1px solid var(--border-color);padding-top:15px;">
                        <button type="button" class="btn btn-secondary" onclick="App.closeModal()">Cancel</button>
                        <button type="button" class="btn btn-primary" onclick="Suppliers.submitPO()">DISPATCH PURCHASE ORDER</button>
                    </div>
                </div>
            `;

            App.openModal('Dispatch New Purchase Order', html);

            const drugSelect = document.getElementById('po-add-drug-id');
            drugSelect.innerHTML = this.drugs.filter(d => d.isActive).map(d => `<option value="${d.drugId}">${d.drugName}</option>`).join('');

            const updatePrice = () => {
                const d = this.drugs.find(d => d.drugId === parseInt(drugSelect.value));
                if (d && d.mrp) document.getElementById('po-add-price').value = (d.mrp * 0.7).toFixed(2);
            };
            drugSelect.addEventListener('change', updatePrice);
            updatePrice();
        } catch (e) {
            App.toast('Failed to launch PO creator: ' + e.message, 'error');
        }
    },

    addPOItemRow() {
        const drugId    = parseInt(document.getElementById('po-add-drug-id').value);
        const qty       = parseInt(document.getElementById('po-add-qty').value);
        const unitPrice = parseFloat(document.getElementById('po-add-price').value);

        if (isNaN(qty) || qty <= 0)       { App.toast('Enter a valid quantity.', 'warning');   return; }
        if (isNaN(unitPrice) || unitPrice <= 0) { App.toast('Enter a valid price.', 'warning'); return; }

        const drug = this.drugs.find(d => d.drugId === drugId);
        if (!drug) return;

        const existing = this.selectedPOItems.find(i => i.drugId === drugId);
        if (existing) {
            existing.orderedQuantity += qty;
            existing.totalPrice = existing.orderedQuantity * existing.unitPrice;
        } else {
            this.selectedPOItems.push({ drugId, drugName: drug.drugName, orderedQuantity: qty, unitPrice, totalPrice: qty * unitPrice });
        }
        this.renderPOBuilderRows();
    },

    removePOItemRow(index) {
        this.selectedPOItems.splice(index, 1);
        this.renderPOBuilderRows();
    },

    renderPOBuilderRows() {
        const tbody = document.getElementById('po-builder-rows');
        tbody.innerHTML = '';
        if (this.selectedPOItems.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--text-muted);">No items added yet.</td></tr>`;
            return;
        }
        this.selectedPOItems.forEach((item, i) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="font-weight:600;">${item.drugName}</td>
                <td>${item.orderedQuantity} units</td>
                <td style="font-family:var(--font-mono);">$${item.unitPrice.toFixed(2)}</td>
                <td style="font-family:var(--font-mono);font-weight:700;color:var(--accent-green);">$${item.totalPrice.toFixed(2)}</td>
                <td><button class="btn btn-danger btn-sm" onclick="Suppliers.removePOItemRow(${i})">Remove</button></td>
            `;
            tbody.appendChild(tr);
        });
    },

    async submitPO() {
        if (this.selectedPOItems.length === 0) { App.toast('Add at least one medication.', 'warning'); return; }

        // Robust guard against double-submit — use a module-level flag
        if (this._submittingPO) return;
        this._submittingPO = true;

        const dispatchBtn = document.querySelector('[onclick="Suppliers.submitPO()"]');
        if (dispatchBtn) {
            dispatchBtn.disabled = true;
            dispatchBtn.textContent = 'Dispatching…';
        }

        const supplierId           = document.getElementById('po-supplier-id').value;
        const expectedDeliveryDate = document.getElementById('po-delivery-date').value;
        const notes                = document.getElementById('po-notes').value;
        const userInfo             = API.getUserInfo();

        const payload = {
            supplierId:   parseInt(supplierId),
            orderedById:  userInfo?.userId || 1,
            orderDate:    new Date().toISOString().split('T')[0],
            expectedDeliveryDate,
            notes,
            status: 'PENDING',
            items: this.selectedPOItems
        };

        try {
            await API.post('/purchase-orders', payload);
            App.toast('Purchase Order dispatched successfully.', 'success');
            this._submittingPO = false;
            App.closeModal();
            await this.loadPOs();
        } catch (e) {
            App.toast(`Dispatch failed: ${e.message}`, 'error');
            this._submittingPO = false;
            if (dispatchBtn) {
                dispatchBtn.disabled = false;
                dispatchBtn.textContent = 'DISPATCH PURCHASE ORDER';
            }
        }
    }
};

// ─────────────────────────────────────────────
// APPLICATION SETTINGS
// ─────────────────────────────────────────────
const AppSettings = {
    // In-memory cache — loaded once at startup from the backend
    _cache: null,

    _defaults() {
        return {
            hospitalName:   'MediCare Pharmacy',
            ownerName:      '',
            address:        '',
            phone:          '',
            email:          '',
            invoiceFooter:  'Thank you for choosing us. Get well soon!',
            currencySymbol: '$',
            taxLabel:       'GST',
            licenseNumber:  '',
        };
    },

    /** Load settings from backend (persisted in ~/.medicare/app-settings.json) */
    async fetchFromServer() {
        try {
            const data = await API.get('/settings');
            this._cache = { ...this._defaults(), ...data };
        } catch (e) {
            console.error('Could not load app settings from server, using defaults:', e.message);
            this._cache = this._defaults();
        }
        return this._cache;
    },

    /**
     * Synchronous read from in-memory cache.
     * Falls back to defaults if cache is not yet populated.
     * Always call fetchFromServer() first (done at login time).
     */
    load() {
        return this._cache ? { ...this._cache } : this._defaults();
    },

    get(key) {
        const s = this.load();
        return s[key] ?? this._defaults()[key];
    },

    /** Save settings to backend and update in-memory cache */
    async saveToServer(settings) {
        const merged = { ...this._defaults(), ...settings };
        try {
            const saved = await API.post('/settings', merged);
            this._cache = { ...this._defaults(), ...saved };
            return true;
        } catch (e) {
            console.error('Failed to save app settings:', e.message);
            throw e;
        }
    },

    openModal() {
        const s = this.load();
        const html = `
            <div style="display:flex;flex-direction:column;gap:18px;">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Hospital / Pharmacy Name</label>
                        <input type="text" id="set-hospital-name" class="form-control" value="${s.hospitalName}" placeholder="e.g. MediCare Pharmacy">
                    </div>
                    <div class="form-group">
                        <label>Owner / Proprietor Name</label>
                        <input type="text" id="set-owner-name" class="form-control" value="${s.ownerName || ''}" placeholder="e.g. Dr. John Smith">
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>License / Registration Number</label>
                        <input type="text" id="set-license" class="form-control" value="${s.licenseNumber}" placeholder="e.g. PH-2024-00142">
                    </div>
                    <div class="form-group">
                        <label>Address</label>
                        <input type="text" id="set-address" class="form-control" value="${s.address}" placeholder="Full pharmacy address">
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>Phone Number</label>
                        <input type="text" id="set-phone" class="form-control" value="${s.phone}" placeholder="+1 555 000 0000">
                    </div>
                    <div class="form-group">
                        <label>Email Address</label>
                        <input type="email" id="set-email" class="form-control" value="${s.email}" placeholder="info@pharmacy.com">
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>Currency Symbol</label>
                        <input type="text" id="set-currency" class="form-control" value="${s.currencySymbol}" placeholder="$">
                    </div>
                    <div class="form-group">
                        <label>Tax Label (on invoices)</label>
                        <input type="text" id="set-tax-label" class="form-control" value="${s.taxLabel}" placeholder="GST / VAT / Tax">
                    </div>
                </div>
                 <div class="form-group">
                    <label>Invoice Footer Message</label>
                    <input type="text" id="set-invoice-footer" class="form-control" value="${s.invoiceFooter}" placeholder="Thank you message shown on receipts">
                </div>
                
                <div style="display:flex;justify-content:flex-end;gap:12px;border-top:1px solid var(--border-color);padding-top:15px;margin-top:10px;">
                    <button type="button" class="btn btn-secondary" onclick="App.closeModal()">Cancel</button>
                    <button type="button" class="btn btn-primary" id="settings-save-btn" onclick="AppSettings.saveFromForm()">SAVE SETTINGS</button>
                </div>
            </div>
        `;
        App.openModal('Application Settings', html);
    },

    async saveFromForm() {
        const btn = document.getElementById('settings-save-btn');
        if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }

        const updated = {
            hospitalName:   document.getElementById('set-hospital-name').value.trim(),
            ownerName:      document.getElementById('set-owner-name').value.trim(),
            licenseNumber:  document.getElementById('set-license').value.trim(),
            address:        document.getElementById('set-address').value.trim(),
            phone:          document.getElementById('set-phone').value.trim(),
            email:          document.getElementById('set-email').value.trim(),
            currencySymbol: document.getElementById('set-currency').value.trim() || '$',
            taxLabel:       document.getElementById('set-tax-label').value.trim() || 'GST',
            invoiceFooter:  document.getElementById('set-invoice-footer').value.trim(),
        };

        try {
            await this.saveToServer(updated);
            App.closeModal();
            App.toast('Settings saved successfully.', 'success');
            if (typeof App !== 'undefined' && App.activeTab) {
                App.loadTabContents(App.activeTab);
            }
        } catch (e) {
            App.toast('Failed to save settings: ' + e.message, 'error');
            if (btn) { btn.disabled = false; btn.textContent = 'SAVE SETTINGS'; }
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const search = document.getElementById('suppliers-search');
    if (search) {
        search.addEventListener('input', App.debounce((e) => {
            const q = e.target.value.toLowerCase();
            document.querySelectorAll('#suppliers-table-body tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
            });
        }));
    }
});
