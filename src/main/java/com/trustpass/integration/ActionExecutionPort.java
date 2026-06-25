package com.trustpass.integration;

import com.trustpass.approval.ApprovalRequestEntity;

public interface ActionExecutionPort {
    String execute(ApprovalRequestEntity approval);
}

