package com.medicare.service;

import com.medicare.repository.PrescriptionRepository;
import com.medicare.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private static final int BILL_START = 20000;
    private static final int RX_START = 10000;

    private final SaleRepository saleRepository;
    private final PrescriptionRepository prescriptionRepository;

    public String nextBillNumber() {
        int max = saleRepository.findMaxBillSuffix().orElse(BILL_START);
        return "B-" + (max + 1);
    }

    public String nextRxNumber() {
        int max = prescriptionRepository.findMaxRxSuffix().orElse(RX_START);
        return "RX-" + (max + 1);
    }
}


