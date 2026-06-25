package com.trustpass.policy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<PolicyEntity, UUID> {
    Optional<PolicyEntity> findFirstByActionTypeAndActiveTrueOrderByUpdatedAtDesc(ActionType actionType);
}

