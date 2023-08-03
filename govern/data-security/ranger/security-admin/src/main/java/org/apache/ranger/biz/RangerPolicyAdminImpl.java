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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicyResourceSignature;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.PolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestProcessor;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerPluginContext;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerPolicyRepository;
import org.apache.ranger.plugin.policyengine.RangerTagAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerTagResource;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator.RangerPolicyResourceEvaluator;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.resourcematcher.RangerAbstractResourceMatcher;
import org.apache.ranger.plugin.service.RangerDefaultRequestProcessor;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.util.GrantRevokeRequest;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.RangerReadWriteLock;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.ServiceDefUtil;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.plugin.util.StringTokenReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RangerPolicyAdminImpl implements RangerPolicyAdmin {
    private static final Logger LOG = LoggerFactory.getLogger(RangerPolicyAdminImpl.class);

    private static final Logger PERF_POLICYENGINE_REQUEST_LOG = RangerPerfTracer.getPerfLogger("policyengine.request");

    private final PolicyEngine                 policyEngine;
    private final RangerAccessRequestProcessor requestProcessor;
    private final static Map<String, Object>          wildcardEvalContext = new HashMap<String, Object>() {
        @Override
        public Object get(Object key) { return RangerAbstractResourceMatcher.WILDCARD_ASTERISK; }
    };
    private       ServiceDBStore               serviceDBStore;

    static {
        wildcardEvalContext.put(RangerAbstractResourceMatcher.WILDCARD_ASTERISK, RangerAbstractResourceMatcher.WILDCARD_ASTERISK);
    }

    static public RangerPolicyAdmin getPolicyAdmin(final RangerPolicyAdminImpl other, final ServicePolicies servicePolicies) {
        RangerPolicyAdmin ret = null;

        if (other != null && servicePolicies != null) {
            PolicyEngine policyEngine = other.policyEngine.cloneWithDelta(servicePolicies);

            if (policyEngine != null) {
                if (policyEngine == other.policyEngine) {
                    ret = other;
                } else {
                    ret = new RangerPolicyAdminImpl(policyEngine);
                }
            }
        }

        return ret;
    }

    RangerPolicyAdminImpl(ServicePolicies servicePolicies, RangerPluginContext pluginContext, RangerRoles roles) {
        this.policyEngine     = new PolicyEngine(servicePolicies, pluginContext, roles, ServiceDBStore.SUPPORTS_IN_PLACE_POLICY_UPDATES);
        this.requestProcessor = new RangerDefaultRequestProcessor(policyEngine);
    }

    private RangerPolicyAdminImpl(final PolicyEngine policyEngine) {
        this.policyEngine     = policyEngine;
        this.requestProcessor = new RangerDefaultRequestProcessor(policyEngine);
    }

    @Override
    public void setServiceStore(ServiceStore svcStore) {
        if (svcStore instanceof ServiceDBStore) {
            this.serviceDBStore = (ServiceDBStore) svcStore;
        }
    }

    @Override
    public boolean isDelegatedAdminAccessAllowed(RangerAccessResource resource, String zoneName, String user, Set<String> userGroups, Set<String> accessTypes) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.isDelegatedAdminAccessAllowed(" + resource + ", " + zoneName + ", " + user + ", " + userGroups + ", " + accessTypes + ")");
        }

        boolean          ret  = false;
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYENGINE_REQUEST_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICYENGINE_REQUEST_LOG, "RangerPolicyAdminImpl.isDelegatedAdminAccessAllowed(user=" + user + ",accessTypes=" + accessTypes + "resource=" + resource.getAsString() + ")");
        }

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {

            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }

            final RangerPolicyRepository matchedRepository = policyEngine.getRepositoryForZone(zoneName);

            if (matchedRepository != null) {
                Set<String> roles = getRolesFromUserAndGroups(user, userGroups);
                Set<String> requestedAccesses = new HashSet<>(accessTypes);

                RangerAccessRequestImpl request = new RangerAccessRequestImpl();
                request.setResource(resource);

                for (RangerPolicyEvaluator evaluator : matchedRepository.getLikelyMatchPolicyEvaluators(request, RangerPolicy.POLICY_TYPE_ACCESS)) {

                    Set<String> allowedAccesses = evaluator.getAllowedAccesses(resource, user, userGroups, roles, requestedAccesses);
                    if (CollectionUtils.isNotEmpty(allowedAccesses)) {
                        requestedAccesses.removeAll(allowedAccesses);
                        if (CollectionUtils.isEmpty(requestedAccesses)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Access granted by policy:[" + evaluator.getPolicy() + "]");
                            }
                            ret = true;
                            break;
                        }
                    }
                }

            }

        }
        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.isDelegatedAdminAccessAllowed(" + resource + ", " + zoneName + ", " + user + ", " + userGroups + ", " + accessTypes + "): " + ret);
        }

        return ret;
    }

    @Override
    public boolean isDelegatedAdminAccessAllowedForRead(RangerPolicy policy, String user, Set<String> userGroups, Set<String> roles, Map<String, Object> evalContext) {
        return isDelegatedAdminAccessAllowed(policy, user, userGroups, roles, true, evalContext);
    }

    @Override
    public boolean isDelegatedAdminAccessAllowedForModify(RangerPolicy policy, String user, Set<String> userGroups, Set<String> roles, Map<String, Object> evalContext) {
        return isDelegatedAdminAccessAllowed(policy, user, userGroups, roles, false, evalContext);
    }

    boolean isDelegatedAdminAccessAllowed(RangerPolicy policy, String user, Set<String> userGroups, Set<String> roles, boolean isRead, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.isDelegatedAdminAccessAllowed(" + policy.getId() + ", " + user + ", " + userGroups + ", " + roles + ", " + isRead + ", " + evalContext + ")");
        }

        boolean          ret  = false;
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYENGINE_REQUEST_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICYENGINE_REQUEST_LOG, "RangerPolicyEngine.isDelegatedAdminAccessAllowed(user=" + user + "," + userGroups + ", roles=" + roles + ")");
        }

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {

            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }

            final RangerPolicyRepository matchedRepository = policyEngine.getRepositoryForMatchedZone(policy);

            if (matchedRepository != null) {
                if (isRead) {
                    Set<String> accessTypes = getAllAccessTypes(policy, getServiceDef());
                    ret = isDelegatedAdminAccessAllowedForPolicy(matchedRepository, policy, user, userGroups, roles, accessTypes, true, evalContext);
                } else {
                    // Get old policy from policy-engine
                    RangerPolicy oldPolicy = null;
                    if (policy.getId() != null) {
                        try {
                            oldPolicy = serviceDBStore.getPolicy(policy.getId());
                        } catch (Exception e) {
                            LOG.error("Cannot get old policy from DB: policy-id:[" + policy.getId() + "]");
                        }
                    }

                    if (oldPolicy != null) {
                        String oldResourceSignature = getResourceSignature(oldPolicy);
                        String newResourceSignature = getResourceSignature(policy);

                        if (StringUtils.equals(oldResourceSignature, newResourceSignature)) {
                            Set<String> modifiedAccessTypes = getAllModifiedAccessTypes(oldPolicy, policy, getServiceDef());
                            ret = isDelegatedAdminAccessAllowedForPolicy(matchedRepository, policy, user, userGroups, roles, modifiedAccessTypes, false, evalContext);
                        } else {
                            Set<String> removedAccessTypes = getAllAccessTypes(oldPolicy, getServiceDef());
                            // Ensure that current policy-engine (without current policy) allows old-policy to be modified
                            final boolean isOldPolicyChangeAllowed = isDelegatedAdminAccessAllowedForPolicy(matchedRepository, oldPolicy, user, userGroups, roles, removedAccessTypes, false, evalContext);
                            if (isOldPolicyChangeAllowed) {
                                Set<String> addedAccessTypes = getAllAccessTypes(policy, getServiceDef());
                                ret = isDelegatedAdminAccessAllowedForPolicy(matchedRepository, policy, user, userGroups, roles, addedAccessTypes, false, evalContext);
                            }
                        }
                    } else {
                        LOG.warn("Cannot get unmodified policy with id:[" + policy.getId() + "]. Checking if thi");
                        Set<String> addedAccessTypes = getAllAccessTypes(policy, getServiceDef());
                        ret = isDelegatedAdminAccessAllowedForPolicy(matchedRepository, policy, user, userGroups, roles, addedAccessTypes, false, evalContext);
                    }
                }
            }
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.isDelegatedAdminAccessAllowed(" + policy.getId() + ", " + user + ", " + userGroups + ", " + roles + ", " + isRead + ", " + evalContext + "): " + ret);
        }

        return ret;
    }

    private boolean isDelegatedAdminAccessAllowedForPolicy(RangerPolicyRepository matchedRepository, RangerPolicy policy, String user, Set<String> userGroups, Set<String> roles, Set<String> accessTypes, boolean isRead, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.isDelegatedAdminAccessAllowedForPolicy(" + policy.getId() + ", " + user + ", " + userGroups + ", " + roles + ", accessTypes" + accessTypes + ", " + isRead + ", " + evalContext + ")");
        }

        boolean ret = false;

        if (CollectionUtils.isEmpty(accessTypes)) {
            LOG.error("Could not get access-types for policy-id:[" + policy.getId() + "]");
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking delegate-admin access for the access-types:[" + accessTypes + "]");
            }

            Set<String> allowedAccesses = getAllowedAccesses(matchedRepository, policy.getResources(), user, userGroups, roles, accessTypes, evalContext);

            if (CollectionUtils.isEmpty(allowedAccesses)) {
                ret = false;
            } else {
                ret = isRead ? CollectionUtils.containsAny(allowedAccesses, accessTypes) : allowedAccesses.containsAll(accessTypes);
            }

            if (ret && CollectionUtils.isNotEmpty(policy.getAdditionalResources())) {
                for (Map<String, RangerPolicyResource> additionalResource : policy.getAdditionalResources()) {
                    Set<String> additionalResourceAllowedActions = getAllowedAccesses(matchedRepository, additionalResource, user, userGroups, roles, accessTypes, evalContext);

                    if (CollectionUtils.isEmpty(additionalResourceAllowedActions)) {
                        allowedAccesses.clear();

                        ret = false;
                    } else {
                        allowedAccesses.retainAll(additionalResourceAllowedActions); // allowedAccesses to contain only access-types that are allowed on all resources in the policy

                        if (isRead) {
                            ret = !allowedAccesses.isEmpty();
                        } else {
                            ret = additionalResourceAllowedActions.containsAll(accessTypes);
                        }
                    }

                    if (!ret) {
                        break;
                    }
                }
            }

            if (!ret) {
                Collection<String> unauthorizedAccesses = CollectionUtils.isEmpty(allowedAccesses) ? accessTypes : CollectionUtils.subtract(accessTypes, allowedAccesses);

                LOG.info("Accesses : " + unauthorizedAccesses + " are not authorized for the policy:[" + policy.getId() + "] by any of delegated-admin policies");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.isDelegatedAdminAccessAllowedForPolicy(" + policy.getId() + ", " + user + ", " + userGroups + ", " + roles + ", accessTypes" + accessTypes + ", " + isRead + ", " + evalContext + "): " + ret);
        }

        return ret;
    }

    private Set<String> getAllowedAccesses(RangerPolicyRepository matchedRepository, Map<String, RangerPolicyResource> resource, String user, Set<String> userGroups, Set<String> roles, Set<String> accessTypes, Map<String, Object> evalContext) {
        // RANGER-3082
        // Convert policy resources to by substituting macros with ASTERISK
        Map<String, RangerPolicyResource> modifiedResource = getPolicyResourcesWithMacrosReplaced(resource, wildcardEvalContext);
        Set<String>                       ret              = null;

        for (RangerPolicyEvaluator evaluator : matchedRepository.getPolicyEvaluators()) {
            Set<String> allowedAccesses = evaluator.getAllowedAccesses(modifiedResource, user, userGroups, roles, accessTypes, evalContext);

            if (CollectionUtils.isNotEmpty(allowedAccesses)) {
                if (ret == null) {
                    ret = new HashSet<>(allowedAccesses);
                } else {
                    ret.addAll(allowedAccesses);
                }

                if (ret.containsAll(accessTypes)) {
                    break;
                }
            }
        }

        return ret;
    }

    @Override
    public List<RangerPolicy> getExactMatchPolicies(RangerAccessResource resource, String zoneName, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.getExactMatchPolicies(" + resource + ", " + zoneName + ", " + evalContext  + ")");
        }

        List<RangerPolicy>     ret              = null;

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }

            RangerPolicyRepository policyRepository = policyEngine.getRepositoryForZone(zoneName);

            if (policyRepository != null) {
                for (RangerPolicyEvaluator evaluator : policyRepository.getPolicyEvaluators()) {
                    if (evaluator.isCompleteMatch(resource, evalContext)) {
                        if (ret == null) {
                            ret = new ArrayList<>();
                        }

                        ret.add(evaluator.getPolicy());
                    }
                }
            }

        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<==> RangerPolicyAdminImpl.getExactMatchPolicies(" + resource + ", " + zoneName + ", " + evalContext  + "): " + ret);
        }

        return ret;
    }

    @Override
    public List<RangerPolicy> getExactMatchPolicies(RangerPolicy policy, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.getExactMatchPolicies(" + policy + ", " + evalContext + ")");
        }

        List<RangerPolicy>     ret              = null;

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {

            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }

            RangerPolicyRepository policyRepository = policyEngine.getRepositoryForMatchedZone(policy);

            if (policyRepository != null) {
                for (RangerPolicyEvaluator evaluator : policyRepository.getPolicyEvaluators()) {
                    if (evaluator.isCompleteMatch(policy.getResources(), policy.getAdditionalResources(), evalContext)) {
                        if (ret == null) {
                            ret = new ArrayList<>();
                        }

                        ret.add(evaluator.getPolicy());
                    }
                }
            }

        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.getExactMatchPolicies(" + policy + ", " + evalContext + "): " + ret);
        }

        return ret;
    }

    @Override
    public List<RangerPolicy> getMatchingPolicies(RangerAccessResource resource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.getMatchingPolicies(" + resource + ")");
        }
        List<RangerPolicy> ret;

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }
            ret = getMatchingPolicies(resource, RangerPolicyEngine.ANY_ACCESS);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.getMatchingPolicies(" + resource + ") : " + ret.size());
        }

        return ret;
    }

    @Override
    public long getPolicyVersion() {
        long ret;

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }
            ret = policyEngine.getPolicyVersion();
        }
        return ret;
    }

    @Override
    public long getRoleVersion() {
        long ret;
        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }
            ret = policyEngine.getRoleVersion();
        }
        return ret;
    }

    @Override
    public String getServiceName() {
        String ret;

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }
            ret = policyEngine.getServiceName();
        }
        return ret;
    }


    @Override
    public RangerServiceDef getServiceDef() {
        RangerServiceDef ret;
        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }
            ret = policyEngine.getServiceDef();
        }
        return ret;
    }
    @Override
    public void setRoles(RangerRoles roles) {
        try (RangerReadWriteLock.RangerLock writeLock = policyEngine.getWriteLock()) {
            if (LOG.isDebugEnabled()) {
                if (writeLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + writeLock);
                }
            }
            policyEngine.setRoles(roles);
        }
    }

    @Override
    public Set<String> getRolesFromUserAndGroups(String user, Set<String> groups) {
        Set<String> ret;
        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }
            ret = policyEngine.getPluginContext().getAuthContext().getRolesForUserAndGroups(user, groups);
        }
        return ret;
    }

    @Override
    public String getUniquelyMatchedZoneName(GrantRevokeRequest grantRevokeRequest) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.getUniquelyMatchedZoneName(" + grantRevokeRequest + ")");
        }
        String ret;

        try (RangerReadWriteLock.RangerLock readLock = policyEngine.getReadLock()) {
            if (LOG.isDebugEnabled()) {
                if (readLock.isLockingEnabled()) {
                    LOG.debug("Acquired lock - " + readLock);
                }
            }
            ret = policyEngine.getUniquelyMatchedZoneName(grantRevokeRequest.getResource());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.getUniquelyMatchedZoneName(" + grantRevokeRequest + ") : " + ret);
        }

        return ret;
    }

    // This API is used only by test-code; checks only policies within default security-zone
    @Override
    public boolean isAccessAllowedByUnzonedPolicies(Map<String, RangerPolicyResource> resources, List<Map<String, RangerPolicyResource>> additionalResources, String user, Set<String> userGroups, String accessType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.isAccessAllowedByUnzonedPolicies(" + resources + ", " + user + ", " + userGroups + ", " + accessType + ")");
        }

        boolean          ret  = false;
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYENGINE_REQUEST_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICYENGINE_REQUEST_LOG, "RangerPolicyEngine.isAccessAllowed(user=" + user + "," + userGroups + ",accessType=" + accessType + ")");
        }

        for (RangerPolicyEvaluator evaluator : policyEngine.getPolicyRepository().getPolicyEvaluators()) {
            ret = evaluator.isAccessAllowed(resources, additionalResources, user, userGroups, accessType);

            if (ret) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Access granted by policy:[" + evaluator.getPolicy() + "]");
                }
                break;
            }
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.isAccessAllowedByUnzonedPolicies(" + resources + ", " + user + ", " + userGroups + ", " + accessType + "): " + ret);
        }

        return ret;
    }

    // This API is used only by test-code; checks only policies within default security-zone
    @Override
    public List<RangerPolicy> getAllowedUnzonedPolicies(String user, Set<String> userGroups, String accessType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.getAllowedByUnzonedPolicies(" + user + ", " + userGroups + ", " + accessType + ")");
        }

        List<RangerPolicy> ret = new ArrayList<>();

        // TODO: run through evaluator in tagPolicyRepository as well
        for (RangerPolicyEvaluator evaluator : policyEngine.getPolicyRepository().getPolicyEvaluators()) {
            RangerPolicy policy = evaluator.getPolicy();

            boolean isAccessAllowed = isAccessAllowedByUnzonedPolicies(policy.getResources(), policy.getAdditionalResources(), user, userGroups, accessType);

            if (isAccessAllowed) {
                ret.add(policy);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.getAllowedByUnzonedPolicies(" + user + ", " + userGroups + ", " + accessType + "): policyCount=" + ret.size());
        }

        return ret;
    }

    void releaseResources(boolean isForced) {
        if (policyEngine != null) {
            policyEngine.preCleanup(isForced);
        }
    }

    private List<RangerPolicy> getMatchingPolicies(RangerAccessResource resource, String accessType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.getMatchingPolicies(" + resource + ", " + accessType + ")");
        }

        List<RangerPolicy>      ret     = new ArrayList<>();
        RangerAccessRequestImpl request = new RangerAccessRequestImpl(resource, accessType, null, null, null);

        requestProcessor.preProcess(request);

        Set<String> zoneNames = policyEngine.getMatchedZonesForResourceAndChildren(resource);

        if (CollectionUtils.isEmpty(zoneNames)) {
            getMatchingPoliciesForZone(request, null, ret);
        } else {
            for (String zoneName : zoneNames) {
                getMatchingPoliciesForZone(request, zoneName, ret);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyAdminImpl.getMatchingPolicies(" + resource + ", " + accessType + ") : " + ret.size());
        }

        return ret;
    }

    private void getMatchingPoliciesForZone(RangerAccessRequest request, String zoneName, List<RangerPolicy> ret) {
        final RangerPolicyRepository matchedRepository = policyEngine.getRepositoryForZone(zoneName);

        if (matchedRepository != null) {
            if (policyEngine.hasTagPolicies(policyEngine.getTagPolicyRepository())) {
                Set<RangerTagForEval> tags = RangerAccessRequestUtil.getRequestTagsFromContext(request.getContext());

                if (CollectionUtils.isNotEmpty(tags)) {
                    final boolean useTagPoliciesFromDefaultZone = !policyEngine.isResourceZoneAssociatedWithTagService(zoneName);

                    for (RangerTagForEval tag : tags) {
                        RangerAccessResource        tagResource      = new RangerTagResource(tag.getType(), policyEngine.getTagPolicyRepository().getServiceDef());
                        RangerAccessRequest         tagRequest       = new RangerTagAccessRequest(tag, policyEngine.getTagPolicyRepository().getServiceDef(), request);
                        List<RangerPolicyEvaluator> likelyEvaluators = policyEngine.getTagPolicyRepository().getLikelyMatchPolicyEvaluators(tagRequest);

                        for (RangerPolicyEvaluator evaluator : likelyEvaluators) {
                            String policyZoneName = evaluator.getPolicy().getZoneName();

                            if (useTagPoliciesFromDefaultZone) {
                                if (StringUtils.isNotEmpty(policyZoneName)) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Tag policy [zone:" + policyZoneName + "] does not belong to default zone. Not evaluating this policy:[" + evaluator.getPolicy() + "]");
                                    }

                                    continue;
                                }
                            } else {
                                if (!StringUtils.equals(zoneName, policyZoneName)) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Tag policy [zone:" + policyZoneName + "] does not belong to the zone:[" + zoneName + "] of the accessed resource. Not evaluating this policy:[" + evaluator.getPolicy() + "]");
                                    }

                                    continue;
                                }
                            }

                            for (RangerPolicyResourceEvaluator resourceEvaluator : evaluator.getResourceEvaluators()) {
                                RangerPolicyResourceMatcher matcher = resourceEvaluator.getPolicyResourceMatcher();

                                if (matcher != null &&
                                        (request.isAccessTypeAny() ? matcher.isMatch(tagResource, RangerPolicyResourceMatcher.MatchScope.ANY, null) : matcher.isMatch(tagResource, null))) {
                                    ret.add(evaluator.getPolicy());

                                    break;
                                }
                            }
                        }

                    }
                }
            }


            if (policyEngine.hasResourcePolicies(matchedRepository)) {
                List<RangerPolicyEvaluator> likelyEvaluators = matchedRepository.getLikelyMatchPolicyEvaluators(request);

                for (RangerPolicyEvaluator evaluator : likelyEvaluators) {
                    for (RangerPolicyResourceEvaluator resourceEvaluator : evaluator.getResourceEvaluators()) {
                        RangerPolicyResourceMatcher matcher = resourceEvaluator.getPolicyResourceMatcher();

                        if (matcher != null &&
                                (request.isAccessTypeAny() ? matcher.isMatch(request.getResource(), RangerPolicyResourceMatcher.MatchScope.ANY, null) : matcher.isMatch(request.getResource(), null))) {
                            ret.add(evaluator.getPolicy());

                            break;
                        }
                    }
                }
            }

        }
    }

    private Map<String, RangerPolicyResource> getPolicyResourcesWithMacrosReplaced(Map<String, RangerPolicyResource> resources, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyAdminImpl.getPolicyResourcesWithMacrosReplaced(" + resources  + ", " + evalContext + ")");
        }

        final Map<String, RangerPolicyResource>  ret;

        Collection<String> resourceKeys = resources == null ? null : resources.keySet();

        if (CollectionUtils.isNotEmpty(resourceKeys)) {
            ret = new HashMap<>();

            for (String resourceName : resourceKeys) {
                RangerPolicyResource resourceValues = resources.get(resourceName);
                List<String>         values         = resourceValues == null ? null : resourceValues.getValues();

                if (CollectionUtils.isNotEmpty(values)) {
                    StringTokenReplacer tokenReplacer = policyEngine.getStringTokenReplacer(resourceName);

                    if (tokenReplacer != null) {
                        List<String> modifiedValues = new ArrayList<>();

                        for (String value : values) {
                            // RANGER-3082 - replace macros in value with ASTERISK
                            String modifiedValue = tokenReplacer.replaceTokens(value, evalContext);
                            modifiedValues.add(modifiedValue);
                        }

                        RangerPolicyResource modifiedPolicyResource = new RangerPolicyResource(modifiedValues, resourceValues.getIsExcludes(), resourceValues.getIsRecursive());
                        ret.put(resourceName, modifiedPolicyResource);
                    } else {
                        ret.put(resourceName, resourceValues);
                    }
                } else {
                    ret.put(resourceName, resourceValues);
                }
            }
        } else {
            ret = resources;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerPolicyEngineImpl.getPolicyResourcesWithMacrosReplaced(" + resources  + ", " + evalContext + "): " + ret);
        }

        return ret;
    }

    private Set<String> getAllAccessTypes(RangerPolicy policy, RangerServiceDef serviceDef) {
        Set<String> ret = new HashSet<>();

        boolean     isValid = true;
        Map<String, Collection<String>> expandedAccesses = ServiceDefUtil.getExpandedImpliedGrants(serviceDef);

        if (MapUtils.isNotEmpty(expandedAccesses)) {

            Integer policyType = policy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : policy.getPolicyType();

            if (policyType == RangerPolicy.POLICY_TYPE_ACCESS) {
                for (RangerPolicy.RangerPolicyItem item : policy.getPolicyItems()) {
                    List<RangerPolicy.RangerPolicyItemAccess> accesses = item.getAccesses();
                    for (RangerPolicy.RangerPolicyItemAccess access : accesses) {
                        ret.addAll(expandedAccesses.get(access.getType()));
                    }
                }
                for (RangerPolicy.RangerPolicyItem item : policy.getDenyPolicyItems()) {
                    List<RangerPolicy.RangerPolicyItemAccess> accesses = item.getAccesses();
                    for (RangerPolicy.RangerPolicyItemAccess access : accesses) {
                        ret.addAll(expandedAccesses.get(access.getType()));
                    }
                }
                for (RangerPolicy.RangerPolicyItem item : policy.getAllowExceptions()) {
                    List<RangerPolicy.RangerPolicyItemAccess> accesses = item.getAccesses();
                    for (RangerPolicy.RangerPolicyItemAccess access : accesses) {
                        ret.addAll(expandedAccesses.get(access.getType()));
                    }
                }
                for (RangerPolicy.RangerPolicyItem item : policy.getDenyExceptions()) {
                    List<RangerPolicy.RangerPolicyItemAccess> accesses = item.getAccesses();
                    for (RangerPolicy.RangerPolicyItemAccess access : accesses) {
                        ret.addAll(expandedAccesses.get(access.getType()));
                    }
                }
            } else if (policyType == RangerPolicy.POLICY_TYPE_DATAMASK) {
                for (RangerPolicy.RangerPolicyItem item : policy.getDataMaskPolicyItems()) {
                    List<RangerPolicy.RangerPolicyItemAccess> accesses = item.getAccesses();
                    for (RangerPolicy.RangerPolicyItemAccess access : accesses) {
                        ret.addAll(expandedAccesses.get(access.getType()));
                    }
                }
            } else if (policyType == RangerPolicy.POLICY_TYPE_ROWFILTER) {
                for (RangerPolicy.RangerPolicyItem item : policy.getRowFilterPolicyItems()) {
                    List<RangerPolicy.RangerPolicyItemAccess> accesses = item.getAccesses();
                    for (RangerPolicy.RangerPolicyItemAccess access : accesses) {
                        ret.addAll(expandedAccesses.get(access.getType()));
                    }
                }
            } else {
                LOG.error("Unknown policy-type :[" + policyType + "], returning empty access-type set");
                isValid = false;
            }
            if (isValid && ret.isEmpty()) {
                ret.add(RangerPolicyEngine.ADMIN_ACCESS);
            }
        }
        return ret;
    }

    private Set<String> getAllModifiedAccessTypes(RangerPolicy oldPolicy, RangerPolicy policy, RangerServiceDef serviceDef) {

        Set<String> ret = new HashSet<>();

        Map<String, Set<String>> oldUserAccesses  = new HashMap<>();
        Map<String, Set<String>> oldGroupAccesses = new HashMap<>();
        Map<String, Set<String>> oldRoleAccesses  = new HashMap<>();

        Map<String, Set<String>> newUserAccesses  = new HashMap<>();
        Map<String, Set<String>> newGroupAccesses = new HashMap<>();
        Map<String, Set<String>> newRoleAccesses  = new HashMap<>();

        collectAccessTypes(oldPolicy, serviceDef, oldUserAccesses, oldGroupAccesses, oldRoleAccesses);
        collectAccessTypes(policy, serviceDef, newUserAccesses, newGroupAccesses, newRoleAccesses);

        ret.addAll(getAccessTypesDiff(newUserAccesses, oldUserAccesses));
        ret.addAll(getAccessTypesDiff(newGroupAccesses, oldGroupAccesses));
        ret.addAll(getAccessTypesDiff(newRoleAccesses, oldRoleAccesses));

        if (ret.isEmpty()) {
            ret.add(RangerPolicyEngine.ADMIN_ACCESS);
        }
        return ret;
    }

    private void collectAccessTypes(RangerPolicy policy, RangerServiceDef serviceDef, Map<String, Set<String>> userAccesses, Map<String, Set<String>> groupAccesses, Map<String, Set<String>> roleAccesses) {
        Map<String, Collection<String>> expandedAccesses = ServiceDefUtil.getExpandedImpliedGrants(serviceDef);

        if (MapUtils.isNotEmpty(expandedAccesses)) {

            Integer policyType = policy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : policy.getPolicyType();

            if (policyType == RangerPolicy.POLICY_TYPE_ACCESS) {
                collectAccessTypes(expandedAccesses, policy.getPolicyItems(), userAccesses, groupAccesses, roleAccesses);
                collectAccessTypes(expandedAccesses, policy.getDenyPolicyItems(), userAccesses, groupAccesses, roleAccesses);
                collectAccessTypes(expandedAccesses, policy.getAllowExceptions(), userAccesses, groupAccesses, roleAccesses);
                collectAccessTypes(expandedAccesses, policy.getDenyExceptions(), userAccesses, groupAccesses, roleAccesses);
            } else if (policyType == RangerPolicy.POLICY_TYPE_DATAMASK) {
                collectAccessTypes(expandedAccesses, policy.getDataMaskPolicyItems(), userAccesses, groupAccesses, roleAccesses);
            } else if (policyType == RangerPolicy.POLICY_TYPE_ROWFILTER) {
                collectAccessTypes(expandedAccesses, policy.getRowFilterPolicyItems(), userAccesses, groupAccesses, roleAccesses);
            } else {
                LOG.error("Unknown policy-type :[" + policyType + "], returning empty access-type set");
            }
        }
    }

    private void collectAccessTypes(Map<String, Collection<String>> expandedAccesses, List<? extends RangerPolicy.RangerPolicyItem> policyItems, Map<String, Set<String>> userAccesses, Map<String, Set<String>> groupAccesses, Map<String, Set<String>> roleAccesses) {
        for (RangerPolicy.RangerPolicyItem item : policyItems) {

            List<RangerPolicy.RangerPolicyItemAccess> accesses = item.getAccesses();
            Set<String> accessTypes = new HashSet<>();

            for (RangerPolicy.RangerPolicyItemAccess access : accesses) {
                accessTypes.addAll(expandedAccesses.get(access.getType()));
            }

            for (String user : item.getUsers()) {
                Set<String> oldAccesses = userAccesses.get(user);
                if (oldAccesses != null) {
                    oldAccesses.addAll(accessTypes);
                } else {
                    userAccesses.put(user, accessTypes);
                }
            }

            for (String group : item.getGroups()) {
                Set<String> oldAccesses = groupAccesses.get(group);
                if (oldAccesses != null) {
                    oldAccesses.addAll(accessTypes);
                } else {
                    groupAccesses.put(group, accessTypes);
                }
            }

            for (String role : item.getRoles()) {
                Set<String> oldAccesses = roleAccesses.get(role);
                if (oldAccesses != null) {
                    oldAccesses.addAll(accessTypes);
                } else {
                    roleAccesses.put(role, accessTypes);
                }
            }
        }
    }

    private Set<String> getAccessTypesDiff(Map<String, Set<String>> newAccessesMap, Map<String, Set<String>> oldAccessesMap) {
        Set<String> ret = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : newAccessesMap.entrySet()) {
            Set<String> oldAccesses = oldAccessesMap.get(entry.getKey());
            if (oldAccesses != null) {
                Collection<String> added = CollectionUtils.subtract(entry.getValue(), oldAccesses);
                ret.addAll(added);
            } else {
                ret.addAll(entry.getValue());
            }
        }
        for (Map.Entry<String, Set<String>> entry : oldAccessesMap.entrySet()) {
            Set<String> newAccesses = newAccessesMap.get(entry.getKey());
            if (newAccesses != null) {
                Collection<String> removed = CollectionUtils.subtract(entry.getValue(), newAccesses);
                ret.addAll(removed);
            } else {
                ret.addAll(entry.getValue());
            }
        }
        return ret;
    }

    private String getResourceSignature(final RangerPolicy policy) {
        return RangerPolicyResourceSignature.toSignatureString(policy.getResources(), policy.getAdditionalResources());
    }
}

