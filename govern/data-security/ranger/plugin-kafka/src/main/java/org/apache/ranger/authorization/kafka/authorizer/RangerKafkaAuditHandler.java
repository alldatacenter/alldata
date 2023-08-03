/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.ranger.authorization.kafka.authorizer;

import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerKafkaAuditHandler extends RangerDefaultAuditHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RangerKafkaAuditHandler.class);

    private AuthzAuditEvent auditEvent      = null;

    public RangerKafkaAuditHandler(){
    }

    @Override
    public void processResult(RangerAccessResult result) {
        // If Cluster Resource Level Topic Creation is not Allowed we don't audit.
        // Subsequent call from Kafka for Topic Creation at Topic resource Level will be audited.
        if (!isAuditingNeeded(result)) {
            return;
        }
        auditEvent = super.getAuthzEvents(result);
    }

    private boolean isAuditingNeeded(final RangerAccessResult result) {
        boolean ret = true;
        boolean 			    isAllowed = result.getIsAllowed();
        RangerAccessRequest request = result.getAccessRequest();
        RangerAccessResourceImpl resource = (RangerAccessResourceImpl) request.getResource();
        String resourceName 			  = (String) resource.getValue(RangerKafkaAuthorizer.KEY_CLUSTER);
        if (resourceName != null) {
            if (request.getAccessType().equalsIgnoreCase(RangerKafkaAuthorizer.ACCESS_TYPE_CREATE) && !isAllowed) {
                ret = false;
            }
        }
        return ret;
    }

    public void flushAudit() {
        if(LOG.isDebugEnabled()) {
            LOG.info("==> RangerYarnAuditHandler.flushAudit(" + "AuditEvent: " + auditEvent + ")");
        }
        if (auditEvent != null) {
            super.logAuthzAudit(auditEvent);
        }
        if(LOG.isDebugEnabled()) {
            LOG.info("<== RangerYarnAuditHandler.flushAudit(" + "AuditEvent: " + auditEvent + ")");
        }
    }
}
