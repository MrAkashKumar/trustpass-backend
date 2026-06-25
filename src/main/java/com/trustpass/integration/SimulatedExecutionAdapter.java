package com.trustpass.integration;

import com.trustpass.approval.ApprovalRequestEntity;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedExecutionAdapter implements ActionExecutionPort {
    private static final Logger log = LoggerFactory.getLogger(SimulatedExecutionAdapter.class);

    @Override
    public String execute(ApprovalRequestEntity approval) {
        log.info("Simulated governed action execution for {}", approval.getReference());
        return "exec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

