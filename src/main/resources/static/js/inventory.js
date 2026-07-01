const Inventory = {
    batches: [],
    drugs: [],
    suppliers: [],

    async load() {
        await this.fetchInventory();
    },

    async fetchInventory() {
        try {
            const data = await API.get('/inventory?size=100');
            this.batches = data.content || [];
            this.renderList();
        } catch (e) {
            App.toast('Failed to load inventory batches.', 'error');
        }
    },

    renderList() {
        const tbody = document.getElementById('inv-table-body');
        tbody.innerHTML = '';

        if (this.batches.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; color: var(--text-muted); padding: 30px;">
                        No stock batches found in inventory.
                    </td>
                </tr>
            `;
            return;
        }

        this.batches.forEach(b => {
            const tr = document.createElement('tr');
            
            let statusBadge = 'badge-green';
            if (b.stockStatus === 'EXPIRED') statusBadge = 'badge-red';
            if (b.stockStatus === 'OUT_OF_STOCK') statusBadge = 'badge-red';
            if (b.stockStatus === 'LOW_STOCK') statusBadge = 'badge-orange';
            if (b.stockStatus === 'RECALLED') statusBadge = 'badge-muted';

            const fmt = (val) => App.formatCurrency(val);
            const expDate = new Date(b.expiryDate).toLocaleDateString();

            tr.innerHTML = `
                <td style="font-weight: 600;">${b.drugName}</td>
                <td style="font-family: var(--font-mono); font-weight: 600;">${b.batchNumber}</td>
                <td style="font-weight: 600;">${b.quantityInStock} units</td>
                <td style="font-family: var(--font-mono);">${expDate}</td>
                <td style="font-family: var(--font-mono); color: var(--text-secondary);">${fmt(b.purchasePrice)}</td>
                <td style="font-family: var(--font-mono); font-weight: 700; color: var(--accent-blue);">${fmt(b.sellingPrice)}</td>
                <td><span class="badge badge-muted">${b.storageLocation || 'Aisle 1'}</span></td>
                <td><span class="badge ${statusBadge}">${b.stockStatus}</span></td>
            `;
            tbody.appendChild(tr);
        });
    },

    async openAddModal() {
        try {
            // Fetch dependencies
            const drugsData = await API.get('/drugs?size=100');
            this.drugs = drugsData.content || [];

            const suppliersData = await API.get('/suppliers?size=100');
            this.suppliers = suppliersData.content || [];

            let drugOptions = '';
            this.drugs.forEach(d => {
                if (d.isActive) {
                    drugOptions += `<option value="${d.drugId}">${d.drugName} (MRP: ${App.formatCurrency(d.mrp)})</option>`;
                }
            });

            let supplierOptions = '<option value="">No Supplier</option>';
            this.suppliers.forEach(s => {
                supplierOptions += `<option value="${s.supplierId}">${s.supplierName}</option>`;
            });

            const html = `
                <form id="inv-create-form" style="display: flex; flex-direction: column; gap: 20px;">
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Medication / Drug</label>
                            <select id="inv-drug-id" class="form-control" required>
                                ${drugOptions}
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Batch Number</label>
                            <input type="text" id="inv-batch-no" class="form-control" placeholder="e.g. BATCH-2026-X" required>
                        </div>
                    </div>

                    <div class="form-grid">
                        <div class="form-group">
                            <label>Quantity In Stock</label>
                            <input type="number" id="inv-quantity" class="form-control" min="1" value="100" required>
                        </div>
                        <div class="form-group">
                            <label>Reorder Level (Alert Limit)</label>
                            <input type="number" id="inv-reorder" class="form-control" min="1" value="20" required>
                        </div>
                    </div>

                    <div class="form-grid">
                        <div class="form-group">
                            <label>Manufacturing Date</label>
                            <input type="date" id="inv-mfg-date" class="form-control" required value="${new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]}">
                        </div>
                        <div class="form-group">
                            <label>Expiry Date</label>
                            <input type="date" id="inv-expiry-date" class="form-control" required value="${new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]}">
                        </div>
                    </div>

                    <div class="form-grid">
                        <div class="form-group">
                            <label>Purchase Price (Buying rate)</label>
                            <input type="number" step="0.01" id="inv-purchase-price" class="form-control" placeholder="0.00" required>
                        </div>
                        <div class="form-group">
                            <label>Selling Price (Selling rate / MRP)</label>
                            <input type="number" step="0.01" id="inv-selling-price" class="form-control" placeholder="0.00" required>
                        </div>
                    </div>

                    <div class="form-grid">
                        <div class="form-group">
                            <label>Shelf / Storage Location</label>
                            <input type="text" id="inv-location" class="form-control" placeholder="e.g. Shelf A-4" value="Shelf A-1">
                        </div>
                        <div class="form-group">
                            <label>Supplier / Vendor</label>
                            <select id="inv-supplier-id" class="form-control">
                                ${supplierOptions}
                            </select>
                        </div>
                    </div>

                    <div style="display: flex; justify-content: flex-end; gap: 12px; border-top:1px solid var(--border-color); padding-top:15px;">
                        <button type="button" class="btn btn-secondary" onclick="App.closeModal()">Cancel</button>
                        <button type="submit" class="btn btn-primary">ADD STOCK BATCH</button>
                    </div>
                </form>
            `;

            App.openModal('Add New Inventory Stock Batch', html);

            // Populate default pricing based on selected drug
            const drugSelect = document.getElementById('inv-drug-id');
            const updatePricing = () => {
                const selectedDrugId = parseInt(drugSelect.value);
                const drugObj = this.drugs.find(d => d.drugId === selectedDrugId);
                if (drugObj) {
                    // Set default selling price to drug's MRP
                    document.getElementById('inv-selling-price').value = drugObj.mrp.toFixed(2);
                    // Default buying price to 70% of MRP
                    document.getElementById('inv-purchase-price').value = (drugObj.mrp * 0.7).toFixed(2);
                }
            };
            
            drugSelect.addEventListener('change', updatePricing);
            updatePricing(); // Run once initially

            // Form submit binder
            document.getElementById('inv-create-form').addEventListener('submit', (e) => {
                e.preventDefault();
                this.submitNewStock();
            });

        } catch (e) {
            App.toast('Failed to load stock entry form.', 'error');
        }
    },

    async submitNewStock() {
        const drugId = document.getElementById('inv-drug-id').value;
        const supplierId = document.getElementById('inv-supplier-id').value;
        const batchNumber = document.getElementById('inv-batch-no').value;
        const quantityInStock = document.getElementById('inv-quantity').value;
        const reorderLevel = document.getElementById('inv-reorder').value;
        const manufacturingDate = document.getElementById('inv-mfg-date').value;
        const expiryDate = document.getElementById('inv-expiry-date').value;
        const purchasePrice = document.getElementById('inv-purchase-price').value;
        const sellingPrice = document.getElementById('inv-selling-price').value;
        const storageLocation = document.getElementById('inv-location').value;

        const payload = {
            drugId: parseInt(drugId),
            supplierId: supplierId ? parseInt(supplierId) : null,
            batchNumber,
            quantityInStock: parseInt(quantityInStock),
            reorderLevel: parseInt(reorderLevel),
            manufacturingDate,
            expiryDate,
            purchasePrice: parseFloat(purchasePrice),
            sellingPrice: parseFloat(sellingPrice),
            storageLocation
        };

        try {
            await API.post('/inventory', payload);
            App.toast(`Stock batch ${batchNumber} added to inventory.`, 'success');
            App.closeModal();
            await this.load();
            Dashboard.load(); // Refresh dashboard KPIs
        } catch (e) {
            App.toast(`Adding stock failed: ${e.message}`, 'error');
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    // Bind search bar filter
    const search = document.getElementById('inv-search');
    if (search) {
        search.addEventListener('input', App.debounce((e) => {
            const query = e.target.value.toLowerCase();
            document.querySelectorAll('#inv-table-body tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(query) ? '' : 'none';
            });
        }));
    }
});
