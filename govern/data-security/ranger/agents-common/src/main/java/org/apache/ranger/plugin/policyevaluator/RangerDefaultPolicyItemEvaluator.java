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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.conditionevaluator.RangerAbstractConditionEvaluator;
import org.apache.ranger.plugin.conditionevaluator.RangerConditionEvaluator;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RangerDefaultPolicyItemEvaluator extends RangerAbstractPolicyItemEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerDefaultPolicyItemEvaluator.class);

	private static final Logger PERF_POLICYITEM_REQUEST_LOG = RangerPerfTracer.getPerfLogger("policyitem.request");
	private static final Logger PERF_POLICYCONDITION_REQUEST_LOG = RangerPerfTracer.getPerfLogger("policycondition.request");

	private boolean hasCurrentUser;
	private boolean hasResourceOwner;

	public RangerDefaultPolicyItemEvaluator(RangerServiceDef serviceDef, RangerPolicy policy, RangerPolicyItem policyItem, int policyItemType, int policyItemIndex, RangerPolicyEngineOptions options) {
		super(serviceDef, policy, policyItem, policyItemType, policyItemIndex, options);
	}

	public void init() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator(policyId=" + policyId + ", policyItem=" + policyItem + ", serviceType=" + getServiceType() + ", conditionsDisabled=" + getConditionsDisabledOption() + ")");
		}

		RangerCustomConditionEvaluator rangerCustomConditionEvaluator = new RangerCustomConditionEvaluator();

		conditionEvaluators = rangerCustomConditionEvaluator.getPolicyItemConditionEvaluator(policy,policyItem,serviceDef,options,policyItemIndex);

		List<String> users = policyItem.getUsers();
		this.hasCurrentUser = CollectionUtils.isNotEmpty(users) && users.contains(RangerPolicyEngine.USER_CURRENT);
		this.hasResourceOwner = CollectionUtils.isNotEmpty(users) && users.contains(RangerPolicyEngine.RESOURCE_OWNER);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyItemEvaluator(policyId=" + policyId + ", conditionsCount=" + getConditionEvaluators().size() + ")");
		}
	}

	@Override
	public boolean isMatch(RangerAccessRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator.isMatch(" + request + ")");
		}

		boolean ret = false;

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYITEM_REQUEST_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICYITEM_REQUEST_LOG, "RangerPolicyItemEvaluator.isMatch(resource=" + request.getResource().getAsString()  + ")");
		}

		if(policyItem != null) {
			if(matchUserGroupAndOwner(request)) {
				if (request.isAccessTypeDelegatedAdmin()) { // used only in grant/revoke scenario
					if (policyItem.getDelegateAdmin()) {
						ret = true;
					}
				} else if (CollectionUtils.isNotEmpty(policyItem.getAccesses())) {
					boolean isAccessTypeMatched = false;

					for (RangerPolicy.RangerPolicyItemAccess access : policyItem.getAccesses()) {
						if (access.getIsAllowed() && StringUtils.equalsIgnoreCase(access.getType(), request.getAccessType())) {
							isAccessTypeMatched = true;
							break;
						}
					}

					if(isAccessTypeMatched) {
						if(matchCustomConditions(request)) {
							ret = true;
						}
					}
				}
			}
		}

		RangerPerfTracer.log(perf);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyItemEvaluator.isMatch(" + request + "): " + ret);
		}

		return ret;
	}

	@Override
	public boolean matchUserGroupAndOwner(String user, Set<String> userGroups, Set<String> roles, String owner) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator.matchUserGroup(" + policyItem + ", " + user + ", " + userGroups + ", " + roles + ", " + owner + ")");
		}

		boolean ret = false;

		if(policyItem != null) {
			if(!ret && user != null && policyItem.getUsers() != null) {
				ret = hasCurrentUser || policyItem.getUsers().contains(user);
			}
			if(!ret && userGroups != null && policyItem.getGroups() != null) {
				ret = policyItem.getGroups().contains(RangerPolicyEngine.GROUP_PUBLIC) ||
						!Collections.disjoint(policyItem.getGroups(), userGroups);
			}
			if (!ret && CollectionUtils.isNotEmpty(roles) && CollectionUtils.isNotEmpty(policyItem.getRoles())) {
				ret = !Collections.disjoint(policyItem.getRoles(), roles);
			}
			if (!ret && hasResourceOwner) {
				ret = user != null && user.equals(owner);
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyItemEvaluator.matchUserGroup(" + policyItem + ", " + user + ", " + userGroups + ", " + roles + ", " + owner + "): " + ret);
		}

		return ret;
	}

	private boolean matchUserGroupAndOwner(RangerAccessRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator.matchUserGroupAndOwner(" + request + ")");
		}

		boolean ret = false;

		String user = request.getUser();
		Set<String> userGroups = request.getUserGroups();

		RangerAccessResource accessedResource = request.getResource();
		String resourceOwner = accessedResource != null ? accessedResource.getOwnerUser() : null;

		if (!ret) {
			Set<String> roles = null;
			if (CollectionUtils.isNotEmpty(policyItem.getRoles())) {
				roles = RangerAccessRequestUtil.getUserRoles(request);
			}
			ret = matchUserGroupAndOwner(user, userGroups, roles, resourceOwner);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyItemEvaluator.matchUserGroupAndOwner(" + request + "): " + ret);
		}

		return ret;
	}
	@Override
	public boolean matchAccessType(String accessType) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator.matchAccessType(" + accessType + ")");
		}

		boolean ret = false;

		if(policyItem != null) {
			boolean isAdminAccess = StringUtils.equals(accessType, RangerPolicyEngine.ADMIN_ACCESS);

			if(isAdminAccess) {
				ret = policyItem.getDelegateAdmin();
			} else {
				if(CollectionUtils.isNotEmpty(policyItem.getAccesses())) {
					boolean isAnyAccess = StringUtils.equals(accessType, RangerPolicyEngine.ANY_ACCESS);

					for(RangerPolicyItemAccess itemAccess : policyItem.getAccesses()) {
						if(! itemAccess.getIsAllowed()) {
							continue;
						}

						if(isAnyAccess) {
							ret = true;

							break;
						} else if(StringUtils.equalsIgnoreCase(itemAccess.getType(), accessType)) {
							ret = true;

							break;
						}
					}
				} else if (StringUtils.isEmpty(accessType)) {
					ret = true;
				}
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyItemEvaluator.matchAccessType(" + accessType + "): " + ret);
		}

		return ret;
	}

	@Override
	public boolean matchCustomConditions(RangerAccessRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator.matchCustomConditions(" + request + ")");
		}

		boolean ret = true;

		if (CollectionUtils.isNotEmpty(conditionEvaluators)) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("RangerDefaultPolicyItemEvaluator.matchCustomConditions(): conditionCount=" + conditionEvaluators.size());
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

					perf = RangerPerfTracer.getPerfTracer(PERF_POLICYCONDITION_REQUEST_LOG, "RangerConditionEvaluator.matchCondition(policyId=" + policyId + ",policyItemIndex=" + getPolicyItemIndex() + ",policyConditionType=" + conditionType + ")");
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
			LOG.debug("<== RangerDefaultPolicyItemEvaluator.matchCustomConditions(" + request + "): " + ret);
		}

		return ret;
	}

	@Override
	public void updateAccessResult(RangerPolicyEvaluator policyEvaluator, RangerAccessResult result, RangerPolicyResourceMatcher.MatchType matchType) {
		policyEvaluator.updateAccessResult(result, matchType, getPolicyItemType() != RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY, getComments());
	}

	RangerPolicyConditionDef getConditionDef(String conditionName) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator.getConditionDef(" + conditionName + ")");
		}

		RangerPolicyConditionDef ret = null;

		if (serviceDef != null && CollectionUtils.isNotEmpty(serviceDef.getPolicyConditions())) {
			for(RangerPolicyConditionDef conditionDef : serviceDef.getPolicyConditions()) {
				if(StringUtils.equals(conditionName, conditionDef.getName())) {
					ret = conditionDef;

					break;
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyItemEvaluator.getConditionDef(" + conditionName + "): " + ret);
		}

		return ret;
	}

	RangerConditionEvaluator newConditionEvaluator(String className) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultPolicyItemEvaluator.newConditionEvaluator(" + className + ")");
		}

		RangerConditionEvaluator evaluator = null;

		try {
			@SuppressWarnings("unchecked")
			Class<RangerConditionEvaluator> matcherClass = (Class<RangerConditionEvaluator>)Class.forName(className);

			evaluator = matcherClass.newInstance();
		} catch(Throwable t) {
			LOG.error("RangerDefaultPolicyItemEvaluator.newConditionEvaluator(" + className + "): error instantiating evaluator", t);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultPolicyItemEvaluator.newConditionEvaluator(" + className + "): " + evaluator);
		}

		return evaluator;
	}
}
