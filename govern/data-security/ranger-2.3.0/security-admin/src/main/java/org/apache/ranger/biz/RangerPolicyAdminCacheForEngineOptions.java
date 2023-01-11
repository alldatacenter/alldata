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

package org.apache.ranger.biz;

import org.apache.ranger.plugin.store.RoleStore;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.store.SecurityZoneStore;
import org.apache.ranger.plugin.store.ServiceStore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RangerPolicyAdminCacheForEngineOptions {
    private static volatile RangerPolicyAdminCacheForEngineOptions sInstance = null;

    private final Map<RangerPolicyEngineOptions, RangerPolicyAdminCache> policyAdminCacheForEngineOptions = Collections.synchronizedMap(new HashMap<>());

    public static RangerPolicyAdminCacheForEngineOptions getInstance() {
        RangerPolicyAdminCacheForEngineOptions ret = sInstance;

        if (ret == null) {
            synchronized (RangerPolicyAdminCacheForEngineOptions.class) {
                ret = sInstance;

                if (ret == null) {
                    sInstance = new RangerPolicyAdminCacheForEngineOptions();
                    ret       = sInstance;
                }
            }
        }

        return ret;
    }

    public final RangerPolicyAdmin getServicePoliciesAdmin(String serviceName, ServiceStore svcStore, SecurityZoneStore zoneStore, RoleStore roleStore, RangerPolicyEngineOptions options) {
        return getServicePoliciesAdmin(serviceName, svcStore, roleStore, zoneStore, options);
    }

    private RangerPolicyAdmin getServicePoliciesAdmin(String serviceName, ServiceStore svcStore, RoleStore roleStore, SecurityZoneStore zoneStore, RangerPolicyEngineOptions options) {

        RangerPolicyAdminCache policyAdminCache = policyAdminCacheForEngineOptions.get(options);

        if (policyAdminCache == null) {
            synchronized (this) {
                policyAdminCache = policyAdminCacheForEngineOptions.get(options);

                if (policyAdminCache == null) {
                    policyAdminCache = new RangerPolicyAdminCache();

                    policyAdminCacheForEngineOptions.put(options, policyAdminCache);
                }
            }
        }

        return policyAdminCache.getServicePoliciesAdmin(serviceName, svcStore, roleStore, zoneStore, options);
    }
}

