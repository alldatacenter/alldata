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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemDataMaskInfo;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerRequestExprResolver;


public class RangerDefaultDataMaskPolicyItemEvaluator extends RangerDefaultPolicyItemEvaluator implements RangerDataMaskPolicyItemEvaluator {
	final private RangerDataMaskPolicyItem  dataMaskPolicyItem;
	final private RangerRequestExprResolver maskedValueExprResolver;
	final private RangerRequestExprResolver maskConditionExprResolver;

	public RangerDefaultDataMaskPolicyItemEvaluator(RangerServiceDef serviceDef, RangerPolicy policy, RangerDataMaskPolicyItem policyItem, int policyItemIndex, RangerPolicyEngineOptions options) {
		super(serviceDef, policy, policyItem, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DATAMASK, policyItemIndex, options);

		dataMaskPolicyItem = policyItem;

		RangerPolicyItemDataMaskInfo dataMaskInfo  = dataMaskPolicyItem != null ? dataMaskPolicyItem.getDataMaskInfo() : null;
		String                       maskedValue   = dataMaskInfo != null ? dataMaskInfo.getValueExpr() : null;
		String                       maskCondition = dataMaskInfo != null ? dataMaskInfo.getConditionExpr() : null;

		if (StringUtils.isNotBlank(maskedValue) && RangerRequestExprResolver.hasExpressions(maskedValue)) {
			maskedValueExprResolver = new RangerRequestExprResolver(maskedValue, getServiceType());
		} else {
			maskedValueExprResolver = null;
		}

		if (StringUtils.isNotBlank(maskCondition) && RangerRequestExprResolver.hasExpressions(maskCondition)) {
			maskConditionExprResolver = new RangerRequestExprResolver(maskCondition, getServiceType());
		} else {
			maskConditionExprResolver = null;
		}
	}

	@Override
	public RangerPolicyItemDataMaskInfo getDataMaskInfo() {
		return dataMaskPolicyItem == null ? null : dataMaskPolicyItem.getDataMaskInfo();
	}

	@Override
	public void updateAccessResult(RangerPolicyEvaluator policyEvaluator, RangerAccessResult result, RangerPolicyResourceMatcher.MatchType matchType) {
		RangerPolicyItemDataMaskInfo dataMaskInfo = getDataMaskInfo();

		if (dataMaskInfo != null) {
			result.setMaskType(dataMaskInfo.getDataMaskType());

			if (maskedValueExprResolver != null) {
				result.setMaskedValue(maskedValueExprResolver.resolveExpressions(result.getAccessRequest()));
			} else {
				result.setMaskedValue(dataMaskInfo.getValueExpr());
			}

			if (maskConditionExprResolver != null) {
				result.setMaskCondition(maskConditionExprResolver.resolveExpressions(result.getAccessRequest()));
			} else {
				result.setMaskCondition(dataMaskInfo.getConditionExpr());
			}

			result.setIsAccessDetermined(true);
			result.setPolicyPriority(policyEvaluator.getPolicyPriority());
			result.setPolicyId(policyEvaluator.getPolicyId());
			result.setReason(getComments());
			result.setPolicyVersion(policyEvaluator.getPolicy().getVersion());

			policyEvaluator.updateAccessResult(result, matchType, true, getComments());
		}
	}

}
