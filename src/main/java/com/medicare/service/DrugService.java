package com.medicare.service;

import com.medicare.dto.DrugDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DrugService {
    DrugDTO createDrug(DrugDTO dto);
    DrugDTO updateDrug(Long id, DrugDTO dto);
    DrugDTO getDrugById(Long id);
    Page<DrugDTO> getAllDrugs(Pageable pageable);
    Page<DrugDTO> getAllDrugsIncludingInactive(Pageable pageable);
    Page<DrugDTO> searchDrugs(String query, Pageable pageable);
    Page<DrugDTO> getDrugsByCategory(String category, Pageable pageable);
    void deleteDrug(Long id);
    List<String> getAllCategories();

    List<DrugDTO> getDrugsWithStock();
}


