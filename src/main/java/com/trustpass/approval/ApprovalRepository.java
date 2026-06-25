package com.trustpass.approval;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    Page<ApprovalRequestEntity> findAllByOrderByRequestedAtDesc(Pageable pageable);
    Page<ApprovalRequestEntity> findByStatusOrderByRequestedAtDesc(ApprovalStatus status, Pageable pageable);
    long countByStatus(ApprovalStatus status);
}

