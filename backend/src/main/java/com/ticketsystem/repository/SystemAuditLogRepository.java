package com.ticketsystem.repository;

import com.ticketsystem.model.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Feature 32 – System-Aktivitätsprotokoll */
@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {
    List<SystemAuditLog> findAllByOrderByTimestampDesc();
    List<SystemAuditLog> findByEventTypeOrderByTimestampDesc(String eventType);
    List<SystemAuditLog> findByActorOrderByTimestampDesc(String actor);
}
