package com.trustpass.audit;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
    Optional<AuditEventEntity> findTopByOrderByIdDesc();
    Page<AuditEventEntity> findAllByOrderByIdDesc(Pageable pageable);
    List<AuditEventEntity> findAllByOrderByIdAsc();
}
