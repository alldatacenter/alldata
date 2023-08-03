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

package org.apache.ranger.plugin.service;

import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerResourceACLs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public abstract class RangerChainedPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(RangerChainedPlugin.class);

    protected final RangerBasePlugin rootPlugin;
    protected final String           serviceType;
    protected final String           serviceName;
    protected final RangerBasePlugin plugin;

    protected RangerChainedPlugin(RangerBasePlugin rootPlugin, String serviceType, String serviceName) {
        LOG.info("RangerChainedPlugin(" + serviceType + ", " + serviceName + ")");

        this.rootPlugin  = rootPlugin;
        this.serviceType = serviceType;
        this.serviceName = serviceName;
        this.plugin      = buildChainedPlugin(serviceType, serviceName, rootPlugin.getAppId());
    }

    public void init() {
        LOG.info("==> RangerChainedPlugin.init(" + serviceType + ", " + serviceName + ")");

        this.plugin.init();

        LOG.info("<== RangerChainedPlugin.init(" + serviceType + ", " + serviceName + ")");
    }

    protected RangerBasePlugin buildChainedPlugin(String serviceType, String serviceName, String appId) {
        return new RangerBasePlugin(serviceType, serviceName, appId);
    }

    public abstract RangerAccessResult isAccessAllowed(RangerAccessRequest request);

    public abstract Collection<RangerAccessResult> isAccessAllowed(Collection<RangerAccessRequest> requests);

    public abstract RangerResourceACLs getResourceACLs(RangerAccessRequest request);

    public abstract RangerResourceACLs getResourceACLs(RangerAccessRequest request, Integer policyType);

    public boolean  isAuthorizeOnlyWithChainedPlugin() { return false; }

    public RangerAccessResult evalDataMaskPolicies(RangerAccessRequest request) {
        return null; // no-op
    }

    public RangerAccessResult evalRowFilterPolicies(RangerAccessRequest request) {
        return null; // no-op
    }
}
