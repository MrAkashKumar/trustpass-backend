package com.trustpass.integration;

import com.trustpass.approval.ApprovalRequestEntity;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "trustpass.integrations.elevenlabs", name = "enabled", havingValue = "false", matchIfMissing = true)
public class SimulatedNotificationAdapter implements ApprovalNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(SimulatedNotificationAdapter.class);

    @Override
    public Optional<String> notifyApprover(ApprovalRequestEntity approval, String approverPhone) {
        log.info("Local approval notification created for {} (phone persisted: false)", approval.getReference());
        return Optional.of("local-notification-" + approval.getReference().toLowerCase());
    }
}

