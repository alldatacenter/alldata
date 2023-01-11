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
package org.apache.ranger.authorization.solr.authorizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.plugin.audit.RangerMultiResourceAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.services.solr.RangerSolrConstants;

import java.util.Arrays;
import java.util.List;

public class RangerSolrAuditHandler extends RangerMultiResourceAuditHandler {
    private static final String PROP_SOLR_PLUGIN_AUDIT_EXCLUDED_USERS = "ranger.solr.plugin.audit.excluded.users";
    private static final String RANGER_AUDIT_COLLECTION               = "ranger_audits";

    private String          solrUser     = "solr";
    private List<String>    excludeUsers = null;
    private AuthzAuditEvent auditEvent   = null;


    public RangerSolrAuditHandler(Configuration config){
        String excludeUserList = config.get(PROP_SOLR_PLUGIN_AUDIT_EXCLUDED_USERS, solrUser);
        excludeUsers           = Arrays.asList(excludeUserList.split(","));
    }

    @Override
    public void processResult(RangerAccessResult result) {
        // We don't audit operation for user "solr" on collection "ranger_audits" to avoid recursive
        // logging due to updated of ranger_audits collection by solr plugin's audit creation.
        if (!isAuditingNeeded(result)) {
            return;
        }
         auditEvent = super.getAuthzEvents(result);
         super.logAuthzAudit(auditEvent);
    }

    private boolean isAuditingNeeded(final RangerAccessResult result) {
        boolean                  ret       = true;
        RangerAccessRequest      request   = result.getAccessRequest();
        RangerAccessResourceImpl resource  = (RangerAccessResourceImpl) request.getResource();
        String resourceName                = (String) resource.getValue(RangerSolrConstants.COLLECTION_KEY);
        String requestUser                 = request.getUser();
        if (resourceName != null && resourceName.equals(RANGER_AUDIT_COLLECTION) && excludeUsers.contains(requestUser)) {
           ret = false;
        }
        return ret;
    }
}
