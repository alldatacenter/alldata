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

package org.apache.ranger.examples.sampleapp;

import java.util.Set;

import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;

public class RangerAuthorizer implements IAuthorizer {
    private static volatile RangerBasePlugin plugin = null;

    public RangerAuthorizer() {

    }

    public void init() {
        if(plugin == null) {
            synchronized (RangerAuthorizer.class) {
                if(plugin == null) {
                    plugin = new RangerBasePlugin("sampleapp", "sampleapp");

                    plugin.setResultProcessor(new RangerDefaultAuditHandler(plugin.getConfig()));

                    plugin.init();
                }
            }
        }
    }

    public boolean authorize(String fileName, String accessType, String user, Set<String> userGroups) {
        RangerAccessResourceImpl resource = new RangerAccessResourceImpl();
        resource.setValue("path", fileName); // "path" must be a value resource name in servicedef JSON

        RangerAccessRequest request = new RangerAccessRequestImpl(resource, accessType, user, userGroups, null);

        RangerAccessResult result = plugin.isAccessAllowed(request);

        return result != null && result.getIsAllowed();
    }
}
