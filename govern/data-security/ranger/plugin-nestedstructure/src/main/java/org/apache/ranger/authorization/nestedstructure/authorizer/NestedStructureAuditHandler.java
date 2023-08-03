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

package org.apache.ranger.authorization.nestedstructure.authorizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NestedStructureAuditHandler extends RangerDefaultAuditHandler {
    public static final String  ACCESS_TYPE_ROWFILTER = "ROW_FILTER";

    List<AuthzAuditEvent> auditEvents  = null;
    boolean               deniedExists = false;

    public NestedStructureAuditHandler(Configuration config) {
        super(config);
    }

    @Override
    public void processResult(RangerAccessResult result) {
        if (result.getIsAudited()) {
            AuthzAuditEvent auditEvent = createAuditEvent(result);

            if (auditEvent != null) {
                if (auditEvents == null) {
                    auditEvents = new ArrayList<>();
                }

                auditEvents.add(auditEvent);

                if (auditEvent.getAccessResult() == 0) {
                    deniedExists = true;
                }
            }
        }
    }

    @Override
    public void processResults(Collection<RangerAccessResult> results) {
        for (RangerAccessResult result : results) {
            processResult(result);
        }
    }

    public void flushAudit() {
        if (auditEvents != null) {
            for (AuthzAuditEvent auditEvent : auditEvents) {
                if (deniedExists && auditEvent.getAccessResult() != 0) { // if deny exists, skip logging for allowed results
                    continue;
                }

                super.logAuthzAudit(auditEvent);
            }
        }
    }

    private AuthzAuditEvent createAuditEvent(RangerAccessResult result) {
        AuthzAuditEvent ret = super.getAuthzEvents(result);

        if (ret != null) {
            int policyType = result.getPolicyType();

            if (policyType == RangerPolicy.POLICY_TYPE_DATAMASK && result.isMaskEnabled()) {
                ret.setAccessType(result.getMaskType());
            } else if (policyType == RangerPolicy.POLICY_TYPE_ROWFILTER) {
                ret.setAccessType(ACCESS_TYPE_ROWFILTER);
            }
        }

        return ret;
    }
}
