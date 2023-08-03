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

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RangerPolicyDeltaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RangerPolicyDeltaUtil.class);

    private static final Logger PERF_POLICY_DELTA_LOG = RangerPerfTracer.getPerfLogger("policy.delta");

    public static List<RangerPolicy> applyDeltas(List<RangerPolicy> policies, List<RangerPolicyDelta> deltas, String serviceType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> applyDeltas(serviceType=" + serviceType + ")");
        }

        List<RangerPolicy> ret;
        RangerPerfTracer   perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_DELTA_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_DELTA_LOG, "RangerPolicyDelta.applyDeltas()");
        }

        if (CollectionUtils.isNotEmpty(deltas)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("applyDeltas(deltas=" + Arrays.toString(deltas.toArray()) + ", serviceType=" + serviceType + ")");
            }

            Map<Long, RangerPolicy> retMap = new HashMap<>();

            for (RangerPolicy policy : policies) {
                retMap.put(policy.getId(), policy);
            }

            for (RangerPolicyDelta delta : deltas) {
                if (!StringUtils.equals(serviceType, delta.getServiceType()) || delta.getPolicyId() == null) {
                    continue;
                }

                int changeType = delta.getChangeType();

                if (changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE && changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE && changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE) {
                    LOG.warn("Found unexpected changeType in policyDelta:[" + delta + "]. Ignoring delta");

                    continue;
                }

                Long         policyId      = delta.getPolicyId();
                RangerPolicy deletedPolicy = retMap.remove(policyId);

                switch(changeType) {
                    case RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE: {
                        if (deletedPolicy != null) {
                            LOG.warn("Unexpected: found existing policy for CHANGE_TYPE_POLICY_CREATE: " + deletedPolicy);
                        }
                        break;
                    }
                    case RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE: {
                        if (deletedPolicy == null) {
                            LOG.warn("Unexpected: found no existing policy for CHANGE_TYPE_POLICY_UPDATE: " + deletedPolicy);
                        }
                        break;
                    }
                    case RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE: {
                        if (deletedPolicy == null) {
                            LOG.warn("Unexpected: found no existing policy for CHANGE_TYPE_POLICY_DELETE: " + deletedPolicy);
                        }
                        break;
                    }
                    default:
                        break;
                }

                if (changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE) {
                    retMap.put(policyId, delta.getPolicy());
                }
            }

            ret = new ArrayList<>(retMap.values());
            ret.sort(RangerPolicy.POLICY_ID_COMPARATOR);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("applyDeltas called with empty deltas. Will return policies without change");
            }
            ret = policies;
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== applyDeltas(serviceType=" + serviceType + "): " + ret);
        }
        return ret;
    }

    public static boolean isValidDeltas(List<RangerPolicyDelta> deltas, String componentServiceType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isValidDeltas(deltas=" + Arrays.toString(deltas.toArray()) + ", componentServiceType=" + componentServiceType +")");
        }
        boolean isValid = true;

        for (RangerPolicyDelta delta : deltas) {
            final Integer changeType = delta.getChangeType();
            final Long    policyId   = delta.getPolicyId();

            if (changeType == null) {
                isValid = false;
                break;
            }

            if (changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE
                    && changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE
                    && changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE) {
                isValid = false;
            } else if (policyId == null) {
                isValid = false;
            } else {
                final String  serviceType = delta.getServiceType();
                final Integer policyType  = delta.getPolicyType();

                if (serviceType == null || (!serviceType.equals(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME) &&
                        !serviceType.equals(componentServiceType))) {
                    isValid = false;
                } else if (policyType == null || (policyType != RangerPolicy.POLICY_TYPE_ACCESS
                        && policyType != RangerPolicy.POLICY_TYPE_DATAMASK
                        && policyType != RangerPolicy.POLICY_TYPE_ROWFILTER)) {
                    isValid = false;
                }
            }

            if (!isValid) {
                break;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isValidDeltas(deltas=" + Arrays.toString(deltas.toArray()) + ", componentServiceType=" + componentServiceType +"): " + isValid);
        }
        return isValid;
    }

    public static Boolean hasPolicyDeltas(final ServicePolicies servicePolicies) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> hasPolicyDeltas(servicePolicies:[" + servicePolicies + "]");
        }
        final Boolean ret;

        if (servicePolicies == null) {
            LOG.error("ServicePolicies are null!");
            ret = null;
        } else {
            boolean isPoliciesExistInSecurityZones     = false;
            boolean isPolicyDeltasExistInSecurityZones = false;

            if (MapUtils.isNotEmpty(servicePolicies.getSecurityZones())) {
                for (ServicePolicies.SecurityZoneInfo element : servicePolicies.getSecurityZones().values()) {
                    if (CollectionUtils.isNotEmpty(element.getPolicies()) && CollectionUtils.isEmpty(element.getPolicyDeltas())) {
                        isPoliciesExistInSecurityZones = true;
                    }
                    if (CollectionUtils.isEmpty(element.getPolicies()) && CollectionUtils.isNotEmpty(element.getPolicyDeltas())) {
                        isPolicyDeltasExistInSecurityZones = true;
                    }
                }
            }

            boolean isPoliciesExist     = CollectionUtils.isNotEmpty(servicePolicies.getPolicies()) || (servicePolicies.getTagPolicies() != null && CollectionUtils.isNotEmpty(servicePolicies.getTagPolicies().getPolicies())) || isPoliciesExistInSecurityZones;
            boolean isPolicyDeltasExist = CollectionUtils.isNotEmpty(servicePolicies.getPolicyDeltas()) || isPolicyDeltasExistInSecurityZones;

            if (isPoliciesExist && isPolicyDeltasExist) {
                LOG.warn("ServicePolicies contain both policies and policy-deltas!! Cannot build policy-engine from these servicePolicies. Please check server-side code!");
                LOG.warn("Downloaded ServicePolicies are [" + servicePolicies + "]");
                ret = null;
            } else if (!isPoliciesExist && !isPolicyDeltasExist) {
                LOG.warn("ServicePolicies do not contain any policies or policy-deltas!!");
                LOG.warn("Downloaded ServicePolicies are [" + servicePolicies + "]");
                if (servicePolicies.getPolicyDeltas() == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Complete set of servicePolicies is received. There may be a change to service. Forcing to create a new policy engine!");
                    }
                    ret = false;    // Force new policy engine creation from servicePolicies
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("servicePolicy deltas are received. There are no material changes in the policies.");
                    }
                    ret = null;
                }
            } else {
                ret = isPolicyDeltasExist;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== hasPolicyDeltas(servicePolicies:[" + servicePolicies + "], ret:[" + ret + "]");
        }
        return ret;
    }
}
