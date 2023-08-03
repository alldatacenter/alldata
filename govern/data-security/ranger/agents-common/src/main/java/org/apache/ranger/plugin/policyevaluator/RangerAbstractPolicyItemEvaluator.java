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

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.conditionevaluator.RangerConditionEvaluator;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;


public abstract class RangerAbstractPolicyItemEvaluator implements RangerPolicyItemEvaluator {

	private static final int RANGER_POLICY_ITEM_EVAL_ORDER_DEFAULT = 1000;

	private static final int RANGER_POLICY_ITEM_EVAL_ORDER_MAX_DISCOUNT_USERSGROUPS       =  100;
	private static final int RANGER_POLICY_ITEM_EVAL_ORDER_MAX_DISCOUNT_ACCESS_TYPES      =  25;
	private static final int RANGER_POLICY_ITEM_EVAL_ORDER_MAX_DISCOUNT_CUSTOM_CONDITIONS =  25;
	private static final int RANGER_POLICY_ITEM_EVAL_ORDER_CUSTOM_CONDITION_PENALTY       =   5;

	final RangerPolicy 				policy;
	final RangerPolicyEngineOptions options;
	final RangerServiceDef          serviceDef;
	final RangerPolicyItem          policyItem;
	final int                       policyItemType;
	final int						policyItemIndex;
	final long                      policyId;
	final int                       evalOrder;

	List<RangerConditionEvaluator> conditionEvaluators = Collections.<RangerConditionEvaluator>emptyList();

	RangerAbstractPolicyItemEvaluator(RangerServiceDef serviceDef, RangerPolicy policy, RangerPolicyItem policyItem, int policyItemType, int policyItemIndex, RangerPolicyEngineOptions options) {
		this.serviceDef     = serviceDef;
		this.policyItem     = policyItem;
		this.policyItemType = policyItemType;
		this.policyItemIndex = policyItemIndex;
		this.options        = options;
		this.policyId       = policy != null && policy.getId() != null ? policy.getId() : -1;
		this.evalOrder      = computeEvalOrder();
		this.policy         = policy;
	}

	@Override
	public List<RangerConditionEvaluator> getConditionEvaluators() {
		return conditionEvaluators;
	}

	@Override
	public int getEvalOrder() {
		return evalOrder;
	}

	@Override
	public RangerPolicyItem getPolicyItem() {
		return policyItem;
	}

	@Override
	public int getPolicyItemType() {
		return policyItemType;
	}

	@Override
	public int getPolicyItemIndex() {
		return policyItemIndex;
	}

	@Override
	public String getComments() {
		return null;
	}

	protected String getServiceType() {
		return serviceDef != null ? serviceDef.getName() : null;
	}

	protected boolean getConditionsDisabledOption() {
		return options != null && options.disableCustomConditions;
	}

	private int computeEvalOrder() {
		int evalOrder = RANGER_POLICY_ITEM_EVAL_ORDER_DEFAULT;

		if(policyItem != null) {
			if((CollectionUtils.isNotEmpty(policyItem.getGroups()) && policyItem.getGroups().contains(RangerPolicyEngine.GROUP_PUBLIC))
				|| (CollectionUtils.isNotEmpty(policyItem.getUsers()) && policyItem.getUsers().contains(RangerPolicyEngine.USER_CURRENT))) {
				evalOrder -= RANGER_POLICY_ITEM_EVAL_ORDER_MAX_DISCOUNT_USERSGROUPS;
			} else {
				int userGroupCount = 0;

				if(! CollectionUtils.isEmpty(policyItem.getUsers())) {
					userGroupCount += policyItem.getUsers().size();
				}

				if(! CollectionUtils.isEmpty(policyItem.getGroups())) {
					userGroupCount += policyItem.getGroups().size();
				}

				evalOrder -= Math.min(RANGER_POLICY_ITEM_EVAL_ORDER_MAX_DISCOUNT_USERSGROUPS, userGroupCount);
			}

			if(CollectionUtils.isNotEmpty(policyItem.getAccesses())) {
				evalOrder -= Math.round(((float)RANGER_POLICY_ITEM_EVAL_ORDER_MAX_DISCOUNT_ACCESS_TYPES * policyItem.getAccesses().size()) / serviceDef.getAccessTypes().size());
			}

			int customConditionsPenalty = 0;
			if(CollectionUtils.isNotEmpty(policyItem.getConditions())) {
				customConditionsPenalty = RANGER_POLICY_ITEM_EVAL_ORDER_CUSTOM_CONDITION_PENALTY * policyItem.getConditions().size();
			}
			int customConditionsDiscount = RANGER_POLICY_ITEM_EVAL_ORDER_MAX_DISCOUNT_CUSTOM_CONDITIONS - customConditionsPenalty;
	        if(customConditionsDiscount > 0) {
				evalOrder -= customConditionsDiscount;
	        }
		}

        return evalOrder;
	}
}
