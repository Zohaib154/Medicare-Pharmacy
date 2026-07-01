const Catalog = {
    drugs: [],

    async load() {
        await this.fetchDrugs();
    },

    async fetchDrugs() {
        try {
            // Fetch all drugs including inactive (same pattern as suppliers)
            const data = await API.get('/drugs/all?size=200&sortBy=drugName');
            const all = data.content || [];
            // Active first (alphabetical), inactive pushed to bottom
            this.drugs = all.sort((a, b) => {
                const aActive = a.isActive !== false;
                const bActive = b.isActive !== false;
                if (aActive !== bActive) return aActive ? -1 : 1;
                return (a.drugName || '').localeCompare(b.drugName || '');
            });
            this.renderList();
        } catch (e) {
            // Fallback to active-only
            try {
                const data2 = await API.get('/drugs?size=200&sortBy=drugName');
                this.drugs = data2.content || [];
                this.renderList();
            } catch (e2) {
                App.toast('Failed to load medication catalogue.', 'error');
            }
        }
    },

    renderList() {
        const tbody = document.getElementById('drugs-table-body');
        tbody.innerHTML = '';

        if (this.drugs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;color:var(--text-muted);padding:30px;">No medications registered in the catalog.</td></tr>`;
            return;
        }

        const fmt = (val) => App.formatCurrency(val);

        this.drugs.forEach(d => {
            const tr = document.createElement('tr');
            const inactive = d.isActive === false;
            if (inactive) tr.style.opacity = '0.55';

            tr.innerHTML = `
                <td>
                    <div style="font-weight:600;">${d.drugName}</div>
                    ${inactive ? '<span class="badge badge-red" style="font-size:10px;margin-top:2px;">INACTIVE</span>' : ''}
                </td>
                <td><span style="font-size:13px;color:var(--text-secondary);">${d.genericName || 'N/A'}</span></td>
                <td><span class="badge badge-blue">${d.category || 'General'}</span></td>
                <td><span class="badge badge-muted">${d.dosageForm || 'Tablet'}</span> <span style="font-size:12px;color:var(--text-secondary);">${d.strength || ''}</span></td>
                <td style="font-family:var(--font-mono);font-weight:700;">${fmt(d.mrp)}</td>
                <td style="font-family:var(--font-mono);">${d.gstPercent || '12'}%</td>
                <td><span class="badge badge-orange">${d.scheduleType || 'NONE'}</span></td>
                <td><span class="badge ${inactive ? 'badge-red' : 'badge-green'}">${inactive ? 'Inactive' : 'Active'}</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="Catalog.openEditModal(${d.drugId})">Edit</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    },

    openAddModal() {
        this.openDrugFormModal(null);
    },

    async openEditModal(drugId) {
        const drug = this.drugs.find(d => d.drugId === drugId);
        if (drug) this.openDrugFormModal(drug);
    },

    openDrugFormModal(d = null) {
        const isEdit = d !== null;
        const title  = isEdit ? 'Edit Medication' : 'Add New Medication to Catalog';

        const html = `
            <form id="drug-form" style="display:flex;flex-direction:column;gap:18px;">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Medication Name</label>
                        <input type="text" id="drug-name" class="form-control" placeholder="e.g. Paracetamol 500mg" value="${d ? d.drugName : ''}" required>
                    </div>
                    <div class="form-group">
                        <label>Generic Chemical Name</label>
                        <input type="text" id="drug-generic" class="form-control" placeholder="e.g. Acetaminophen" value="${d ? d.genericName || '' : ''}" required>
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>Category</label>
                        <input type="text" id="drug-category" class="form-control" placeholder="e.g. Analgesics" value="${d ? d.category || '' : ''}" required>
                    </div>
                    <div class="form-group">
                        <label>Dosage Form</label>
                        <select id="drug-form" class="form-control" required>
                            <option value="TABLET"   ${d && d.dosageForm === 'TABLET'    ? 'selected' : ''}>Tablet</option>
                            <option value="CAPSULE"  ${d && d.dosageForm === 'CAPSULE'   ? 'selected' : ''}>Capsule</option>
                            <option value="LIQUID"   ${d && d.dosageForm === 'LIQUID'    ? 'selected' : ''}>Liquid Suspension</option>
                            <option value="INJECTION"${d && d.dosageForm === 'INJECTION' ? 'selected' : ''}>Injection</option>
                            <option value="CREAM"    ${d && d.dosageForm === 'CREAM'     ? 'selected' : ''}>Ointment / Cream</option>
                            <option value="INHALER"  ${d && d.dosageForm === 'INHALER'   ? 'selected' : ''}>Inhaler / Spray</option>
                        </select>
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>Strength / Spec</label>
                        <input type="text" id="drug-strength" class="form-control" placeholder="e.g. 500 mg" value="${d ? d.strength || '' : ''}" required>
                    </div>
                    <div class="form-group">
                        <label>Selling Price (MRP)</label>
                        <input type="number" step="0.01" id="drug-mrp" class="form-control" placeholder="0.00" value="${d ? d.mrp || '' : ''}" required>
                    </div>
                </div>
                <div class="form-grid">
                    <div class="form-group">
                        <label>GST Tax %</label>
                        <input type="number" step="0.1" id="drug-gst" class="form-control" value="${d ? d.gstPercent || 12 : 12}" required>
                    </div>
                    <div class="form-group">
                        <label>Schedule / Prescription Category</label>
                        <select id="drug-schedule" class="form-control">
                            <option value="NONE"        ${d && d.scheduleType === 'NONE'         ? 'selected' : ''}>Over the Counter (None)</option>
                            <option value="SCHEDULE_H"  ${d && d.scheduleType === 'SCHEDULE_H'   ? 'selected' : ''}>Schedule H (Rx Required)</option>
                            <option value="SCHEDULE_H1" ${d && d.scheduleType === 'SCHEDULE_H1'  ? 'selected' : ''}>Schedule H1 (Narcotic/Restricted)</option>
                            <option value="SCHEDULE_G"  ${d && d.scheduleType === 'SCHEDULE_G'   ? 'selected' : ''}>Schedule G (Medical Supervised)</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label>Brief Description</label>
                    <textarea id="drug-desc" class="form-control" placeholder="Chemical description and indications..." rows="2">${d ? d.description || '' : ''}</textarea>
                </div>
                ${isEdit ? `
                <div class="form-group">
                    <label>Status</label>
                    <select id="drug-status" class="form-control">
                        <option value="true"  ${d.isActive !== false ? 'selected' : ''}>Active</option>
                        <option value="false" ${d.isActive === false  ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>` : ''}
                <div style="display:flex;justify-content:flex-end;gap:12px;border-top:1px solid var(--border-color);padding-top:15px;">
                    <button type="button" class="btn btn-secondary" onclick="App.closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'SAVE CHANGES' : 'ADD TO CATALOG'}</button>
                </div>
            </form>
        `;

        App.openModal(title, html);
        document.getElementById('drug-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.submitDrug(isEdit ? d.drugId : null, d);
        });
    },

    async submitDrug(drugId, original) {
        const drugName    = document.getElementById('drug-name').value;
        const genericName = document.getElementById('drug-generic').value;
        const category    = document.getElementById('drug-category').value;
        const dosageForm  = document.getElementById('drug-form').value;
        const strength    = document.getElementById('drug-strength').value;
        const mrp         = parseFloat(document.getElementById('drug-mrp').value);
        const gstPercent  = parseFloat(document.getElementById('drug-gst').value);
        const scheduleType= document.getElementById('drug-schedule').value;
        const description = document.getElementById('drug-desc').value;
        const statusEl    = document.getElementById('drug-status');
        const isActive    = statusEl ? statusEl.value === 'true' : true;

        const payload = { drugName, genericName, category, dosageForm, strength, mrp, gstPercent, scheduleType, description, isActive };

        try {
            if (drugId) {
                await API.put(`/drugs/${drugId}`, payload);
                if (original && original.isActive !== false && !isActive) {
                    App.toast(`"${drugName}" marked as Inactive and hidden from POS/orders.`, 'warning');
                } else {
                    App.toast(`Medication ${drugName} updated.`, 'success');
                }
            } else {
                await API.post('/drugs', payload);
                App.toast(`Medication ${drugName} added successfully.`, 'success');
            }
            App.closeModal();
            await this.load();
        } catch (e) {
            App.toast(`Failed to save: ${e.message}`, 'error');
        }
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const search = document.getElementById('drugs-search');
    if (search) {
        search.addEventListener('input', App.debounce((e) => {
            const q = e.target.value.toLowerCase();
            document.querySelectorAll('#drugs-table-body tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
            });
        }));
    }
});
