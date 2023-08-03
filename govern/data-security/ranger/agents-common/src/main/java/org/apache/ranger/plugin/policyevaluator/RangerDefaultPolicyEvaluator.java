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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.conditionevaluator.RangerAbstractConditionEvaluator;
import org.apache.ranger.plugin.conditionevaluator.RangerConditionEvaluator;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.policyengine.RangerResourceAccessInfo;
import org.apache.ranger.plugin.policyengine.RangerTagAccessRequest;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.ServiceDefUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RangerDefaultPolicyEvaluator extends RangerAbstractPolicyEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerDefaultPolicyEvaluator.class);

	private static final Logger PERF_POLICY_INIT_LOG = RangerPerfTracer.getPerfLogger("policy.init");
	private static final Logger PERF_POLICY_INIT_ACLSUMMARY_LOG = RangerPerfTracer.getPerfLogger("policy.init.ACLSummary");
	private static final Logger PERF_POLICY_REQUEST_LOG = RangerPerfTracer.getPerfLogger("policy.request");
	private static final Logger PERF_POLICYCONDITION_REQUEST_LOG = RangerPerfTracer.getPerfLogger("policycondition.request");

	private List<RangerValidityScheduleEvaluator> validityScheduleEvaluators;
	private List<RangerPolicyItemEvaluator> allowEvaluators;
	private List<RangerPolicyItemEvaluator> denyEvaluators;
	private List<RangerPolicyItemEvaluator> allowExceptionEvaluators;
	private List<RangerPolicyItemEvaluator> denyExceptionEvaluators;
	private int                             customConditionsCount;
	private List<RangerDataMaskPolicyItemEvaluator>  dataMaskEvaluators;
	private List<RangerRowFilterPolicyItemEvaluator> rowFilterEvaluators;
	private List<RangerConditionEvaluator>  conditionEvaluators;
	private String perfTag;
	private PolicyACLSummary aclSummary                 = null;
	private boolean          useAclSummaryForEvaluation = false;
	private boolean          disableRoleResolution      = true;

	@Override
	public int getCustomConditionsCount() {
		return customConditionsCount;
	}

	@Override
	public int getValidityScheduleEvaluatorsCount() {
		return validityScheduleEvaluators.size();
	}

	@Override
	public void init(RangerPolicy policy, RangerServiceDef serviceDef, RangerPolicyEngineOptions options) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.init()");
		}

		StringBuilder perfTagBuffer = new StringBuilder();
		if (policy != null) {
			perfTagBuffer.append("policyId=").append(policy.getId()).append(", policyName=").append(policy.getName());
		}

		perfTag = perfTagBuffer.toString();

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_INIT_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_INIT_LOG, "RangerPolicyEvaluator.init(" + perfTag + ")");
		}

		super.init(policy, serviceDef, options);

		policy = getPolicy();

		preprocessPolicy(policy, serviceDef);

		if(policy != null) {
			validityScheduleEvaluators = createValidityScheduleEvaluators(policy);

			this.disableRoleResolution = options.disableRoleResolution;

			if (!options.disableAccessEvaluationWithPolicyACLSummary) {
				aclSummary = createPolicyACLSummary();
			}

			useAclSummaryForEvaluation = aclSummary != null;

			if (useAclSummaryForEvaluation) {
				allowEvaluators          = Collections.<RangerPolicyItemEvaluator>emptyList();
				denyEvaluators           = Collections.<RangerPolicyItemEvaluator>emptyList();
				allowExceptionEvaluators = Collections.<RangerPolicyItemEvaluator>emptyList();
				denyExceptionEvaluators  = Collections.<RangerPolicyItemEvaluator>emptyList();
			} else {
				allowEvaluators          = createPolicyItemEvaluators(policy, serviceDef, options, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW);

				if (ServiceDefUtil.getOption_enableDenyAndExceptionsInPolicies(serviceDef, pluginContext)) {
					denyEvaluators           = createPolicyItemEvaluators(policy, serviceDef, options, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY);
					allowExceptionEvaluators = createPolicyItemEvaluators(policy, serviceDef, options, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS);
					denyExceptionEvaluators  = createPolicyItemEvaluators(policy, serviceDef, options, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY_EXCEPTIONS);
				} else {
					denyEvaluators           = Collections.<RangerPolicyItemEvaluator>emptyList();
					allowExceptionEvaluators = Collections.<RangerPolicyItemEvaluator>emptyList();
					denyExceptionEvaluators  = Collections.<RangerPolicyItemEvaluator>emptyList();
				}
			}

			dataMaskEvaluators  = createDataMaskPolicyItemEvaluators(policy, serviceDef, options, policy.getDataMaskPolicyItems());
			rowFilterEvaluators = createRowFilterPolicyItemEvaluators(policy, serviceDef, options, policy.getRowFilterPolicyItems());
			conditionEvaluators = createRangerPolicyConditionEvaluator(policy, serviceDef, options);
		} else {
			validityScheduleEvaluators = Collections.<RangerValidityScheduleEvaluator>emptyList();
			allowEvaluators            = Collections.<RangerPolicyItemEvaluator>emptyList();
			denyEvaluators             = Collections.<RangerPolicyItemEvaluator>emptyList();
			allowExceptionEvaluators   = Collections.<RangerPolicyItemEvaluator>emptyList();
			denyExceptionEvaluators    = Collections.<RangerPolicyItemEvaluator>emptyList();
			dataMaskEvaluators         = Collections.<RangerDataMaskPolicyItemEvaluator>emptyList();
			rowFilterEvaluators        = Collections.<RangerRowFilterPolicyItemEvaluator>emptyList();
			conditionEvaluators        = Collections.<RangerConditionEvaluator>emptyList();
		}

		RangerPolicyItemEvaluator.EvalOrderComparator comparator = new RangerPolicyItemEvaluator.EvalOrderComparator();
		Collections.sort(allowEvaluators, comparator);
		Collections.sort(denyEvaluators, comparator);
		Collections.sort(allowExceptionEvaluators, comparator);
		Collections.sort(denyExceptionEvaluators, comparator);

		/* dataMask, rowFilter policyItems must be evaulated in the order given in the policy; hence no sort
		Collections.sort(dataMaskEvaluators);
		Collections.sort(rowFilterEvaluators);
		*/

		RangerPerfTracer.log(perf);

		if (useAclSummaryForEvaluation && (policy.getPolicyType() == null || policy.getPolicyType() == RangerPolicy.POLICY_TYPE_ACCESS)) {
			LOG.info("PolicyEvaluator for policy:[" + policy.getId() + "] is set up to use ACL Summary to evaluate access");
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.init()");
		}
	}

	@Override
    public boolean isApplicable(Date accessTime) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyEvaluator.isApplicable(" + accessTime + ")");
        }

        boolean ret = false;

        if (accessTime != null && CollectionUtils.isNotEmpty(validityScheduleEvaluators)) {
			for (RangerValidityScheduleEvaluator evaluator : validityScheduleEvaluators) {
				if (evaluator.isApplicable(accessTime.getTime())) {
					ret = true;
					break;
				}
			}
        } else {
        	ret = true;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyEvaluator.isApplicable(" + accessTime + ") : " + ret);
        }

        return ret;
    }

    @Override
    public void evaluate(RangerAccessRequest request, RangerAccessResult result) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyEvaluator.evaluate(policyId=" + getPolicy().getId() + ", " + request + ", " + result + ")");
        }

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_REQUEST_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_REQUEST_LOG, "RangerPolicyEvaluator.evaluate(requestHashCode=" + Integer.toHexString(System.identityHashCode(request)) + ","
					+ perfTag + ")");
		}

        if (request != null && result != null) {
			for (RangerPolicyResourceEvaluator resourceEvaluator : getResourceEvaluators()) {
				RangerPolicyResourceMatcher resourceMatcher = resourceEvaluator.getPolicyResourceMatcher();

				if (!result.getIsAccessDetermined() || !result.getIsAuditedDetermined()) {
					RangerPolicyResourceMatcher.MatchType matchType;

					if (RangerTagAccessRequest.class.isInstance(request)) {
						matchType = ((RangerTagAccessRequest) request).getMatchType();
						if (matchType == RangerPolicyResourceMatcher.MatchType.ANCESTOR) {
							matchType = RangerPolicyResourceMatcher.MatchType.SELF;
						}
					} else {
						if (request.getResourceMatchingScope() == RangerAccessRequest.ResourceMatchingScope.SELF_OR_CHILD) {
							request.getContext().put(RangerAccessRequest.RANGER_ACCESS_REQUEST_SCOPE_STRING, RangerAccessRequest.ResourceMatchingScope.SELF_OR_CHILD);
						}
						matchType = resourceMatcher != null ? resourceMatcher.getMatchType(request.getResource(), request.getContext()) : RangerPolicyResourceMatcher.MatchType.NONE;
						request.getContext().remove(RangerAccessRequest.RANGER_ACCESS_REQUEST_SCOPE_STRING);
					}

					final boolean isMatched;

					if (request.isAccessTypeAny() || Boolean.TRUE.equals(RangerAccessRequestUtil.getIsAnyAccessInContext(request.getContext()))) {
						isMatched = matchType != RangerPolicyResourceMatcher.MatchType.NONE;
					} else if (request.getResourceMatchingScope() == RangerAccessRequest.ResourceMatchingScope.SELF_OR_DESCENDANTS) {
						isMatched = matchType != RangerPolicyResourceMatcher.MatchType.NONE;
					} else {
						isMatched = matchType == RangerPolicyResourceMatcher.MatchType.SELF || matchType == RangerPolicyResourceMatcher.MatchType.SELF_AND_ALL_DESCENDANTS;
					}

					if (isMatched) {
						//Evaluate Policy Level Custom Conditions, if any and allowed then go ahead for policyItem level evaluation
						if (matchPolicyCustomConditions(request)) {
							if (!result.getIsAuditedDetermined()) {
								if (isAuditEnabled()) {
									result.setIsAudited(true);
									result.setAuditPolicyId(getPolicy().getId());
								}
							}
							if (!result.getIsAccessDetermined()) {
								if (hasMatchablePolicyItem(request)) {
									evaluatePolicyItems(request, matchType, result);
								}
							}
						}
					}
				}
			}
		}

		RangerPerfTracer.log(perf);

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyEvaluator.evaluate(policyId=" + getPolicy().getId() + ", " + request + ", " + result + ")");
        }
    }

	@Override
	public boolean isMatch(RangerAccessResource resource, Map<String, Object> evalContext) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.isMatch(" + resource + ", " + evalContext + ")");
		}

		boolean ret = false;

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_REQUEST_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_REQUEST_LOG, "RangerPolicyEvaluator.isMatch(resource=" + resource.getAsString() + "," + evalContext + "," + perfTag + ")");
		}

		for (RangerPolicyResourceEvaluator resourceEvaluator : getResourceEvaluators()) {
			RangerPolicyResourceMatcher resourceMatcher = resourceEvaluator.getPolicyResourceMatcher();

			ret = resourceMatcher != null && resourceMatcher.isMatch(resource, evalContext);

			if (ret) {
				break;
			}
		}

		RangerPerfTracer.log(perf);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.isMatch(" + resource + ", " + evalContext + "): " + ret);
		}

		return ret;
	}

	@Override
	public boolean isCompleteMatch(RangerAccessResource resource, Map<String, Object> evalContext) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.isCompleteMatch(" + resource + ", " + evalContext + ")");
		}

		final boolean ret;

		List<RangerPolicyResourceEvaluator> resourceEvaluators = getResourceEvaluators();

		if (resourceEvaluators.size() == 1) {
			RangerPolicyResourceEvaluator resourceEvaluator = resourceEvaluators.get(0);
			RangerPolicyResourceMatcher   resourceMatcher   = resourceEvaluator.getPolicyResourceMatcher();

			ret = resourceMatcher != null && resourceMatcher.isCompleteMatch(resource, evalContext);
		} else {
			ret = false;
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.isCompleteMatch(" + resource + "): " + ret);
		}

		return ret;
	}

	@Override
	public boolean isCompleteMatch(Map<String, RangerPolicyResource> resources, List<Map<String, RangerPolicyResource>> additionalResources, Map<String, Object> evalContext) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.isCompleteMatch(" + resources + ", " + evalContext + ")");
		}

		boolean ret = false;

		List<RangerPolicyResourceEvaluator> resourceEvaluators = getResourceEvaluators();

		for (int i = 0; i < resourceEvaluators.size(); i++) {
			RangerPolicyResourceEvaluator     resourceEvaluator = resourceEvaluators.get(i);
			RangerPolicyResourceMatcher       resourceMatcher   = resourceEvaluator.getPolicyResourceMatcher();
			Map<String, RangerPolicyResource> policyResource    = null;

			if (i == 0) {
				policyResource = resources;
			} else if (additionalResources != null && additionalResources.size() >= i) {
				policyResource = additionalResources.get(i - 1);
			}

			ret = resourceMatcher != null && policyResource != null && resourceMatcher.isCompleteMatch(policyResource, evalContext);

			if (!ret) {
				break;
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.isCompleteMatch(" + resources + ", " + evalContext + "): " + ret);
		}

		return ret;
	}

	@Override
	public Set<String> getAllowedAccesses(RangerAccessResource resource, String user, Set<String> userGroups, Set<String> roles, Set<String> accessTypes) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.getAllowedAccesses(" + resource + ", " + user + ", " + userGroups + ", " + roles + ", " + accessTypes + ")");
		}

		Set<String> ret = null;

		if (isMatch(resource, null)) {
			ret = new HashSet<>();
			for (String accessType : accessTypes) {
				if (isAccessAllowed(user, userGroups, roles, resource.getOwnerUser(), accessType)) {
					ret.add(accessType);
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.getAllowedAccesses(" + resource + ", " + user + ", " + userGroups + ", " + roles + ", " + accessTypes + "): " + ret);
		}

		return ret;
	}

	@Override
	public Set<String> getAllowedAccesses(Map<String, RangerPolicyResource> resources, String user, Set<String> userGroups, Set<String> roles,  Set<String> accessTypes, Map<String, Object> evalContext) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.getAllowedAccesses(" + getPolicy().getId() + ", " + user + ", " + userGroups + ", " + roles + ", " + accessTypes + ", " + evalContext + ")");
		}

		Set<String> ret = null;

		if (isMatch(resources, evalContext)) {
			if (CollectionUtils.isNotEmpty(accessTypes)) {
				ret = new HashSet<>();
				for (String accessType : accessTypes) {
					if (isAccessAllowed(user, userGroups, roles, null, accessType)) {
						ret.add(accessType);
					}
				}
			} else {
				if (isAccessAllowed(user, userGroups, roles, null, null)) {
					ret = new HashSet<>();
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.getAllowedAccesses(" + getPolicy().getId() + ", " + user + ", " + userGroups + ", " + roles + ", " + accessTypes + ", " + evalContext + "): " + ret);
		}

		return ret;
	}
	/*
	 * This is used only by test code
	 */

	@Override
	public boolean isAccessAllowed(Map<String, RangerPolicyResource> resources, List<Map<String, RangerPolicyResource>> additionalResources, String user, Set<String> userGroups, String accessType) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.isAccessAllowed(" + resources + ", " + user + ", " + userGroups + ", " + accessType + ")");
		}

		boolean ret = isAccessAllowed(user, userGroups, null, null, accessType) && isMatch(resources, null);

		if (ret && additionalResources != null) {
			for (Map<String, RangerPolicyResource> additionalResource : additionalResources) {
				ret = isMatch(additionalResource, null);

				if (!ret) {
					break;
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.isAccessAllowed(" + resources + ", " + user + ", " + userGroups + ", " + accessType + "): " + ret);
		}

		return ret;
	}


	@Override
	public void getResourceAccessInfo(RangerAccessRequest request, RangerResourceAccessInfo result) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.getResourceAccessInfo(" + request + ", " + result + ")");
		}

		for (RangerPolicyResourceEvaluator resourceEvaluator : getResourceEvaluators()) {
			RangerPolicyResourceMatcher           resourceMatcher = resourceEvaluator.getPolicyResourceMatcher();
			RangerPolicyResourceMatcher.MatchType matchType;

			if (RangerTagAccessRequest.class.isInstance(request)) {
				matchType = ((RangerTagAccessRequest) request).getMatchType();
			} else {
				matchType = resourceMatcher != null ? resourceMatcher.getMatchType(request.getResource(), request.getContext()) : RangerPolicyResourceMatcher.MatchType.NONE;
			}

			final boolean isMatched = matchType != RangerPolicyResourceMatcher.MatchType.NONE;

			if (isMatched) {

				if (CollectionUtils.isNotEmpty(allowEvaluators)) {
					Set<String> users = new HashSet<>();
					Set<String> groups = new HashSet<>();

					getResourceAccessInfo(request, allowEvaluators, users, groups);

					if (CollectionUtils.isNotEmpty(allowExceptionEvaluators)) {
						Set<String> exceptionUsers = new HashSet<>();
						Set<String> exceptionGroups = new HashSet<>();

						getResourceAccessInfo(request, allowExceptionEvaluators, exceptionUsers, exceptionGroups);

						users.removeAll(exceptionUsers);
						groups.removeAll(exceptionGroups);
					}

					result.getAllowedUsers().addAll(users);
					result.getAllowedGroups().addAll(groups);
				}
				if (matchType != RangerPolicyResourceMatcher.MatchType.DESCENDANT) {
					if (CollectionUtils.isNotEmpty(denyEvaluators)) {
						Set<String> users = new HashSet<String>();
						Set<String> groups = new HashSet<String>();

						getResourceAccessInfo(request, denyEvaluators, users, groups);

						if (CollectionUtils.isNotEmpty(denyExceptionEvaluators)) {
							Set<String> exceptionUsers = new HashSet<String>();
							Set<String> exceptionGroups = new HashSet<String>();

							getResourceAccessInfo(request, denyExceptionEvaluators, exceptionUsers, exceptionGroups);

							users.removeAll(exceptionUsers);
							groups.removeAll(exceptionGroups);
						}

						result.getDeniedUsers().addAll(users);
						result.getDeniedGroups().addAll(groups);
					}
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.getResourceAccessInfo(" + request + ", " + result + ")");
		}
	}

	/*
		This API is called by policy-engine to support components which need to statically determine Ranger ACLs
		for a given resource. It will always return a non-null object. The accesses that cannot be determined
		statically will be marked as CONDITIONAL.
	*/

	@Override
	public PolicyACLSummary getPolicyACLSummary() {
		if (aclSummary == null) {
			boolean forceCreation = true;
			aclSummary = createPolicyACLSummary(forceCreation);
		}

		return aclSummary;
	}

	@Override
	public void updateAccessResult(RangerAccessResult result, RangerPolicyResourceMatcher.MatchType matchType, boolean isAllowed, String reason) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.updateAccessResult(" + result + ", " + matchType +", " + isAllowed + ", " + reason + ", " + getPolicyId() + ")");
		}
		if (!isAllowed) {
			if (matchType != RangerPolicyResourceMatcher.MatchType.DESCENDANT) {
				result.setIsAllowed(false);
				result.setPolicyPriority(getPolicyPriority());
				result.setPolicyId(getPolicyId());
				result.setReason(reason);
				result.setPolicyVersion(getPolicy().getVersion());
			}
		} else {
			if (!result.getIsAllowed()) { // if access is not yet allowed by another policy
				if (matchType != RangerPolicyResourceMatcher.MatchType.ANCESTOR) {
					result.setIsAllowed(true);
					result.setPolicyPriority(getPolicyPriority());
					result.setPolicyId(getPolicyId());
					result.setReason(reason);
					result.setPolicyVersion(getPolicy().getVersion());
				}
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.updateAccessResult(" + result + ", " + matchType +", " + isAllowed + ", " + reason + ", " + getPolicyId() + ")");
		}
	}

	/*
		This API is only called during initialization of Policy Evaluator if policy-engine is configured to use
		PolicyACLSummary for access evaluation (that is, if disableAccessEvaluationWithPolicyACLSummary option
		is set to false). It may return null object if all accesses for all user/groups cannot be determined statically.
	*/

	private PolicyACLSummary createPolicyACLSummary() {
		boolean forceCreation = false;
		return createPolicyACLSummary(forceCreation);
	}

	private PolicyACLSummary createPolicyACLSummary(boolean isCreationForced) {
		PolicyACLSummary ret  = null;
		RangerPerfTracer perf = null;

		if (RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_INIT_ACLSUMMARY_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_INIT_ACLSUMMARY_LOG, "RangerPolicyEvaluator.init.ACLSummary(" + perfTag + ")");
		}

		RangerPolicy policy;
		if (!disableRoleResolution && hasRoles(getPolicy())) {
			policy = getPolicyWithRolesResolved(getPolicy());
		} else {
			policy = getPolicy();
		}

		final boolean hasNonPublicGroupOrConditionsInAllowExceptions = hasNonPublicGroupOrConditions(policy.getAllowExceptions());
		final boolean hasNonPublicGroupOrConditionsInDenyExceptions  = hasNonPublicGroupOrConditions(policy.getDenyExceptions());
		final boolean hasPublicGroupInAllowAndUsersInAllowExceptions = hasPublicGroupAndUserInException(policy.getPolicyItems(), policy.getAllowExceptions());
		final boolean hasPublicGroupInDenyAndUsersInDenyExceptions   = hasPublicGroupAndUserInException(policy.getDenyPolicyItems(), policy.getDenyExceptions());
		final boolean hasContextSensitiveSpecification               = hasContextSensitiveSpecification();
		final boolean hasRoles                                       = hasRoles(policy);
		final boolean isUsableForEvaluation                          =    !hasNonPublicGroupOrConditionsInAllowExceptions
		                                                               && !hasNonPublicGroupOrConditionsInDenyExceptions
		                                                               && !hasPublicGroupInAllowAndUsersInAllowExceptions
		                                                               && !hasPublicGroupInDenyAndUsersInDenyExceptions
                                                                               && !hasContextSensitiveSpecification
                                                                               && !hasRoles;

		if (isUsableForEvaluation || isCreationForced) {
			ret = new PolicyACLSummary();

			for (RangerPolicyItem policyItem : policy.getDenyPolicyItems()) {
				ret.processPolicyItem(policyItem, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY, hasNonPublicGroupOrConditionsInDenyExceptions || hasPublicGroupInDenyAndUsersInDenyExceptions);
			}

			if (!hasNonPublicGroupOrConditionsInDenyExceptions && !hasPublicGroupInDenyAndUsersInDenyExceptions) {
				for (RangerPolicyItem policyItem : policy.getDenyExceptions()) {
					ret.processPolicyItem(policyItem, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY_EXCEPTIONS, false);
				}
			}

			for (RangerPolicyItem policyItem : policy.getPolicyItems()) {
				ret.processPolicyItem(policyItem, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW, hasNonPublicGroupOrConditionsInAllowExceptions || hasPublicGroupInAllowAndUsersInAllowExceptions);
			}

			if (!hasNonPublicGroupOrConditionsInAllowExceptions && !hasPublicGroupInAllowAndUsersInAllowExceptions) {
				for (RangerPolicyItem policyItem : policy.getAllowExceptions()) {
					ret.processPolicyItem(policyItem, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS, false);
				}
			}

			for (RangerRowFilterPolicyItem policyItem : policy.getRowFilterPolicyItems()) {
				ret.processRowFilterPolicyItem(policyItem);
			}

			for (RangerDataMaskPolicyItem policyItem : policy.getDataMaskPolicyItems()) {
				ret.processDataMaskPolicyItem(policyItem);
			}

			final boolean isDenyAllElse = Boolean.TRUE.equals(policy.getIsDenyAllElse());

			final Set<String> allAccessTypeNames;

			if (isDenyAllElse) {
				allAccessTypeNames = new HashSet<>();
				RangerServiceDef serviceDef = getServiceDef();
				for (RangerAccessTypeDef accessTypeDef :serviceDef.getAccessTypes()) {
					if (!StringUtils.equalsIgnoreCase(accessTypeDef.getName(), "all")) {
						allAccessTypeNames.add(accessTypeDef.getName());
					}
				}
			} else {
				allAccessTypeNames = Collections.EMPTY_SET;
			}

			ret.finalizeAcls(isDenyAllElse, allAccessTypeNames);
		}

		RangerPerfTracer.logAlways(perf);

		return ret;
	}

	private RangerPolicy getPolicyWithRolesResolved(final RangerPolicy policy) {
		// Create new policy with no roles in it
		// For each policyItem, expand roles into users and groups; and replace all policyItems with expanded roles - TBD

		RangerPolicy ret = new RangerPolicy();
		ret.updateFrom(policy);
		ret.setId(policy.getId());
		ret.setGuid(policy.getGuid());
		ret.setVersion(policy.getVersion());

		List<RangerPolicyItem> policyItems = new ArrayList<>();
		List<RangerPolicyItem> denyPolicyItems = new ArrayList<>();
		List<RangerPolicyItem> allowExceptions = new ArrayList<>();
		List<RangerPolicyItem> denyExceptions = new ArrayList<>();
		List<RangerDataMaskPolicyItem> dataMaskPolicyItems = new ArrayList<>();
		List<RangerRowFilterPolicyItem> rowFilterPolicyItems = new ArrayList<>();

		for (RangerPolicyItem policyItem : policy.getPolicyItems()) {
			RangerPolicyItem newPolicyItem = new RangerPolicyItem(policyItem.getAccesses(), policyItem.getUsers(), policyItem.getGroups(), policyItem.getRoles(), policyItem.getConditions(), policyItem.getDelegateAdmin());
			getPolicyItemWithRolesResolved(newPolicyItem, policyItem);

			policyItems.add(newPolicyItem);
		}
		ret.setPolicyItems(policyItems);

		for (RangerPolicyItem policyItem : policy.getDenyPolicyItems()) {
			RangerPolicyItem newPolicyItem = new RangerPolicyItem(policyItem.getAccesses(), policyItem.getUsers(), policyItem.getGroups(), policyItem.getRoles(), policyItem.getConditions(), policyItem.getDelegateAdmin());
			getPolicyItemWithRolesResolved(newPolicyItem, policyItem);

			denyPolicyItems.add(newPolicyItem);
		}
		ret.setDenyPolicyItems(denyPolicyItems);

		for (RangerPolicyItem policyItem : policy.getAllowExceptions()) {
			RangerPolicyItem newPolicyItem = new RangerPolicyItem(policyItem.getAccesses(), policyItem.getUsers(), policyItem.getGroups(), policyItem.getRoles(), policyItem.getConditions(), policyItem.getDelegateAdmin());
			getPolicyItemWithRolesResolved(newPolicyItem, policyItem);

			allowExceptions.add(newPolicyItem);
		}
		ret.setAllowExceptions(allowExceptions);

		for (RangerPolicyItem policyItem : policy.getDenyExceptions()) {
			RangerPolicyItem newPolicyItem = new RangerPolicyItem(policyItem.getAccesses(), policyItem.getUsers(), policyItem.getGroups(), policyItem.getRoles(), policyItem.getConditions(), policyItem.getDelegateAdmin());
			getPolicyItemWithRolesResolved(newPolicyItem, policyItem);

			denyExceptions.add(newPolicyItem);
		}
		ret.setDenyExceptions(denyExceptions);

		for (RangerDataMaskPolicyItem policyItem : policy.getDataMaskPolicyItems()) {
			RangerDataMaskPolicyItem newPolicyItem = new RangerDataMaskPolicyItem(policyItem.getAccesses(), policyItem.getDataMaskInfo(), policyItem.getUsers(), policyItem.getGroups(), policyItem.getRoles(), policyItem.getConditions(), policyItem.getDelegateAdmin());
			getPolicyItemWithRolesResolved(newPolicyItem, policyItem);

			dataMaskPolicyItems.add(newPolicyItem);
		}
		ret.setDataMaskPolicyItems(dataMaskPolicyItems);

		for (RangerRowFilterPolicyItem policyItem : policy.getRowFilterPolicyItems()) {
			RangerRowFilterPolicyItem newPolicyItem = new RangerRowFilterPolicyItem(policyItem.getRowFilterInfo(), policyItem.getAccesses(), policyItem.getUsers(), policyItem.getGroups(), policyItem.getRoles(), policyItem.getConditions(), policyItem.getDelegateAdmin());
			getPolicyItemWithRolesResolved(newPolicyItem, policyItem);

			rowFilterPolicyItems.add(newPolicyItem);
		}
		ret.setRowFilterPolicyItems(rowFilterPolicyItems);

		return ret;
	}

	private void getPolicyItemWithRolesResolved(RangerPolicyItem newPolicyItem, final RangerPolicyItem policyItem) {
		Set<String> usersFromRoles = new HashSet<>();
		Set<String> groupsFromRoles = new HashSet<>();

		List<String> roles = policyItem.getRoles();

		for (String role : roles) {
			Set<String> users = getPluginContext().getAuthContext().getRangerRolesUtil().getRoleToUserMapping().get(role);
			Set<String> groups = getPluginContext().getAuthContext().getRangerRolesUtil().getRoleToGroupMapping().get(role);
			if (CollectionUtils.isNotEmpty(users)) {
				usersFromRoles.addAll(users);
			}
			if (CollectionUtils.isNotEmpty(groups)) {
				groupsFromRoles.addAll(groups);
			}
			if (CollectionUtils.isNotEmpty(usersFromRoles) || CollectionUtils.isNotEmpty(groupsFromRoles)) {
				usersFromRoles.addAll(policyItem.getUsers());
				groupsFromRoles.addAll(policyItem.getGroups());

				newPolicyItem.setUsers(new ArrayList<>(usersFromRoles));
				newPolicyItem.setGroups(new ArrayList<>(groupsFromRoles));
				newPolicyItem.setRoles(null);
			}
		}
	}

	private boolean hasPublicGroupAndUserInException(List<RangerPolicyItem> grants, List<RangerPolicyItem> exceptionItems) {
		boolean ret = false;

		if (CollectionUtils.isNotEmpty(exceptionItems)) {
			boolean hasPublicGroupInGrant = false;

			for (RangerPolicyItem policyItem : grants) {
				if (policyItem.getGroups().contains(RangerPolicyEngine.GROUP_PUBLIC) || policyItem.getUsers().contains(RangerPolicyEngine.USER_CURRENT)) {
					hasPublicGroupInGrant = true;
					break;
				}
			}

			if (hasPublicGroupInGrant) {
				boolean hasPublicGroupInException = false;

				for (RangerPolicyItem policyItem : exceptionItems) {
					if (policyItem.getGroups().contains(RangerPolicyEngine.GROUP_PUBLIC) || policyItem.getUsers().contains(RangerPolicyEngine.USER_CURRENT)) {
						hasPublicGroupInException = true;
						break;
					}
				}

				if (!hasPublicGroupInException) {
					ret = true;
				}
			}
		}

		return ret;
	}

	protected void evaluatePolicyItems(RangerAccessRequest request, RangerPolicyResourceMatcher.MatchType matchType, RangerAccessResult result) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.evaluatePolicyItems(" + request + ", " + result + ", " + matchType + ")");
		}
		if (useAclSummaryForEvaluation && (getPolicy().getPolicyType() == null || getPolicy().getPolicyType() == RangerPolicy.POLICY_TYPE_ACCESS)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using ACL Summary for access evaluation. PolicyId=[" + getPolicyId() + "]");
			}
			Integer accessResult = lookupPolicyACLSummary(request.getUser(), request.getUserGroups(), RangerAccessRequestUtil.getUserRoles(request), request.isAccessTypeAny() || Boolean.TRUE.equals(RangerAccessRequestUtil.getIsAnyAccessInContext(request.getContext())) ? RangerPolicyEngine.ANY_ACCESS : request.getAccessType());
			if (accessResult != null) {
				updateAccessResult(result, matchType, accessResult.equals(RangerPolicyEvaluator.ACCESS_ALLOWED), null);
			} else if (getPolicy().getIsDenyAllElse()) {
				updateAccessResult(result, matchType, false, "matched deny-all-else policy");
			}
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using policyItemEvaluators for access evaluation. PolicyId=[" + getPolicyId() + "]");
			}

			RangerPolicyItemEvaluator matchedPolicyItem = getMatchingPolicyItem(request, result);

			if (matchedPolicyItem != null) {
				matchedPolicyItem.updateAccessResult(this, result, matchType);
			} else if (getPolicy().getIsDenyAllElse() && (getPolicy().getPolicyType() == null || getPolicy().getPolicyType() == RangerPolicy.POLICY_TYPE_ACCESS)) {
				updateAccessResult(result, matchType, false, "matched deny-all-else policy");
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.evaluatePolicyItems(" + request + ", " + result + ", " + matchType + ")");
		}
	}

	private Integer lookupPolicyACLSummary(String user, Set<String> userGroups, Set<String> userRoles, String accessType) {
		Integer accessResult = null;

		Map<String, PolicyACLSummary.AccessResult> accesses = aclSummary.getUsersAccessInfo().get(user);

		accessResult = lookupAccess(user, accessType, accesses);

		if (accessResult == null) {

			Set<String> groups = new HashSet<>();
			groups.add(RangerPolicyEngine.GROUP_PUBLIC);
			groups.addAll(userGroups);

			for (String userGroup : groups) {
				accesses = aclSummary.getGroupsAccessInfo().get(userGroup);
				accessResult = lookupAccess(userGroup, accessType, accesses);
				if (accessResult != null) {
					break;
				}
			}

			if (accessResult == null) {
				if (userRoles != null) {
					for (String userRole : userRoles) {
						accesses = aclSummary.getRolesAccessInfo().get(userRole);
						accessResult = lookupAccess(userRole, accessType, accesses);
						if (accessResult != null) {
							break;
						}
					}
				}
			}
		}

		return accessResult;
	}

	private Integer lookupAccess(String userOrGroup, String accessType, Map<String, PolicyACLSummary.AccessResult> accesses) {
		Integer ret = null;
		if (accesses != null) {
			if (accessType.equals(RangerPolicyEngine.ANY_ACCESS)) {
				ret = getAccessResultForAnyAccess(accesses);
			} else {
				PolicyACLSummary.AccessResult accessResult = accesses.get(accessType);
				if (accessResult != null) {
					if (accessResult.getResult() == RangerPolicyEvaluator.ACCESS_CONDITIONAL) {
						LOG.error("Access should not be conditional at this point! user=[" + userOrGroup + "], " + "accessType=[" + accessType + "]");
					} else {
						ret = accessResult.getResult();
					}
				}
			}
		}
		return ret;
	}

	private Integer getAccessResultForAnyAccess(Map<String, PolicyACLSummary.AccessResult> accesses) {
		final Integer ret;

		int allowedAccessCount = 0;
		int deniedAccessCount = 0;

		for (Map.Entry<String, PolicyACLSummary.AccessResult> entry : accesses.entrySet()) {
			if (StringUtils.equals(entry.getKey(), RangerPolicyEngine.ADMIN_ACCESS)) {
				// Don't count admin access if present
				continue;
			}
			PolicyACLSummary.AccessResult accessResult = entry.getValue();
			if (accessResult.getResult() == RangerPolicyEvaluator.ACCESS_ALLOWED) {
				allowedAccessCount++;
				break;
			} else if (accessResult.getResult() == RangerPolicyEvaluator.ACCESS_DENIED) {
				deniedAccessCount++;
			}
		}

		if (allowedAccessCount > 0) {
			// At least one access allowed
			ret = RangerPolicyEvaluator.ACCESS_ALLOWED;
		} else if (deniedAccessCount == getServiceDef().getAccessTypes().size()) {
			// All accesses explicitly denied
			ret = RangerPolicyEvaluator.ACCESS_DENIED;
		} else {
			ret = null;
		}

		return ret;
	}

	protected RangerPolicyItemEvaluator getDeterminingPolicyItem(String user, Set<String> userGroups, Set<String> roles, String owner, String accessType) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.getDeterminingPolicyItem(" + user + ", " + userGroups + ", " + roles + ", " + owner + ", " + accessType + ")");
		}

		RangerPolicyItemEvaluator ret = null;

		/*
		 *  1. if a deny matches without hitting any deny-exception, return that
		 *  2. if an allow matches without hitting any allow-exception, return that
		 */
		ret = getMatchingPolicyItem(user, userGroups, roles, owner, accessType, denyEvaluators, denyExceptionEvaluators);

		if(ret == null) {
			ret = getMatchingPolicyItem(user, userGroups, roles, owner, accessType, allowEvaluators, allowExceptionEvaluators);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.getDeterminingPolicyItem(" + user + ", " + userGroups + ", " + roles + ", " + owner + ", " + accessType + "): " + ret);
		}

		return ret;
	}

	private void getResourceAccessInfo(RangerAccessRequest request, List<? extends RangerPolicyItemEvaluator> policyItems, Set<String> users, Set<String> groups) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.getResourceAccessInfo(" + request + ", " + policyItems + ", " + users + ", " + groups + ")");
		}

		if (CollectionUtils.isNotEmpty(policyItems)) {
			for (RangerPolicyItemEvaluator policyItemEvaluator : policyItems) {
				if (policyItemEvaluator.matchAccessType(request.getAccessType()) && policyItemEvaluator.matchCustomConditions(request)) {
					if (CollectionUtils.isNotEmpty(policyItemEvaluator.getPolicyItem().getUsers())) {
						users.addAll(policyItemEvaluator.getPolicyItem().getUsers());
					}

					if (CollectionUtils.isNotEmpty(policyItemEvaluator.getPolicyItem().getGroups())) {
						groups.addAll(policyItemEvaluator.getPolicyItem().getGroups());
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.getResourceAccessInfo(" + request + ", " + policyItems + ", " + users + ", " + groups + ")");
		}
	}

	protected boolean isMatch(RangerPolicy policy, Map<String, Object> evalContext) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.isMatch(" + policy.getId() + ", " + evalContext + ")");
		}

		final boolean ret = isMatch(policy.getResources(), evalContext);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.isMatch(" + policy.getId() + ", " + evalContext + "): " + ret);
		}

		return ret;
	}

	protected boolean isMatch(Map<String, RangerPolicyResource> resources, Map<String, Object> evalContext) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.isMatch(" + resources + ", " + evalContext + ")");
		}

		boolean ret = false;

		for (RangerPolicyResourceEvaluator resourceEvaluator : getResourceEvaluators()) {
			RangerPolicyResourceMatcher resourceMatcher = resourceEvaluator.getPolicyResourceMatcher();

			ret = resourceMatcher != null && resourceMatcher.isMatch(resources, evalContext);

			if (ret) {
				break;
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.isMatch(" + resources + ", " + evalContext + "): " + ret);
		}

		return ret;
	}

	protected boolean isAccessAllowed(String user, Set<String> userGroups, Set<String> roles, String owner, String accessType) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.isAccessAllowed(" + user + ", " + userGroups + ", " + roles + ", " +  owner + ", " + accessType + ")");
		}

		boolean ret = false;

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_REQUEST_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_REQUEST_LOG, "RangerPolicyEvaluator.isAccessAllowed(hashCode=" + Integer.toHexString(System.identityHashCode(this)) + "," + perfTag + ")");
		}

		if (useAclSummaryForEvaluation && (getPolicy().getPolicyType() == null || getPolicy().getPolicyType() == RangerPolicy.POLICY_TYPE_ACCESS)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using ACL Summary for checking if access is allowed. PolicyId=[" + getPolicyId() +"]");
			}

			Integer accessResult = StringUtils.isEmpty(accessType) ? null : lookupPolicyACLSummary(user, userGroups, roles, accessType);
			if (accessResult != null && accessResult.equals(RangerPolicyEvaluator.ACCESS_ALLOWED)) {
				ret = true;
			}
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using policyItemEvaluators for checking if access is allowed. PolicyId=[" + getPolicyId() +"]");
			}

			RangerPolicyItemEvaluator item = this.getDeterminingPolicyItem(user, userGroups, roles, owner, accessType);

			if (item != null && item.getPolicyItemType() == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW) {
				ret = true;
			}
		}

		RangerPerfTracer.log(perf);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.isAccessAllowed(" + user + ", " + userGroups + ", " + roles + ", " + owner + ", " + accessType + "): " + ret);
		}

		return ret;
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("RangerDefaultPolicyEvaluator={");

		super.toString(sb);

		for (RangerPolicyResourceEvaluator resourceEvaluator : getResourceEvaluators()) {
			RangerPolicyResourceMatcher resourceMatcher = resourceEvaluator.getPolicyResourceMatcher();

			sb.append("resourceMatcher={");
			if(resourceMatcher != null) {
				resourceMatcher.toString(sb);
			}
			sb.append("} ");
		}

		sb.append("}");

		return sb;
	}

	protected void preprocessPolicy(RangerPolicy policy, RangerServiceDef serviceDef) {
		if(policy == null || (!hasAllow() && !hasDeny()) || serviceDef == null) {
			return;
		}

		Map<String, Collection<String>> impliedAccessGrants = getImpliedAccessGrants(serviceDef);

		if(impliedAccessGrants == null || impliedAccessGrants.isEmpty()) {
			return;
		}

		preprocessPolicyItems(policy.getPolicyItems(), impliedAccessGrants);
		preprocessPolicyItems(policy.getDenyPolicyItems(), impliedAccessGrants);
		preprocessPolicyItems(policy.getAllowExceptions(), impliedAccessGrants);
		preprocessPolicyItems(policy.getDenyExceptions(), impliedAccessGrants);
		preprocessPolicyItems(policy.getDataMaskPolicyItems(), impliedAccessGrants);
		preprocessPolicyItems(policy.getRowFilterPolicyItems(), impliedAccessGrants);
	}

	protected void preprocessPolicyItems(List<? extends RangerPolicyItem> policyItems, Map<String, Collection<String>> impliedAccessGrants) {
		for(RangerPolicyItem policyItem : policyItems) {
			if(CollectionUtils.isEmpty(policyItem.getAccesses())) {
				continue;
			}

			// Only one round of 'expansion' is done; multi-level impliedGrants (like shown below) are not handled for now
			// multi-level impliedGrants: given admin=>write; write=>read: must imply admin=>read,write
			for(Map.Entry<String, Collection<String>> e : impliedAccessGrants.entrySet()) {
				String             accessType    = e.getKey();
				Collection<String> impliedGrants = e.getValue();

				RangerPolicyItemAccess access = getAccess(policyItem, accessType);

				if(access == null) {
					continue;
				}

				for(String impliedGrant : impliedGrants) {
					RangerPolicyItemAccess impliedAccess = getAccess(policyItem, impliedGrant);

					if(impliedAccess == null) {
						impliedAccess = new RangerPolicyItemAccess(impliedGrant, access.getIsAllowed());

						policyItem.getAccesses().add(impliedAccess);
					} else {
						if(! impliedAccess.getIsAllowed()) {
							impliedAccess.setIsAllowed(access.getIsAllowed());
						}
					}
				}
			}
		}
	}

	protected Map<String, Collection<String>> getImpliedAccessGrants(RangerServiceDef serviceDef) {
		Map<String, Collection<String>> ret = null;

		if(serviceDef != null && !CollectionUtils.isEmpty(serviceDef.getAccessTypes())) {
			for(RangerAccessTypeDef accessTypeDef : serviceDef.getAccessTypes()) {
				if(!CollectionUtils.isEmpty(accessTypeDef.getImpliedGrants())) {
					if(ret == null) {
						ret = new HashMap<>();
					}

					Collection<String> impliedAccessGrants = ret.get(accessTypeDef.getName());

					if(impliedAccessGrants == null) {
						impliedAccessGrants = new HashSet<>();

						ret.put(accessTypeDef.getName(), impliedAccessGrants);
					}

					impliedAccessGrants.addAll(accessTypeDef.getImpliedGrants());
				}
			}
		}

		return ret;
	}

	private RangerPolicyItemAccess getAccess(RangerPolicyItem policyItem, String accessType) {
		RangerPolicyItemAccess ret = null;

		if(policyItem != null && CollectionUtils.isNotEmpty(policyItem.getAccesses())) {
			for(RangerPolicyItemAccess itemAccess : policyItem.getAccesses()) {
				if (itemAccess != null && StringUtils.equalsIgnoreCase(itemAccess.getType(), accessType)) {
					ret = itemAccess;

					break;
				}
			}
		}

		return ret;
	}

    private List<RangerValidityScheduleEvaluator> createValidityScheduleEvaluators(RangerPolicy policy) {
	    List<RangerValidityScheduleEvaluator> ret = null;

	    if (CollectionUtils.isNotEmpty(policy.getValiditySchedules())) {
	        ret = new ArrayList<>();

	        for (RangerValiditySchedule schedule : policy.getValiditySchedules()) {
	            ret.add(new RangerValidityScheduleEvaluator(schedule));
            }
        } else {
            ret = Collections.<RangerValidityScheduleEvaluator>emptyList();
        }

        return ret;
    }

	private List<RangerPolicyItemEvaluator> createPolicyItemEvaluators(RangerPolicy policy, RangerServiceDef serviceDef, RangerPolicyEngineOptions options, int policyItemType) {
		List<RangerPolicyItemEvaluator> ret         = null;
		List<RangerPolicyItem>          policyItems = null;

		if(isPolicyItemTypeEnabled(serviceDef, policyItemType)) {
			if (policyItemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW) {
				policyItems = policy.getPolicyItems();
			} else if (policyItemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY) {
				policyItems = policy.getDenyPolicyItems();
			} else if (policyItemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS) {
				policyItems = policy.getAllowExceptions();
			} else if (policyItemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY_EXCEPTIONS) {
				policyItems = policy.getDenyExceptions();
			}
		}

		if(CollectionUtils.isNotEmpty(policyItems)) {
			ret = new ArrayList<>();

			int policyItemCounter = 1;

			for(RangerPolicyItem policyItem : policyItems) {
				RangerPolicyItemEvaluator itemEvaluator = new RangerDefaultPolicyItemEvaluator(serviceDef, policy, policyItem, policyItemType, policyItemCounter++, options);

				itemEvaluator.init();

				ret.add(itemEvaluator);

				if(CollectionUtils.isNotEmpty(itemEvaluator.getConditionEvaluators())) {
					customConditionsCount += itemEvaluator.getConditionEvaluators().size();
				}
			}
		} else {
			ret = Collections.<RangerPolicyItemEvaluator>emptyList();
		}

		return ret;
	}

	private List<RangerDataMaskPolicyItemEvaluator> createDataMaskPolicyItemEvaluators(RangerPolicy policy, RangerServiceDef serviceDef, RangerPolicyEngineOptions options, List<RangerDataMaskPolicyItem> policyItems) {
		List<RangerDataMaskPolicyItemEvaluator> ret = null;

		if(CollectionUtils.isNotEmpty(policyItems)) {
			ret = new ArrayList<>();

			int policyItemCounter = 1;

			for(RangerDataMaskPolicyItem policyItem : policyItems) {
				RangerDataMaskPolicyItemEvaluator itemEvaluator = new RangerDefaultDataMaskPolicyItemEvaluator(serviceDef, policy, policyItem, policyItemCounter++, options);

				itemEvaluator.init();

				ret.add(itemEvaluator);

				if(CollectionUtils.isNotEmpty(itemEvaluator.getConditionEvaluators())) {
					customConditionsCount += itemEvaluator.getConditionEvaluators().size();
				}
			}
		} else {
			ret = Collections.<RangerDataMaskPolicyItemEvaluator>emptyList();
		}

		return ret;
	}

	private List<RangerRowFilterPolicyItemEvaluator> createRowFilterPolicyItemEvaluators(RangerPolicy policy, RangerServiceDef serviceDef, RangerPolicyEngineOptions options, List<RangerRowFilterPolicyItem> policyItems) {
		List<RangerRowFilterPolicyItemEvaluator> ret = null;

		if(CollectionUtils.isNotEmpty(policyItems)) {
			ret = new ArrayList<>();

			int policyItemCounter = 1;

			for(RangerRowFilterPolicyItem policyItem : policyItems) {
				RangerRowFilterPolicyItemEvaluator itemEvaluator = new RangerDefaultRowFilterPolicyItemEvaluator(serviceDef, policy, policyItem, policyItemCounter++, options);

				itemEvaluator.init();

				ret.add(itemEvaluator);

				if(CollectionUtils.isNotEmpty(itemEvaluator.getConditionEvaluators())) {
					customConditionsCount += itemEvaluator.getConditionEvaluators().size();
				}
			}
		} else {
			ret = Collections.<RangerRowFilterPolicyItemEvaluator>emptyList();
		}

		return ret;
	}

	private boolean isPolicyItemTypeEnabled(RangerServiceDef serviceDef, int policyItemType) {
		boolean ret = true;

		if(policyItemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY ||
		   policyItemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS ||
		   policyItemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY_EXCEPTIONS) {
			ret = ServiceDefUtil.getOption_enableDenyAndExceptionsInPolicies(serviceDef, pluginContext);
		}

		return ret;
	}

	private static boolean hasNonPublicGroupOrConditions(List<RangerPolicyItem> policyItems) {
		boolean ret = false;
		for (RangerPolicyItem policyItem : policyItems) {
			if (CollectionUtils.isNotEmpty(policyItem.getConditions())) {
				ret = true;
				break;
			}
			List<String> allGroups = policyItem.getGroups();
			if (CollectionUtils.isNotEmpty(allGroups) && !allGroups.contains(RangerPolicyEngine.GROUP_PUBLIC)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	protected RangerPolicyItemEvaluator getMatchingPolicyItem(RangerAccessRequest request, RangerAccessResult result) {
		RangerPolicyItemEvaluator ret = null;

		Integer policyType = getPolicy().getPolicyType();
		if (policyType == null) {
			policyType = RangerPolicy.POLICY_TYPE_ACCESS;
		}

		switch (policyType) {
			case RangerPolicy.POLICY_TYPE_ACCESS: {
				ret = getMatchingPolicyItemForAccessPolicyForSpecificAccess(request, result);
				break;
			}
			case RangerPolicy.POLICY_TYPE_DATAMASK: {
				ret = getMatchingPolicyItem(request, dataMaskEvaluators);
				break;
			}
			case RangerPolicy.POLICY_TYPE_ROWFILTER: {
				ret = getMatchingPolicyItem(request, rowFilterEvaluators);
				break;
			}
			default:
				break;
		}

		return ret;
	}

	protected RangerPolicyItemEvaluator getMatchingPolicyItemForAccessPolicyForSpecificAccess(RangerAccessRequest request, RangerAccessResult result) {
		RangerPolicyItemEvaluator ret = getMatchingPolicyItem(request, denyEvaluators, denyExceptionEvaluators);

		if(ret == null && !result.getIsAccessDetermined()) { // a deny policy could have set isAllowed=true, but in such case it wouldn't set isAccessDetermined=true
			ret = getMatchingPolicyItem(request, allowEvaluators, allowExceptionEvaluators);
		}

		return ret;
	}

	protected <T extends RangerPolicyItemEvaluator> T getMatchingPolicyItem(RangerAccessRequest request, List<T> evaluators) {
		T ret = getMatchingPolicyItem(request, evaluators, null);

		return ret;
	}

	private <T extends RangerPolicyItemEvaluator> T getMatchingPolicyItem(RangerAccessRequest request, List<T> evaluators, List<T> exceptionEvaluators) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyEvaluator.getMatchingPolicyItem(" + request + ")");
        }

        T ret = null;

        if(CollectionUtils.isNotEmpty(evaluators)) {
            for (T evaluator : evaluators) {
                if(evaluator.isMatch(request)) {
                    ret = evaluator;

                    break;
                }
            }
        }

        if(ret != null && CollectionUtils.isNotEmpty(exceptionEvaluators)) {
            for (T exceptionEvaluator : exceptionEvaluators) {
                if(exceptionEvaluator.isMatch(request)) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("RangerDefaultPolicyEvaluator.getMatchingPolicyItem(" + request + "): found exception policyItem(" + exceptionEvaluator.getPolicyItem() + "); ignoring the matchedPolicyItem(" + ret.getPolicyItem() + ")");
                    }

                    ret = null;

                    break;
                }
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyEvaluator.getMatchingPolicyItem(" + request + "): " + ret);
        }

        return ret;
    }

	private <T extends RangerPolicyItemEvaluator> T getMatchingPolicyItem(String user, Set<String> userGroups, Set<String> roles, String owner, String accessType, List<T> evaluators, List<T> exceptionEvaluators) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerDefaultPolicyEvaluator.getMatchingPolicyItem(" + user + ", " + userGroups + ", " + roles + ", " + owner + ", " + accessType + ")");
        }

        T ret = null;

        if(CollectionUtils.isNotEmpty(evaluators)) {
            for (T evaluator : evaluators) {
                if(evaluator.matchUserGroupAndOwner(user, userGroups, roles, owner) && evaluator.matchAccessType(accessType)) {
                    ret = evaluator;

                    break;
                }
            }
        }

        if(ret != null && CollectionUtils.isNotEmpty(exceptionEvaluators)) {
            for (T exceptionEvaluator : exceptionEvaluators) {
                if(exceptionEvaluator.matchUserGroupAndOwner(user, userGroups, roles, owner) && exceptionEvaluator.matchAccessType(accessType)) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("RangerDefaultPolicyEvaluator.getMatchingPolicyItem(" + user + ", " + userGroups + ", " + accessType + "): found exception policyItem(" + exceptionEvaluator.getPolicyItem() + "); ignoring the matchedPolicyItem(" + ret.getPolicyItem() + ")");
                    }

                    ret = null;

                    break;
                }
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerDefaultPolicyEvaluator.getMatchingPolicyItem(" + user + ", " + userGroups + ", " + roles + ", " + owner + ", " + accessType + "): " + ret);
        }
        return ret;
    }

	// Policy Level Condition evaluator
	private boolean matchPolicyCustomConditions(RangerAccessRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyEvaluator.matchPolicyCustomConditions(" + request + ")");
		}

		boolean ret = true;

		if (CollectionUtils.isNotEmpty(conditionEvaluators)) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("RangerDefaultPolicyEvaluator.matchPolicyCustomConditions(): conditionCount=" + conditionEvaluators.size());
			}
			for(RangerConditionEvaluator conditionEvaluator : conditionEvaluators) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("evaluating condition: " + conditionEvaluator);
				}
				RangerPerfTracer perf = null;

				if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYCONDITION_REQUEST_LOG)) {

					String conditionType = null;
					if (conditionEvaluator instanceof RangerAbstractConditionEvaluator) {
						conditionType = ((RangerAbstractConditionEvaluator)conditionEvaluator).getPolicyItemCondition().getType();
					}

					perf = RangerPerfTracer.getPerfTracer(PERF_POLICYCONDITION_REQUEST_LOG, "RangerConditionEvaluator.matchPolicyCustomConditions(policyId=" + getPolicyId() +  ",policyConditionType=" + conditionType + ")");
				}

				boolean conditionEvalResult = conditionEvaluator.isMatched(request);

				RangerPerfTracer.log(perf);

				if (!conditionEvalResult) {
					if(LOG.isDebugEnabled()) {
						LOG.debug(conditionEvaluator + " returned false");
					}
					ret = false;
					break;
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyEvaluator.matchCustomConditions(" + request + "): " + ret);
		}

		return ret;
	}

	private List<RangerConditionEvaluator> createRangerPolicyConditionEvaluator(RangerPolicy policy,
																				RangerServiceDef serviceDef,
																				RangerPolicyEngineOptions options) {
		List<RangerConditionEvaluator> rangerConditionEvaluators = null;

		RangerCustomConditionEvaluator rangerConditionEvaluator = new RangerCustomConditionEvaluator();

		rangerConditionEvaluators = rangerConditionEvaluator.getRangerPolicyConditionEvaluator(policy,serviceDef,options);

		if (rangerConditionEvaluators != null) {
			customConditionsCount += rangerConditionEvaluators.size();
		}

		return rangerConditionEvaluators;
	}

}
