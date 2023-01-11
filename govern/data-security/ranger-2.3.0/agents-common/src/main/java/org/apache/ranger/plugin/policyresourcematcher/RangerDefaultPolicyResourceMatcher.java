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

package org.apache.ranger.plugin.policyresourcematcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefHelper;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.resourcematcher.RangerDefaultResourceMatcher;
import org.apache.ranger.plugin.resourcematcher.RangerResourceMatcher;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerDefaultPolicyResourceMatcher implements RangerPolicyResourceMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(RangerDefaultPolicyResourceMatcher.class);

    private static final Logger PERF_POLICY_RESOURCE_MATCHER_INIT_LOG = RangerPerfTracer.getPerfLogger("policyresourcematcher.init");
    private static final Logger PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG = RangerPerfTracer.getPerfLogger("policyresourcematcher.match");

    protected RangerServiceDef                  serviceDef;
    protected int                               policyType;
    protected Map<String, RangerPolicyResource> policyResources;

    private Map<String, RangerResourceMatcher>  allMatchers;
    private boolean                             needsDynamicEval = false;
    private List<RangerResourceDef>             validResourceHierarchy;
    private boolean                             isInitialized = false;
    private RangerServiceDefHelper              serviceDefHelper;

    @Override
    public void setServiceDef(RangerServiceDef serviceDef) {
        if (isInitialized) {
            LOG.warn("RangerDefaultPolicyResourceMatcher is already initialized. init() must be done again after updating serviceDef");
        }

        this.serviceDef = serviceDef;
    }

    @Override
    public void setPolicy(RangerPolicy policy) {
        if (isInitialized) {
            LOG.warn("RangerDefaultPolicyResourceMatcher is already initialized. init() must be done again after updating policy");
        }

        if (policy == null) {
            setPolicyResources(null, RangerPolicy.POLICY_TYPE_ACCESS);
        } else {
            setPolicyResources(policy.getResources(), policy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : policy.getPolicyType());
        }
    }

    @Override
    public void setPolicyResources(Map<String, RangerPolicyResource> policyResources) {
        if (isInitialized) {
            LOG.warn("RangerDefaultPolicyResourceMatcher is already initialized. init() must be done again after updating policy-resources");
        }

        setPolicyResources(policyResources, RangerPolicy.POLICY_TYPE_ACCESS);
    }

    @Override
    public void setPolicyResources(Map<String, RangerPolicyResource> policyResources, int policyType) {
        this.policyResources = policyResources;
        this.policyType = policyType;
    }

    @Override
    public void setServiceDefHelper(RangerServiceDefHelper serviceDefHelper) {
        this.serviceDefHelper = serviceDefHelper;
    }

    @Override
    public RangerServiceDef getServiceDef() {
        return serviceDef;
    }

    @Override
    public RangerResourceMatcher getResourceMatcher(String resourceName) {
        return allMatchers != null ? allMatchers.get(resourceName) : null;
    }

    @Override
    public boolean getNeedsDynamicEval() { return needsDynamicEval; }

    @Override
    public void init() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyResourceMatcher.init()");
        }

        allMatchers            = null;
        needsDynamicEval       = false;
        validResourceHierarchy = null;
        isInitialized          = false;

        String errorText = "";

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_RESOURCE_MATCHER_INIT_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_RESOURCE_MATCHER_INIT_LOG, "RangerDefaultPolicyResourceMatcher.init()");
        }

        if (policyResources != null && !policyResources.isEmpty() && serviceDef != null) {
            serviceDefHelper                                    = serviceDefHelper == null ? new RangerServiceDefHelper(serviceDef, false) : serviceDefHelper;

            Set<List<RangerResourceDef>> resourceHierarchies   = serviceDefHelper.getResourceHierarchies(policyType, policyResources.keySet());
            int                          validHierarchiesCount = 0;

            for (List<RangerResourceDef> resourceHierarchy : resourceHierarchies) {

                if (isHierarchyValidForResources(resourceHierarchy, policyResources)) {
                    validHierarchiesCount++;

                    if (validHierarchiesCount == 1) {
                        validResourceHierarchy = resourceHierarchy;
                    } else {
                        validResourceHierarchy = null;
                    }
                } else {
                    LOG.warn("RangerDefaultPolicyResourceMatcher.init(): gaps found in policyResources, skipping hierarchy:[" + resourceHierarchies + "]");
                }
            }

            if (validHierarchiesCount > 0) {
                allMatchers = new HashMap<>();

                for (List<RangerResourceDef> resourceHierarchy : resourceHierarchies) {
                    for (RangerResourceDef resourceDef : resourceHierarchy) {
                        String resourceName = resourceDef.getName();

                        if (allMatchers.containsKey(resourceName)) {
                            continue;
                        }

                        RangerPolicyResource policyResource = policyResources.get(resourceName);

                        if (policyResource == null) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("RangerDefaultPolicyResourceMatcher.init(): no matcher created for " + resourceName + ". Continuing ...");
                            }

                            continue;
                        }

                        RangerResourceMatcher matcher = createResourceMatcher(resourceDef, policyResource);

                        if (matcher != null) {
                            if (!needsDynamicEval && matcher.getNeedsDynamicEval()) {
                                needsDynamicEval = true;
                            }

                            allMatchers.put(resourceName, matcher);
                        } else {
                            LOG.error("RangerDefaultPolicyResourceMatcher.init(): failed to find matcher for resource " + resourceName);

                            allMatchers = null;
                            errorText   = "no matcher found for resource " + resourceName;

                            break;
                        }
                    }

                    if (allMatchers == null) {
                        break;
                    }
                }
            } else {
                errorText = "policyResources elements are not part of any valid resourcedef hierarchy.";
            }
        } else {
            errorText = "policyResources is null or empty, or serviceDef is null.";
        }

        if (allMatchers == null && policyType != RangerPolicy.POLICY_TYPE_AUDIT) {
            serviceDefHelper       = null;
            validResourceHierarchy = null;

            Set<String>   policyResourceKeys = policyResources == null ? null : policyResources.keySet();
            String        serviceDefName     = serviceDef == null ? "" : serviceDef.getName();
            StringBuilder keysString         = new StringBuilder();

            if (CollectionUtils.isNotEmpty(policyResourceKeys)) {
                for (String policyResourceKeyName : policyResourceKeys) {
                    keysString.append(policyResourceKeyName).append(" ");
                }
            }

            LOG.error("RangerDefaultPolicyResourceMatcher.init() failed: " + errorText + " (serviceDef=" + serviceDefName + ", policyResourceKeys=" + keysString.toString());
        } else {
            isInitialized = true;
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyResourceMatcher.init(): ret=" + isInitialized);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        return toString(sb).toString();
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("RangerDefaultPolicyResourceMatcher={");

        sb.append("isInitialized=").append(isInitialized).append(", ");

        sb.append("matchers={");
        if(allMatchers != null) {
            for(RangerResourceMatcher matcher : allMatchers.values()) {
                sb.append("{").append(matcher).append("} ");
            }
        }
        sb.append("} ");

        sb.append("}");

        return sb;
    }

    @Override
    public boolean isCompleteMatch(RangerAccessResource resource, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyResourceMatcher.isCompleteMatch(" + resource + ", " + evalContext + ")");
        }

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG, "RangerDefaultPolicyResourceMatcher.grantRevokeMatch()");
        }

        boolean            ret          = false;
        Collection<String> resourceKeys = resource == null ? null : resource.getKeys();
        Collection<String> policyKeys   = policyResources == null ? null : policyResources.keySet();
        boolean            keysMatch    = resourceKeys != null && policyKeys != null && CollectionUtils.isEqualCollection(resourceKeys, policyKeys);

        if (keysMatch) {
            for (RangerResourceDef resourceDef : serviceDef.getResources()) {
                String                resourceName  = resourceDef.getName();
                Object                resourceValue = resource.getValue(resourceName);
                RangerResourceMatcher matcher       = getResourceMatcher(resourceName);

                if (resourceValue == null) {
                    ret = matcher == null || matcher.isCompleteMatch(null, evalContext);
                } else if (resourceValue instanceof String) {
                    String strValue = (String) resourceValue;

                    if (StringUtils.isEmpty(strValue)) {
                        ret = matcher == null || matcher.isCompleteMatch(strValue, evalContext);
                    } else {
                        ret = matcher != null && matcher.isCompleteMatch(strValue, evalContext);
                    }
                } else { // return false for any other type of resourceValue
                    ret = false;
                }

                if (!ret) {
                    break;
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isCompleteMatch(): keysMatch=false. resourceKeys=" + resourceKeys + "; policyKeys=" + policyKeys);
            }
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyResourceMatcher.isCompleteMatch(" + resource + ", " + evalContext + "): " + ret);
        }

        return ret;
    }

    @Override
    public boolean isCompleteMatch(Map<String, RangerPolicyResource> resources, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyResourceMatcher.isCompleteMatch(" + resources + ", " + evalContext + ")");
        }

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG, "RangerDefaultPolicyResourceMatcher.applyPolicyMatch()");
        }

        boolean            ret          = false;
        Collection<String> resourceKeys = resources == null ? null : resources.keySet();
        Collection<String> policyKeys   = policyResources == null ? null : policyResources.keySet();
        boolean            keysMatch    = resourceKeys != null && policyKeys != null && CollectionUtils.isEqualCollection(resourceKeys, policyKeys);

        if (keysMatch) {
            for (RangerResourceDef resourceDef : serviceDef.getResources()) {
                String               resourceName   = resourceDef.getName();
                RangerPolicyResource resourceValues = resources.get(resourceName);
                RangerPolicyResource policyValues   = policyResources == null ? null : policyResources.get(resourceName);

                if (resourceValues == null || CollectionUtils.isEmpty(resourceValues.getValues())) {
                    ret = (policyValues == null || CollectionUtils.isEmpty(policyValues.getValues()));
                } else if (policyValues != null && CollectionUtils.isNotEmpty(policyValues.getValues())) {
                    ret = CollectionUtils.isEqualCollection(resourceValues.getValues(), policyValues.getValues());
                }

                if (!ret) {
                    break;
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("isCompleteMatch(): keysMatch=false. resourceKeys=" + resourceKeys + "; policyKeys=" + policyKeys);
            }
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyResourceMatcher.isCompleteMatch(" + resources + ", " + evalContext + "): " + ret);
        }

        return ret;
    }

    @Override
    public boolean isMatch(RangerPolicy policy, MatchScope scope, Map<String, Object> evalContext) {
        boolean ret = false;

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG, "RangerDefaultPolicyResourceMatcher.getPoliciesNonLegacy()");
        }

        Map<String, RangerPolicyResource> resources = policy.getResources();

        if (policy.getPolicyType() == policyType && MapUtils.isNotEmpty(resources)) {
            List<RangerResourceDef> hierarchy = getMatchingHierarchy(resources.keySet());

            if (CollectionUtils.isNotEmpty(hierarchy)) {
                MatchType                matchType      = MatchType.NONE;
                RangerAccessResourceImpl accessResource = new RangerAccessResourceImpl();

                accessResource.setServiceDef(serviceDef);

                // Build up accessResource resourceDef by resourceDef.
                // For each resourceDef,
                //         examine policy-values one by one.
                //         The first value that is acceptable, that is,
                //             value matches in any way, is used for that resourceDef, and
                //            next resourceDef is processed.
                //         If none of the values matches, the policy as a whole definitely will not match,
                //        therefore, the match is failed
                // After all resourceDefs are processed, and some match is achieved at every
                // level, the final matchType (which is for the entire policy) is checked against
                // requested scope to determine the match-result.

                // Unit tests in TestDefaultPolicyResourceForPolicy.java, TestDefaultPolicyResourceMatcher.java
                // test_defaultpolicyresourcematcher_for_hdfs_policy.json, and
                // test_defaultpolicyresourcematcher_for_hive_policy.json, and
                // test_defaultPolicyResourceMatcher.json

                boolean skipped = false;

                for (RangerResourceDef resourceDef : hierarchy) {
                    String               name           = resourceDef.getName();
                    RangerPolicyResource policyResource = resources.get(name);

                    if (policyResource != null && CollectionUtils.isNotEmpty(policyResource.getValues())) {
                        ret       = false;
                        matchType = MatchType.NONE;

                        if (!skipped) {
                            for (String value : policyResource.getValues()) {
                                accessResource.setValue(name, value);

                                matchType = getMatchType(accessResource, evalContext);

                                if (matchType != MatchType.NONE) { // One value for this resourceDef matched
                                    ret = true;
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    } else {
                        skipped = true;
                    }

                    if (!ret) { // None of the values specified for this resourceDef matched, no point in continuing with next resourceDef
                        break;
                    }
                }

                ret = ret && isMatch(scope, matchType);
            }
        }

        RangerPerfTracer.log(perf);

        return ret;
    }

    @Override
    public boolean isMatch(RangerAccessResource resource, Map<String, Object> evalContext) {
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG, "RangerDefaultPolicyResourceMatcher.grantRevokeMatch()");
        }

        /*
        * There is already API to get the delegateAdmin permissions for a map of policyResources.
        * That implementation should be reused for figuring out delegateAdmin permissions for a resource as well.
         */

        Map<String, RangerPolicyResource> policyResources = null;

        for (RangerResourceDef resourceDef : serviceDef.getResources()) {
            String resourceName = resourceDef.getName();
            Object resourceValue = resource.getValue(resourceName);
            if (resourceValue instanceof String) {
                String strValue = (String) resourceValue;

                if (policyResources == null) {
                    policyResources = new HashMap<>();
                }
                policyResources.put(resourceName, new RangerPolicyResource(strValue));
            } else if (resourceValue != null) { // return false for any other type of resourceValue
                policyResources = null;

                break;
            }
        }
        final boolean ret = MapUtils.isNotEmpty(policyResources) && isMatch(policyResources, evalContext);

        RangerPerfTracer.log(perf);

        return ret;
    }

    @Override
    public boolean isMatch(Map<String, RangerPolicyResource> resources, Map<String, Object> evalContext) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyResourceMatcher.isMatch(" + resources  + ", " + evalContext + ")");
        }

        boolean ret = false;

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG, "RangerDefaultPolicyResourceMatcher.delegateAdminMatch()");
        }

        if(serviceDef != null && serviceDef.getResources() != null) {
            Collection<String> resourceKeys = resources == null ? null : resources.keySet();
            Collection<String> policyKeys   = policyResources == null ? null : policyResources.keySet();

            boolean keysMatch = CollectionUtils.isEmpty(resourceKeys) || (policyKeys != null && policyKeys.containsAll(resourceKeys));

            if(keysMatch) {
                for(RangerResourceDef resourceDef : serviceDef.getResources()) {
                    String                resourceName   = resourceDef.getName();
                    RangerPolicyResource  resourceValues = resources == null ? null : resources.get(resourceName);
                    List<String>          values         = resourceValues == null ? null : resourceValues.getValues();
                    RangerResourceMatcher matcher        = allMatchers == null ? null : allMatchers.get(resourceName);

                    if (matcher != null) {
                        if (CollectionUtils.isNotEmpty(values)) {
                            for (String value : values) {
                                ret = matcher.isMatch(value, evalContext);
                                if (!ret) {
                                    break;
                                }
                            }
                        } else {
                            ret = matcher.isMatchAny();
                        }
                    } else {
                        ret = CollectionUtils.isEmpty(values);
                    }

                    if(! ret) {
                        break;
                    }
                }
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("isMatch(): keysMatch=false. resourceKeys=" + resourceKeys + "; policyKeys=" + policyKeys);
                }
            }
        }

        RangerPerfTracer.log(perf);

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyResourceMatcher.isMatch(" + resources  + ", " + evalContext + "): " + ret);
        }

        return ret;
    }

    @Override
    public boolean isMatch(RangerAccessResource resource, MatchScope scope, Map<String, Object> evalContext) {
        MatchType matchType = getMatchType(resource, evalContext);
        return isMatch(scope, matchType);
    }

    @Override
    public MatchType getMatchType(RangerAccessResource resource, Map<String, Object> evalContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyResourceMatcher.getMatchType(" + resource + evalContext + ")");
        }
        MatchType ret              = MatchType.NONE;

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_RESOURCE_MATCHER_MATCH_LOG, "RangerDefaultPolicyResourceMatcher.getMatchType()");
        }

        if (resource != null && policyResources != null) {
            int resourceKeysSize = resource.getKeys() == null ? 0 : resource.getKeys().size();

            if (policyResources.size() == 0 && resourceKeysSize == 0) {
                ret = MatchType.SELF;
            } else {
                List<RangerResourceDef> hierarchy = getMatchingHierarchy(resource);
                if (CollectionUtils.isNotEmpty(hierarchy)) {

                    int lastNonAnyMatcherIndex = -1;
                    int matchersSize = 0;

                    for (RangerResourceDef resourceDef : hierarchy) {
                        RangerResourceMatcher matcher = getResourceMatcher(resourceDef.getName());
                        if (matcher != null) {
                            if (!matcher.isMatchAny()) {
                                lastNonAnyMatcherIndex = matchersSize;
                            }
                            matchersSize++;
                        } else {
                            break;
                        }
                    }

                    if (resourceKeysSize == 0) {
                        ret = MatchType.SELF;
                    }

                    for (RangerResourceDef resourceDef : hierarchy) {

                        RangerResourceMatcher matcher = getResourceMatcher(resourceDef.getName());
                        Object resourceValue = resource.getValue(resourceDef.getName());

                        if (matcher != null) {
                            if (resourceValue != null || matcher.isMatchAny()) {
                                if (matcher.isMatch(resourceValue, evalContext)) {
                                    ret = MatchType.SELF;
                                } else {
                                    ret = MatchType.NONE;
                                    break;
                                }
                            }
                        } else {
                            if (resourceValue != null) {
                                // More resource-values than matchers
                                ret = MatchType.ANCESTOR;
                            }
                            break;
                        }
                    }

                    if (ret == MatchType.SELF && resourceKeysSize < policyResources.size()) {
                        // More matchers than resource-values
                        if (resourceKeysSize > lastNonAnyMatcherIndex) {
                            // all remaining matchers which matched resource value of null are of type Any
                            ret = MatchType.SELF_AND_ALL_DESCENDANTS;
                        } else {
                            ret = MatchType.DESCENDANT;
                        }
                    }
                }
            }
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyResourceMatcher.getMatchType(" + resource + evalContext + "): " + ret);
        }

        return ret;
    }

    public static boolean isHierarchyValidForResources(List<RangerResourceDef> hierarchy, Map<String, ?> resources) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isHierarchyValidForResources(" + StringUtils.join(hierarchy, ",") + ")");
        }
        boolean ret = true;

        if (hierarchy != null) {
            boolean skipped = false;

            for (RangerResourceDef resourceDef : hierarchy) {
                String resourceName = resourceDef.getName();
                Object resourceValue = resources.get(resourceName);

                if (resourceValue == null) {
                    if (!skipped) {
                        skipped = true;
                    }
                } else {
                    if (skipped) {
                        ret = false;
                        break;
                    }
                }
            }
        } else {
            ret = false;
         }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isHierarchyValidForResources(" + StringUtils.join(hierarchy, ",") + ") : " + ret);
        }
        return ret;
    }

    private List<RangerResourceDef> getMatchingHierarchy(Set<String> resourceKeys) {
        List<RangerResourceDef> ret = null;

        if (CollectionUtils.isNotEmpty(resourceKeys) && serviceDefHelper != null) {
            Set<List<RangerResourceDef>> resourceHierarchies = serviceDefHelper.getResourceHierarchies(policyType, resourceKeys);

            // pick the shortest hierarchy
            for (List<RangerResourceDef> resourceHierarchy : resourceHierarchies) {
                if (ret == null) {
                    ret = resourceHierarchy;
                } else {
                    if (resourceHierarchy.size() < ret.size()) {
                        ret = resourceHierarchy;
                        if (ret.size() == resourceKeys.size()) {
                            break;
                        }
                    }
                }
            }
        }

        return ret;
    }

    private List<RangerResourceDef> getMatchingHierarchy(RangerAccessResource resource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyResourceMatcher.getMatchingHierarchy(" + resource + ")");
        }

        final List<RangerResourceDef> ret;

        Set<String> policyResourcesKeySet = policyResources.keySet();
        Set<String> resourceKeySet = resource.getKeys();

        if (CollectionUtils.isNotEmpty(resourceKeySet)) {
            List<RangerResourceDef> aValidHierarchy = null;

            if (validResourceHierarchy != null && serviceDefHelper != null) {
                if (serviceDefHelper.hierarchyHasAllResources(validResourceHierarchy, resourceKeySet)) {
                    aValidHierarchy = validResourceHierarchy;
                }
            } else {
                if (policyResourcesKeySet.containsAll(resourceKeySet)) {
                    aValidHierarchy = getMatchingHierarchy(policyResourcesKeySet);
                } else if (resourceKeySet.containsAll(policyResourcesKeySet)) {
                    aValidHierarchy = getMatchingHierarchy(resourceKeySet);
                }
            }
            ret = isHierarchyValidForResources(aValidHierarchy, resource.getAsMap()) ? aValidHierarchy : null;
        } else {
            ret = validResourceHierarchy != null ? validResourceHierarchy : getMatchingHierarchy(policyResourcesKeySet);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyResourceMatcher.getMatchingHierarchy(" + resource + "): " + ret);
        }

        return ret;
    }

    private boolean isMatch(final MatchScope scope, final MatchType matchType) {
        final boolean ret;
        switch (scope) {
            case SELF: {
                ret = matchType == MatchType.SELF || matchType == MatchType.SELF_AND_ALL_DESCENDANTS;
                break;
            }
            case ANCESTOR: {
                ret = matchType == MatchType.ANCESTOR;
                break;
            }
            case DESCENDANT: {
                ret = matchType == MatchType.DESCENDANT;
                break;
            }
            case SELF_OR_ANCESTOR: {
                ret = matchType == MatchType.SELF || matchType == MatchType.SELF_AND_ALL_DESCENDANTS || matchType == MatchType.ANCESTOR;
                break;
            }
            case SELF_OR_DESCENDANT: {
                ret = matchType == MatchType.SELF || matchType == MatchType.SELF_AND_ALL_DESCENDANTS || matchType == MatchType.DESCENDANT;
                break;
            }
            default: {
                ret = matchType != MatchType.NONE;
                break;
            }
        }
        return ret;
    }

    private static RangerResourceMatcher createResourceMatcher(RangerResourceDef resourceDef, RangerPolicyResource resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyResourceMatcher.createResourceMatcher(" + resourceDef + ", " + resource + ")");
        }

        RangerResourceMatcher ret = null;

        if (resourceDef != null) {
            String resName = resourceDef.getName();
            String clsName = resourceDef.getMatcher();

            if (!StringUtils.isEmpty(clsName)) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<RangerResourceMatcher> matcherClass = (Class<RangerResourceMatcher>) Class.forName(clsName);

                    ret = matcherClass.newInstance();
                } catch (Exception excp) {
                    LOG.error("failed to instantiate resource matcher '" + clsName + "' for '" + resName + "'. Default resource matcher will be used", excp);
                }
            }


            if (ret == null) {
                ret = new RangerDefaultResourceMatcher();
            }

            ret.setResourceDef(resourceDef);
            ret.setPolicyResource(resource);
            ret.init();
        } else {
            LOG.error("RangerDefaultPolicyResourceMatcher: RangerResourceDef is null");
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyResourceMatcher.createResourceMatcher(" + resourceDef + ", " + resource + "): " + ret);
        }

        return ret;
    }

}
