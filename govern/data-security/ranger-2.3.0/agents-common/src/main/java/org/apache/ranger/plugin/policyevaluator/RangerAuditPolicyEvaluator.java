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

package org.apache.ranger.plugin.policyevaluator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ranger.plugin.model.AuditFilter;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.policyengine.*;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RangerAuditPolicyEvaluator extends RangerDefaultPolicyEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(RangerAuditPolicyEvaluator.class);

    private final RangerAuditPolicy                    auditPolicy;
    private final boolean                              matchAnyResource;
    private final List<RangerAuditPolicyItemEvaluator> auditItemEvaluators = new ArrayList<>();

    public RangerAuditPolicyEvaluator(AuditFilter auditFilter, int priority) {
        this.auditPolicy      = new RangerAuditPolicy(auditFilter, priority);
        this.matchAnyResource = MapUtils.isEmpty(auditFilter.getResources());

        if (LOG.isDebugEnabled()) {
            LOG.debug("RangerAuditPolicyEvaluator(auditFilter=" + auditFilter + ", priority=" + priority + ", matchAnyResource=" + matchAnyResource + ")");
        }
    }

    public RangerAuditPolicy getAuditPolicy() { return auditPolicy; }

    @Override
    public void init(RangerPolicy policy, RangerServiceDef serviceDef, RangerPolicyEngineOptions options) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAuditPolicyEvaluator.init(" + auditPolicy.getId() + ")");
        }

        super.init(auditPolicy, serviceDef, options);

        int policyItemIndex = 1;

        for (RangerAuditPolicyItem policyItem : auditPolicy.getAuditPolicyItems()) {
            RangerAuditPolicyItemEvaluator itemEvaluator = new RangerAuditPolicyItemEvaluator(serviceDef, auditPolicy, policyItem, policyItemIndex, options);
            auditItemEvaluators.add(itemEvaluator);
            policyItemIndex = policyItemIndex + 1;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAuditPolicyEvaluator.init(" + auditPolicy.getId() + ")");
        }
    }

    @Override
    public boolean isAncestorOf(RangerResourceDef resourceDef) {
        return matchAnyResource || super.isAncestorOf(resourceDef);
    }

    @Override
    public void evaluate(RangerAccessRequest request, RangerAccessResult result) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAuditPolicyEvaluator.evaluate(" + auditPolicy.getId() + ", " + request + ", " + result + ")");
        }

        if (request != null && result != null) {
            if (!result.getIsAuditedDetermined()) {
                if (matchResource(request)) {
                    evaluatePolicyItems(request, result);
                }
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAuditPolicyEvaluator.evaluate(" + auditPolicy.getId() + ", " + request + ", " + result + ")");
        }
    }

    @Override
    protected void preprocessPolicy(RangerPolicy policy, RangerServiceDef serviceDef) {
        super.preprocessPolicy(policy, serviceDef);

        Map<String, Collection<String>> impliedAccessGrants = getImpliedAccessGrants(serviceDef);

        if (impliedAccessGrants == null || impliedAccessGrants.isEmpty()) {
            return;
        }

        preprocessPolicyItems(auditPolicy.getAuditPolicyItems(), impliedAccessGrants);
    }

    private boolean matchResource(RangerAccessRequest request) {
        final boolean ret;

        if (!matchAnyResource) {
            RangerPolicyResourceMatcher.MatchType matchType;

            if (RangerTagAccessRequest.class.isInstance(request)) {
                matchType = ((RangerTagAccessRequest) request).getMatchType();

                if (matchType == RangerPolicyResourceMatcher.MatchType.ANCESTOR) {
                    matchType = RangerPolicyResourceMatcher.MatchType.SELF;
                }
            } else {
                RangerPolicyResourceMatcher resourceMatcher = getPolicyResourceMatcher();

                if (resourceMatcher != null) {
                    matchType = resourceMatcher.getMatchType(request.getResource(), request.getContext());
                } else {
                    matchType = RangerPolicyResourceMatcher.MatchType.NONE;
                }
            }

            if (request.isAccessTypeAny()) {
                ret = matchType != RangerPolicyResourceMatcher.MatchType.NONE;
            } else if (request.getResourceMatchingScope() == RangerAccessRequest.ResourceMatchingScope.SELF_OR_DESCENDANTS) {
                ret = matchType != RangerPolicyResourceMatcher.MatchType.NONE;
            } else {
                ret = matchType == RangerPolicyResourceMatcher.MatchType.SELF || matchType == RangerPolicyResourceMatcher.MatchType.SELF_AND_ALL_DESCENDANTS;
            }
        } else {
            ret = true;
        }

        return ret;
    }

    private void evaluatePolicyItems(RangerAccessRequest request, RangerAccessResult result) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAuditPolicyEvaluator.evaluatePolicyItems(" + auditPolicy.getId() + ", " + request + ", " + result + ")");
        }

        for (RangerAuditPolicyItemEvaluator itemEvaluator : auditItemEvaluators) {
           if (itemEvaluator.isMatch(request, result)) {
               Boolean isAudited = itemEvaluator.getIsAudited();

               if (isAudited != null) {
                   result.setIsAudited(isAudited);

                   break;
               }
           }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAuditPolicyEvaluator.evaluatePolicyItems(" + auditPolicy.getId() + ", " + request + ", " + result + ")");
        }
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("RangerAuditPolicyEvaluator={");

        super.toString(sb);

        sb.append("auditPolicy={");
        auditPolicy.toString(sb);
        sb.append("}");

        sb.append(" matchAnyResource={").append(matchAnyResource).append("}");

        sb.append("}");

        return sb;
    }


    public static class RangerAuditPolicyItemEvaluator extends RangerDefaultPolicyItemEvaluator {
        private final RangerAuditPolicyItem auditPolicyItem;
        private final boolean               matchAnyResult;
        private final boolean               matchAnyUser;
        private final boolean               matchAnyAction;
        private final boolean               hasResourceOwner;

        public RangerAuditPolicyItemEvaluator(RangerServiceDef serviceDef, RangerPolicy policy, RangerAuditPolicyItem policyItem, int policyItemIndex, RangerPolicyEngineOptions options) {
            super(serviceDef, policy, policyItem, POLICY_ITEM_TYPE_ALLOW, policyItemIndex, options);

            this.auditPolicyItem  = policyItem;
            this.matchAnyResult   = policyItem.getAccessResult() == null;

            List<String> users    = policyItem.getUsers();
            List<String> groups   = policyItem.getGroups();
            List<String> roles    = policyItem.getRoles();
            this.matchAnyUser     = (CollectionUtils.isEmpty(users) && CollectionUtils.isEmpty(groups) && CollectionUtils.isEmpty(roles)) ||
                                    (CollectionUtils.isNotEmpty(groups) && groups.contains(RangerPolicyEngine.GROUP_PUBLIC)) ||
                                    (CollectionUtils.isNotEmpty(users) && users.contains(RangerPolicyEngine.USER_CURRENT));
            this.matchAnyAction   = policyItem.getActions().isEmpty() && policyItem.getAccessTypes().isEmpty();
            this.hasResourceOwner = CollectionUtils.isNotEmpty(users) && users.contains(RangerPolicyEngine.RESOURCE_OWNER);

            if (LOG.isDebugEnabled()) {
                LOG.debug("RangerAuditPolicyItemEvaluator(" + auditPolicyItem + ", matchAnyUser=" + matchAnyUser + ", matchAnyAction=" + matchAnyAction + ", hasResourceOwner=" + hasResourceOwner + ")");
            }
        }

        public Boolean getIsAudited() { return auditPolicyItem.getIsAudited(); }

        public boolean isMatch(RangerAccessRequest request, RangerAccessResult result) {
            boolean ret = matchAccessResult(result) &&
                          matchUserGroupRole(request) &&
                          matchAction(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("RangerAuditPolicyItemEvaluator.isMatch(" + request + ", " + result + "): ret=" + ret);
            }

            return ret;
        }

        private boolean matchAccessResult(RangerAccessResult result) {
            boolean ret = matchAnyResult;

            if (!ret) {
                switch (auditPolicyItem.getAccessResult()) {
                    case DENIED:
                        ret = result.getIsAccessDetermined() && !result.getIsAllowed();
                        break;

                    case ALLOWED:
                        ret = result.getIsAccessDetermined() && result.getIsAllowed();
                        break;

                    case NOT_DETERMINED:
                        ret = !result.getIsAccessDetermined();
                        break;
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("RangerAuditPolicyItemEvaluator.matchAccessResult(" + result + "): ret=" + ret);
            }

            return ret;
        }

        private boolean matchUserGroupRole(RangerAccessRequest request) {
            boolean ret = matchAnyUser;

            if (!ret) {

                if (auditPolicyItem.getUsers() != null && request.getUser() != null) {
                   ret = auditPolicyItem.getUsers().contains(request.getUser());

                   if (!ret && hasResourceOwner) {
                        String owner = request.getResource() != null ? request.getResource().getOwnerUser() : null;
                        ret = request.getUser().equals(owner);
                   }
                }

                if (!ret && auditPolicyItem.getGroups() != null && request.getUserGroups() != null) {
                    ret = CollectionUtils.containsAny(auditPolicyItem.getGroups(), request.getUserGroups());
                }

                if (!ret && auditPolicyItem.getRoles() != null) {
                    ret = CollectionUtils.containsAny(auditPolicyItem.getRoles(), RangerAccessRequestUtil.getCurrentUserRolesFromContext(request.getContext()));
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("RangerAuditPolicyItemEvaluator.matchUserGroupRole(" + request + "): ret=" + ret);
            }

            return ret;
        }

        private boolean matchAction(RangerAccessRequest request) {
            boolean ret = matchAnyAction;

            if (!ret) {
                if (request.getAction() != null) {
                    ret = auditPolicyItem.getActions().contains(request.getAction());
                }

                if (!ret && request.getAccessType() != null) {
                    ret = auditPolicyItem.getAccessTypes().contains(request.getAccessType());
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("RangerAuditPolicyItemEvaluator.matchAction(" + request + "): ret=" + ret);
            }

            return ret;
        }
    }

    public static class RangerAuditPolicy extends RangerPolicy {
        private final List<RangerAuditPolicyItem> auditPolicyItems;

        public RangerAuditPolicy(AuditFilter auditFilter, int priority) {
            setId((long)priority);
            setResources(auditFilter.getResources());
            setPolicyType(POLICY_TYPE_AUDIT);
            setPolicyPriority(priority);

            this.auditPolicyItems = Collections.singletonList(new RangerAuditPolicyItem(auditFilter));
        }

        public List<RangerAuditPolicyItem> getAuditPolicyItems() { return auditPolicyItems; }
    }

    public static class RangerAuditPolicyItem extends RangerPolicyItem {
        private final AuditFilter.AccessResult accessResult;
        private final Set<String>              actions;
        private final Set<String>              accessTypes;
        private final Boolean                  isAudited;

        public RangerAuditPolicyItem(AuditFilter auditFilter) {
            super(getPolicyItemAccesses(auditFilter.getAccessTypes()), auditFilter.getUsers(), auditFilter.getGroups(), auditFilter.getRoles(), null, null);

            this.accessResult = auditFilter.getAccessResult();
            this.actions      = auditFilter.getActions() != null ? new HashSet<>(auditFilter.getActions()) : Collections.emptySet();
            this.accessTypes  = auditFilter.getAccessTypes() != null ? new HashSet<>(auditFilter.getAccessTypes()) : Collections.emptySet();
            this.isAudited    = auditFilter.getIsAudited();
        }

        public Set<String> getActions() { return actions; }

        public Set<String> getAccessTypes() { return accessTypes; }

        public AuditFilter.AccessResult getAccessResult() { return accessResult; }

        public Boolean getIsAudited() { return isAudited; }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("RangerAuditPolicyItem={");
            super.toString(sb);
            sb.append(" accessResult={").append(accessResult).append("}");

            sb.append(" actions={");
            if (actions != null) {
                for (String action : actions) {
                    if (action != null) {
                        sb.append(action).append(" ");
                    }
                }
            }
            sb.append("}");

            sb.append(" accessTypes={");
            if (accessTypes != null) {
                for (String accessTypes : accessTypes) {
                    if (accessTypes != null) {
                        sb.append(accessTypes).append(" ");
                    }
                }
            }
            sb.append("}");

            sb.append(" isAudited={").append(isAudited).append("}");
            sb.append("}");

            return sb;
        }

        private static List<RangerPolicyItemAccess> getPolicyItemAccesses(List<String> accessTypes) {
            final List<RangerPolicyItemAccess> ret;

            if (accessTypes != null) {
                ret = new ArrayList<>(accessTypes.size());

                for (String accessType : accessTypes) {
                    ret.add(new RangerPolicyItemAccess(accessType));
                }
            } else {
                ret = Collections.emptyList();
            }

            return ret;
        }
    }
}
