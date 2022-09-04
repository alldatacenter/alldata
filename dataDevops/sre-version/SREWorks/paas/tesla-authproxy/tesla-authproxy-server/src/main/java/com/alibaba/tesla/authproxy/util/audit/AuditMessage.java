package com.alibaba.tesla.authproxy.util.audit;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AuditMessage {
    private String action;
    private String user;
    private String target;
    private String outcome;
    private String reason;
    private String message;
}