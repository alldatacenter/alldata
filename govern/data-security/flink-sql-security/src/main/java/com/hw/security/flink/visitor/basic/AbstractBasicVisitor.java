package com.hw.security.flink.visitor.basic;

import com.hw.security.flink.PolicyManager;
import com.hw.security.flink.SecurityContext;
import org.apache.calcite.sql.util.SqlBasicVisitor;

/**
 * @description: AbstractBasicVisitor
 * @author: HamaWhite
 */
public abstract class AbstractBasicVisitor extends SqlBasicVisitor<Void> {

    protected final SecurityContext securityContext;

    protected final PolicyManager policyManager;

    protected final String username;

    protected AbstractBasicVisitor(SecurityContext securityContext, String username) {
        this.securityContext = securityContext;
        this.policyManager = securityContext.getPolicyManager();
        this.username = username;
    }
}
