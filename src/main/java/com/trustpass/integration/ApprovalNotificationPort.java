package com.trustpass.integration;

import com.trustpass.approval.ApprovalRequestEntity;
import java.util.Optional;

public interface ApprovalNotificationPort {
    Optional<String> notifyApprover(ApprovalRequestEntity approval, String approverPhone);
}

