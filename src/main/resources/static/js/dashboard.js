const Dashboard = {
    async load() {
        try {
            const metrics = await API.get('/dashboard');
            this.renderMetrics(metrics);
            await this.loadChart();
        } catch (e) {
            console.error('Failed to load dashboard metrics', e);
            App.toast('Could not fetch dashboard KPIs.', 'error');
        }
    },

    async loadChart() {
        try {
            const data = await API.get('/dashboard/revenue-chart');
            const container = document.getElementById('db-revenue-chart');
            if (!container) return;
            container.innerHTML = '';

            const maxRevenue = Math.max(...data.map(d => d.revenue || 0), 10);

            data.forEach(d => {
                const pct = ((d.revenue || 0) / maxRevenue) * 100;
                const barWrapper = document.createElement('div');
                barWrapper.style = 'flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end;';
                
                const fmt = (val) => App.formatCurrency(val, 0);
                
                barWrapper.innerHTML = `
                    <div style="font-size: 11px; font-weight: 600; margin-bottom: 4px; color: var(--accent-green);">${d.revenue > 0 ? fmt(d.revenue) : ''}</div>
                    <div style="width: 70%; height: ${Math.max(pct, 5)}%; background: linear-gradient(180deg, var(--accent-primary) 0%, #a7f3d0 100%); border-radius: 4px 4px 0 0; transition: height 0.5s ease;"></div>
                    <div style="font-size: 12px; font-weight: 600; color: var(--text-secondary); margin-top: 8px;">${d.day}</div>
                `;
                container.appendChild(barWrapper);
            });
        } catch (e) {
            console.error('Failed to load dashboard chart', e);
        }
    },

    renderMetrics(m) {
        // Format cash amounts helper
        const fmt = (val) => App.formatCurrency(val);

        // Standard KPI numbers
        document.getElementById('db-revenue').textContent = fmt(m.todayRevenue);
        document.getElementById('db-monthly-revenue').textContent = fmt(m.monthRevenue);
        document.getElementById('db-out-stock').textContent = m.outOfStockCount || 0;
        document.getElementById('db-expiring').textContent = m.expiringIn30Days || 0;
        
        // Operations Summary
        document.getElementById('db-today-sales').textContent = m.todayTransactions || 0;
        document.getElementById('db-avg-bill').textContent = fmt(m.avgBillValue);
        document.getElementById('db-total-patients').textContent = m.totalPatients || 0;

        // Render top drugs
        const topDrugsTableBody = document.getElementById('db-top-drugs-list');
        topDrugsTableBody.innerHTML = '';
        
        const topDrugs = m.topSellingDrugs || [];
        if (topDrugs.length === 0) {
            topDrugsTableBody.innerHTML = `
                <tr>
                    <td colspan="3" style="text-align: center; color: var(--text-muted); padding: 30px;">
                        No transactions registered this month.
                    </td>
                </tr>
            `;
            return;
        }

        topDrugs.forEach(drug => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="font-weight: 600;">${drug.drugName}</td>
                <td><span class="badge badge-blue">${drug.unitsSold} units</span></td>
                <td style="font-family: var(--font-mono); font-weight: 700; color: var(--accent-green);">${fmt(drug.revenue)}</td>
            `;
            topDrugsTableBody.appendChild(tr);
        });
    }
};
