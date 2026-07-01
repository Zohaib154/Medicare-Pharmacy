package com.medicare.service;

import com.medicare.entity.Inventory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockDeduction {
    private final Inventory batch;
    private final int quantity;
}


