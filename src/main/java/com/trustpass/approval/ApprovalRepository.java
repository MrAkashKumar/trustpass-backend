package com.trustpass.approval;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    Page<ApprovalRequestEntity> findAllByOrderByRequestedAtDesc(Pageable pageable);
    Page<ApprovalRequestEntity> findByStatusOrderByRequestedAtDesc(ApprovalStatus status, Pageable pageable);
    Optional<ApprovalRequestEntity> findByExternalRequestId(String externalRequestId);
    Optional<ApprovalRequestEntity> findByIdempotencyKey(String idempotencyKey);
    long countByStatus(ApprovalStatus status);
}
