package com.clearpath.clearpath_api.repository;

import com.clearpath.clearpath_api.entity.AiInvestigationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiInvestigationRecordRepository
        extends JpaRepository<AiInvestigationRecord, Long> {

    List<AiInvestigationRecord>
    findTop20ByOrderIdOrderByCreatedAtDesc(String orderId);
}