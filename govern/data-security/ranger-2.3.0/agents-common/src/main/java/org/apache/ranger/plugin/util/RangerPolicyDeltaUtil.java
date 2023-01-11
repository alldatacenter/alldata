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
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class RangerPolicyDeltaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RangerPolicyDeltaUtil.class);

    private static final Logger PERF_POLICY_DELTA_LOG = RangerPerfTracer.getPerfLogger("policy.delta");

    public static List<RangerPolicy> applyDeltas(List<RangerPolicy> policies, List<RangerPolicyDelta> deltas, String serviceType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> applyDeltas(serviceType=" + serviceType + ")");
        }

        List<RangerPolicy> ret;

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_DELTA_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_DELTA_LOG, "RangerPolicyDelta.applyDeltas()");
        }

        boolean hasExpectedServiceType = false;

        if (CollectionUtils.isNotEmpty(deltas)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("applyDeltas(deltas=" + Arrays.toString(deltas.toArray()) + ", serviceType=" + serviceType + ")");
            }

            for (RangerPolicyDelta delta : deltas) {
                if (serviceType.equals(delta.getServiceType())) {
                    hasExpectedServiceType = true;
                    break;
                } else if (!serviceType.equals(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME) && !delta.getServiceType().equals(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
                    LOG.warn("Found unexpected serviceType in policyDelta:[" + delta + "]. Was expecting serviceType:[" + serviceType + "]. Should NOT have come here!! Ignoring delta and continuing");
                }
            }

            if (hasExpectedServiceType) {
                ret = new ArrayList<>(policies);

                for (RangerPolicyDelta delta : deltas) {
                    if (!serviceType.equals(delta.getServiceType())) {
                        continue;
                    }

                    int changeType = delta.getChangeType();

                    if (changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE || changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE || changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE) {
                        Long policyId = delta.getPolicyId();

                        if (policyId == null) {
                            continue;
                        }

                        List<RangerPolicy>     deletedPolicies = new ArrayList<>();

                        Iterator<RangerPolicy> iter = ret.iterator();

                        while (iter.hasNext()) {
                            RangerPolicy policy = iter.next();
                            if (policyId.equals(policy.getId()) && (changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE || changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE)) {
                                deletedPolicies.add(policy);
                                iter.remove();
                            }
                        }

                        switch(changeType) {
                            case RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE: {
                                if (CollectionUtils.isNotEmpty(deletedPolicies)) {
                                    LOG.warn("Unexpected: found existing policy for CHANGE_TYPE_POLICY_CREATE: " + Arrays.toString(deletedPolicies.toArray()));
                                }
                                break;
                            }
                            case RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE: {
                                if (CollectionUtils.isEmpty(deletedPolicies) || deletedPolicies.size() > 1) {
                                    LOG.warn("Unexpected: found no policy or multiple policies for CHANGE_TYPE_POLICY_UPDATE: " + Arrays.toString(deletedPolicies.toArray()));
                                }
                                break;
                            }
                            case RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE: {
                                if (CollectionUtils.isEmpty(deletedPolicies) || deletedPolicies.size() > 1) {
                                    LOG.warn("Unexpected: found no policy or multiple policies for CHANGE_TYPE_POLICY_DELETE: " + Arrays.toString(deletedPolicies.toArray()));
                                }
                                break;
                            }
                            default:
                                break;
                        }

                        if (changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE) {
                            ret.add(delta.getPolicy());
                        }
                    } else {
                        LOG.warn("Found unexpected changeType in policyDelta:[" + delta + "]. Ignoring delta");
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("applyDeltas - none of the deltas is for " + serviceType + ")");
                }
                ret = policies;
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("applyDeltas called with empty deltas. Will return policies without change");
            }
            ret = policies;
        }

        if (CollectionUtils.isNotEmpty(deltas) && hasExpectedServiceType && CollectionUtils.isNotEmpty(ret)) {
            ret.sort(RangerPolicy.POLICY_ID_COMPARATOR);
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
