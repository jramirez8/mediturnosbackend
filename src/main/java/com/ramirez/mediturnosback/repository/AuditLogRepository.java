package com.ramirez.mediturnosback.repository;

import com.ramirez.mediturnosback.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreadoEnDesc();
}
