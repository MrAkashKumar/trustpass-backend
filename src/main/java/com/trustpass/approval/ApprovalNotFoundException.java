package com.trustpass.approval;

import java.util.UUID;

public class ApprovalNotFoundException extends RuntimeException {
    public ApprovalNotFoundException(UUID id) {
        super("Approval request not found: " + id);
    }
}

