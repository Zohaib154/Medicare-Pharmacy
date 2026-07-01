/**
 * Lightweight client-side PDF generator for POS receipts (no external CDN).
 * Works in browser and JavaFX WebView; desktop save uses javaBridge.savePdf when available.
 */
const ReceiptPdf = {
    escapePdf(text) {
        return String(text || '')
            .replace(/\\/g, '\\\\')
            .replace(/\(/g, '\\(')
            .replace(/\)/g, '\\)');
    },

    buildLines(sale) {
        const fmt = (val) => App.formatCurrency(val);
        const dateStr = App.formatLocalDateTime(sale.saleDateTime);
        const discountPct = parseFloat(sale.discountPercent) || 0;
        const discountAmt = parseFloat(sale.discountAmount) || 0;

        // Pull from AppSettings
        const settings = (typeof AppSettings !== 'undefined') ? AppSettings.load() : {};
        const hospitalName  = settings.hospitalName  || 'MediCare Pharmacy';
        const ownerName     = settings.ownerName     || '';
        const address       = settings.address       || '';
        const phone         = settings.phone         || '';
        const invoiceFooter = settings.invoiceFooter || 'Thank you for your business!';
        const taxLabel      = settings.taxLabel      || 'GST';

        const lines = [
            hospitalName,
        ];
        if (ownerName) lines.push(ownerName);
        if (address)   lines.push(address);
        if (phone)     lines.push(`Tel: ${phone}`);
        lines.push(
            '',
            `Bill Code: ${sale.billNumber}`,
            `Date: ${dateStr}`,
            `Operator: ${sale.soldByName || '—'}`,
            `Customer: ${sale.patientName || 'Walk-in Customer'}`,
            `Payment: ${sale.paymentMethod || '—'}`,
            '',
            'Item                          Rate      Amount',
            '------------------------------------------------'
        );

        (sale.items || []).forEach(item => {
            const name = `${item.drugName} x ${item.quantity}`;
            lines.push(`${name.substring(0, 28).padEnd(28)} ${fmt(item.unitPrice).padStart(8)} ${fmt(item.totalPrice).padStart(8)}`);
        });

        lines.push('------------------------------------------------');
        lines.push(`Subtotal:${fmt(sale.subtotal).padStart(42)}`);
        if (discountPct > 0) {
            lines.push(`Discount (${discountPct}%):${('-' + fmt(discountAmt)).padStart(35)}`);
        }
        lines.push(`${taxLabel} Tax Total:${fmt(sale.gstAmount).padStart(36)}`);
        lines.push(`GRAND TOTAL:${fmt(sale.totalAmount).padStart(38)}`);
        lines.push(`Cash Tendered:${fmt(sale.amountPaid).padStart(36)}`);
        lines.push(`Change Returned:${fmt(sale.changeReturned).padStart(34)}`);
        lines.push('');
        lines.push(invoiceFooter);
        lines.push(`Ref: ${sale.billNumber}-${sale.saleId}`);
        return lines;
    },

    generatePdfBytes(lines) {
        const fontSize = 10;
        const lineHeight = 14;
        let y = 750;
        let stream = 'BT /F1 ' + fontSize + ' Tf\n';

        lines.forEach(line => {
            stream += `1 0 0 1 50 ${y} Tm (${this.escapePdf(line)}) Tj\n`;
            y -= lineHeight;
        });
        stream += 'ET';

        const streamLen = stream.length;
        const objects = [
            '1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n',
            '2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n',
            '3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n',
            `4 0 obj\n<< /Length ${streamLen} >>\nstream\n${stream}\nendstream\nendobj\n`,
            '5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n'
        ];

        let pdf = '%PDF-1.4\n';
        const offsets = [0];
        objects.forEach(obj => {
            offsets.push(pdf.length);
            pdf += obj;
        });

        const xrefStart = pdf.length;
        pdf += 'xref\n0 ' + (objects.length + 1) + '\n';
        pdf += '0000000000 65535 f \n';
        for (let i = 1; i <= objects.length; i++) {
            pdf += String(offsets[i]).padStart(10, '0') + ' 00000 n \n';
        }
        pdf += 'trailer\n<< /Size ' + (objects.length + 1) + ' /Root 1 0 R >>\n';
        pdf += 'startxref\n' + xrefStart + '\n%%EOF';
        return pdf;
    },

    pdfFilename(sale) {
        const bill = (sale && sale.billNumber) ? sale.billNumber.replace(/[^\w-]/g, '') : 'invoice';
        return `RxPro-Invoice-${bill}.pdf`;
    },

    toBase64(pdfString) {
        const bytes = new TextEncoder().encode(pdfString);
        let binary = '';
        bytes.forEach(b => { binary += String.fromCharCode(b); });
        return btoa(binary);
    },

    download(sale) {
        if (!sale) {
            App.toast('No receipt data available for PDF export.', 'error');
            return false;
        }

        try {
            const lines = this.buildLines(sale);
            const pdfString = this.generatePdfBytes(lines);
            const filename = this.pdfFilename(sale);

            if (window.javaBridge && typeof window.javaBridge.savePdf === 'function') {
                window.javaBridge.savePdf(this.toBase64(pdfString), filename);
                App.toast(`PDF saved: ${filename}`, 'success');
                return true;
            }

            const blob = new Blob([pdfString], { type: 'application/pdf' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
            App.toast(`PDF downloaded: ${filename}`, 'success');
            return true;
        } catch (e) {
            console.error('PDF generation failed', e);
            App.toast('Could not generate PDF. Try Print Invoice and choose "Save as PDF".', 'error');
            return false;
        }
    }
};
