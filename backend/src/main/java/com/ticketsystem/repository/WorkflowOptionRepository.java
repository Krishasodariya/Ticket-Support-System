package com.ticketsystem.repository;

import com.ticketsystem.model.WorkflowOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowOptionRepository extends JpaRepository<WorkflowOption, Long> {
    List<WorkflowOption> findByTypeOrderBySortOrderAscNameAsc(String type);
    List<WorkflowOption> findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(String type);
    boolean existsByTypeIgnoreCaseAndNameIgnoreCase(String type, String name);
}
