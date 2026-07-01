const POS = {
    cart: [],
    drugs: [],
    patients: [],
    currentReceipt: null,

    async load() {
        this.cart = [];
        this.updateCartUI();

        // Reset totals and fields
        document.getElementById('pos-discount').value = 0;
        document.getElementById('pos-amount-paid').value = "";

        // Dynamically update placeholder to current currency symbol
        const settings = (typeof AppSettings !== 'undefined') ? AppSettings.load() : {};
        const symbol = settings.currencySymbol || '$';
        const amtPaidInput = document.getElementById('pos-amount-paid');
        if (amtPaidInput) {
            amtPaidInput.placeholder = `${symbol} Amount tendered`;
        }

        this.calculateTotals(0, 0, 0);

        await this.fetchPatients();
        await this.fetchDrugs();
    },

    async fetchPatients() {
        try {
            // Patient pageable default size 100
            const data = await API.get('/patients?size=100');
            this.patients = data.content || [];
            
            const select = document.getElementById('pos-select-patient');
            select.innerHTML = '<option value="">Walk-in Customer</option>';
            this.patients.forEach(p => {
                select.innerHTML += `<option value="${p.patientId}">${p.fullName} (${p.contactNumber || 'No Contact'})</option>`;
            });
        } catch (e) {
            console.error('Failed to fetch patients for POS', e);
        }
    },

    async fetchDrugs() {
        try {
            const drugsWithStock = await API.get('/drugs/with-stock');
            this.drugs = drugsWithStock || [];

            const drugListTable = document.getElementById('pos-drug-list-table');
            drugListTable.innerHTML = '';

            for (const d of this.drugs) {
                const totalStock = d.totalStock != null ? d.totalStock : 0;
                const earliestBatch = '—';

                const tr = document.createElement('tr');
                
                let stockBadgeClass = 'badge-green';
                let stockStatusText = `${totalStock} in stock`;
                if (totalStock === 0) {
                    stockBadgeClass = 'badge-red';
                    stockStatusText = 'Out of Stock';
                } else if (totalStock < 20) {
                    stockBadgeClass = 'badge-orange';
                    stockStatusText = `Low Stock (${totalStock})`;
                }

                const priceFormatted = App.formatCurrency(d.mrp);

                tr.innerHTML = `
                    <td>
                        <div style="font-weight: 600;">${d.drugName}</div>
                        <div style="font-size: 12px; color: var(--text-secondary);">${d.genericName || ''}</div>
                    </td>
                    <td><span class="badge badge-muted">${d.dosageForm || 'Tablet'}</span> <span style="font-size:12px;">${d.strength || ''}</span></td>
                    <td style="font-family: var(--font-mono); font-size:13px;">${earliestBatch}</td>
                    <td><span class="badge ${stockBadgeClass}">${stockStatusText}</span></td>
                    <td style="font-family: var(--font-mono); font-weight: 700; color: var(--accent-primary);">${priceFormatted}</td>
                    <td>
                        <button class="btn btn-primary btn-sm" onclick="POS.addToCart(${d.drugId})" ${totalStock === 0 ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : ''}>
                            ADD TO BILL
                        </button>
                    </td>
                `;
                drugListTable.appendChild(tr);
            }
        } catch (e) {
            App.toast('Failed to load drug catalogue.', 'error');
        }
    },

    addToCart(drugId) {
        const drug = this.drugs.find(d => d.drugId === drugId);
        if (!drug) return;

        const cartItem = this.cart.find(item => item.drugId === drugId);
        if (cartItem) {
            cartItem.quantity += 1;
        } else {
            this.cart.push({
                drugId: drug.drugId,
                drugName: drug.drugName,
                mrp: drug.mrp,
                gstPercent: drug.gstPercent || 12.00,
                quantity: 1
            });
        }
        
        App.toast(`${drug.drugName} added to register drawer.`, 'success');
        this.updateCartUI();
    },

    updateQty(drugId, change) {
        const item = this.cart.find(item => item.drugId === drugId);
        if (!item) return;

        item.quantity += change;
        if (item.quantity <= 0) {
            this.removeFromCart(drugId);
        } else {
            this.updateCartUI();
        }
    },

    removeFromCart(drugId) {
        const idx = this.cart.findIndex(item => item.drugId === drugId);
        if (idx !== -1) {
            const name = this.cart[idx].drugName;
            this.cart.splice(idx, 1);
            App.toast(`${name} removed from register drawer.`, 'warning');
            this.updateCartUI();
        }
    },

    updateCartUI() {
        const cartList = document.getElementById('pos-cart-list');
        cartList.innerHTML = '';

        document.getElementById('pos-cart-badge').textContent = `${this.cart.length} Items`;

        if (this.cart.length === 0) {
            cartList.innerHTML = `
                <div style="flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center; color: var(--text-muted); text-align:center; padding: 40px 0;">
                    <svg style="width: 48px; height: 48px; stroke: currentColor; fill:none; stroke-width:1.5; margin-bottom:12px;" viewBox="0 0 24 24"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>
                    <p style="font-weight: 500;">Register Drawer Empty</p>
                    <p style="font-size:12px; margin-top:4px;">Add medications from the catalog on the left to start billing.</p>
                </div>
            `;
            this.calculateTotals(0, 0, 0);
            return;
        }

        this.cart.forEach(item => {
            const priceFormatted = App.formatCurrency(item.mrp * item.quantity);
            
            const div = document.createElement('div');
            div.className = 'cart-item';
            div.innerHTML = `
                <div class="cart-item-info">
                    <div class="cart-item-title">${item.drugName}</div>
                    <div class="cart-item-meta">${App.formatCurrency(item.mrp)} each (Tax: ${item.gstPercent}%)</div>
                </div>
                <div class="cart-item-qty">
                    <div class="qty-btn" onclick="POS.updateQty(${item.drugId}, -1)">-</div>
                    <div style="font-weight: 600; font-size:14px; min-width:20px; text-align:center;">${item.quantity}</div>
                    <div class="qty-btn" onclick="POS.updateQty(${item.drugId}, 1)">+</div>
                </div>
                <div style="font-family: var(--font-mono); font-weight: 700; font-size: 14px; min-width: 70px; text-align: right;">${priceFormatted}</div>
                <div style="color: var(--accent-red); cursor: pointer; display: flex; align-items: center;" onclick="POS.removeFromCart(${item.drugId})">
                    <svg style="width:18px; height:18px; stroke:currentColor; fill:none; stroke-width:2;" viewBox="0 0 24 24"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                </div>
            `;
            cartList.appendChild(div);
        });

        this.recalculateCartValues();
    },

    recalculateCartValues() {
        let subtotal = 0;
        let totalGst = 0;
        const discountPct = parseFloat(document.getElementById('pos-discount').value) || 0;

        this.cart.forEach(item => {
            const lineTotal = item.mrp * item.quantity;
            const itemGst = lineTotal * (item.gstPercent / 100);
            subtotal += lineTotal;
            totalGst += itemGst;
        });

        const discountAmount = subtotal * (discountPct / 100);
        const grandTotal = (subtotal - discountAmount) + totalGst;

        this.calculateTotals(subtotal, totalGst, grandTotal);
    },

    calculateTotals(subtotal, gst, total) {
        const fmt = (val) => App.formatCurrency(val);
        document.getElementById('pos-subtotal').textContent = fmt(subtotal);
        document.getElementById('pos-gst').textContent = fmt(gst);
        document.getElementById('pos-total').textContent = fmt(total);
        
        // Pre-fill amount paid if empty or less than total
        const amtPaidInput = document.getElementById('pos-amount-paid');
        if (!amtPaidInput.value || parseFloat(amtPaidInput.value) < total) {
            amtPaidInput.value = total.toFixed(2);
        }
    },

    async finalizeCheckout() {
        if (this.cart.length === 0) {
            App.toast('No items in checkout cart.', 'warning');
            return;
        }

        const user = API.getUserInfo();
        const patientId = document.getElementById('pos-select-patient').value;
        const paymentMethod = document.getElementById('pos-payment-method').value;
        const discountPercent = parseFloat(document.getElementById('pos-discount').value) || 0;
        const amountPaid = parseFloat(document.getElementById('pos-amount-paid').value) || 0;

        const requestBody = {
            soldById: user.userId,
            patientId: patientId ? parseInt(patientId) : null,
            paymentMethod: paymentMethod,
            discountPercent: discountPercent,
            amountPaid: amountPaid,
            items: this.cart.map(item => ({
                drugId: item.drugId,
                quantity: item.quantity,
                unitPrice: item.mrp,
                discountPercent: 0
            }))
        };

        try {
            const completedSale = await API.post('/sales', requestBody);
            App.toast(`Bill ${completedSale.billNumber} created successfully!`, 'success');
            
            // Launch Receipt Modal
            this.showReceipt(completedSale);
            
            // Clear and reload POS
            this.load();
        } catch (e) {
            App.toast(`Checkout failed: ${e.message}`, 'error');
        }
    },

    showReceipt(s) {
        this.currentReceipt = s;
        const fmt = (val) => App.formatCurrency(val);

        // Pull from AppSettings (persisted on server, loaded at login)
        const settings = (typeof AppSettings !== 'undefined') ? AppSettings.load() : {};
        const hospitalName   = settings.hospitalName   || 'MediCare Pharmacy';
        const ownerName      = settings.ownerName      || '';
        const address        = settings.address        || '';
        const phone          = settings.phone          || '';
        const invoiceFooter  = settings.invoiceFooter  || 'Thank you for your business!';
        const taxLabel       = settings.taxLabel       || 'GST';

        let itemsHtml = '';
        s.items.forEach(item => {
            itemsHtml += `
                <tr>
                    <td>${item.drugName} x ${item.quantity}</td>
                    <td style="text-align: right;">${fmt(item.unitPrice)}</td>
                    <td style="text-align: right;">${fmt(item.totalPrice)}</td>
                </tr>
            `;
        });

        const patientName = s.patientName || 'Walk-in Customer';
        const dateStr = App.formatLocalDateTime(s.saleDateTime);
        const discountPct = parseFloat(s.discountPercent) || 0;
        const discountAmt = parseFloat(s.discountAmount) || 0;

        const html = `
            <div class="receipt-wrapper">
                <div class="receipt-header">
                    <div class="receipt-title">${hospitalName}</div>
                    ${ownerName ? `<div style="font-size: 12px; font-weight:600; margin-top:3px;">${ownerName}</div>` : ''}
                    ${address ? `<div style="font-size: 11px; margin-top:4px;">${address}</div>` : ''}
                    ${phone   ? `<div style="font-size: 11px;">Tel: ${phone}</div>` : ''}
                </div>
                
                <div style="margin-bottom: 12px;">
                    <div class="receipt-details"><span>Bill Code:</span> <strong class="selectable">${s.billNumber}</strong></div>
                    <div class="receipt-details"><span>Date:</span> <span>${dateStr}</span></div>
                    <div class="receipt-details"><span>Operator:</span> <span>${s.soldByName}</span></div>
                    <div class="receipt-details"><span>Customer:</span> <span>${patientName}</span></div>
                    <div class="receipt-details"><span>Method:</span> <span>${s.paymentMethod}</span></div>
                </div>

                <table class="receipt-table">
                    <thead>
                        <tr>
                            <th>Item Details</th>
                            <th style="text-align: right;">Rate</th>
                            <th style="text-align: right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${itemsHtml}
                    </tbody>
                </table>

                <div class="receipt-totals">
                    <div class="receipt-details"><span>Subtotal:</span> <span>${fmt(s.subtotal)}</span></div>
                    ${discountPct > 0 ? `<div class="receipt-details receipt-discount"><span>Bill Discount (${discountPct}%):</span> <span>-${fmt(discountAmt)}</span></div>` : ''}
                    <div class="receipt-details"><span>${taxLabel} Tax Total:</span> <span>${fmt(s.gstAmount)}</span></div>
                    <div class="receipt-details" style="font-size:16px; font-weight:700; border-top: 1px dashed black; padding-top:6px; margin-top:6px;">
                        <span>GRAND TOTAL:</span> <span>${fmt(s.totalAmount)}</span>
                    </div>
                    <div class="receipt-details" style="margin-top:6px;"><span>Cash Tendered:</span> <span>${fmt(s.amountPaid)}</span></div>
                    <div class="receipt-details"><span>Change Returned:</span> <span>${fmt(s.changeReturned)}</span></div>
                </div>

                <div class="receipt-footer">
                    <div>${invoiceFooter}</div>
                    <div style="font-size:10px; color:gray; margin-top:6px;">Please retain this receipt as proof of purchase.</div>
                    <div style="font-size:9px; color:lightgray; margin-top:4px; font-family:var(--font-sans)">* Barcode Signature: ${s.billNumber}-${s.saleId} *</div>
                </div>
            </div>
            <div class="no-print" style="display:flex; justify-content: flex-end; gap:12px; margin-top:20px; flex-wrap: wrap;">
                <button class="btn btn-secondary" onclick="App.closeModal()">Close Window</button>
                <button class="btn btn-primary" onclick="POS.printReceipt()">Print Invoice</button>
            </div>
        `;

        App.openModal('POS Sale Invoice', html);
    },

    printReceipt() {
        const onAfterPrint = () => {
            window.removeEventListener('afterprint', onAfterPrint);
        };
        window.addEventListener('afterprint', onAfterPrint);

        try {
            window.print();
        } catch (e) {
            console.error('Print failed', e);
            App.toast('Print is unavailable in this browser environment.', 'warning');
            return;
        }

        setTimeout(() => {
            if (document.hasFocus()) {
                App.toast('Tip: Choose "Microsoft Print to PDF" in the print dialog to save a copy.', 'info');
            }
        }, 1500);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    // Bind checkout button click
    const btn = document.getElementById('pos-checkout-btn');
    if (btn) {
        btn.addEventListener('click', () => POS.finalizeCheckout());
    }

    // Bind discount change recalculations
    const disc = document.getElementById('pos-discount');
    if (disc) {
        disc.addEventListener('input', () => POS.recalculateCartValues());
    }

    const search = document.getElementById('pos-search-drug');
    if (search) {
        search.addEventListener('input', App.debounce((e) => {
            const query = e.target.value.toLowerCase();
            document.querySelectorAll('#pos-drug-list-table tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(query) ? '' : 'none';
            });
        }));
    }
});
