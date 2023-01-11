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

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemRowFilterInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerRequestExprResolver;


public class RangerDefaultRowFilterPolicyItemEvaluator extends RangerDefaultPolicyItemEvaluator implements RangerRowFilterPolicyItemEvaluator {
	final private RangerRowFilterPolicyItem rowFilterPolicyItem;
	final private String                    rowFilterExpr;
	final private RangerRequestExprResolver exprResolver;

	public RangerDefaultRowFilterPolicyItemEvaluator(RangerServiceDef serviceDef, RangerPolicy policy, RangerRowFilterPolicyItem policyItem, int policyItemIndex, RangerPolicyEngineOptions options) {
		super(serviceDef, policy, policyItem, RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DATAMASK, policyItemIndex, options);

		rowFilterPolicyItem = policyItem;

		RangerPolicyItemRowFilterInfo rowFilterInfo = getRowFilterInfo();

		if (rowFilterInfo != null && rowFilterInfo.getFilterExpr() != null) {
			rowFilterExpr = rowFilterInfo.getFilterExpr();
		} else {
			rowFilterExpr = null;
		}

		if (rowFilterExpr != null && RangerRequestExprResolver.hasExpressions(rowFilterExpr)) {
			exprResolver = new RangerRequestExprResolver(rowFilterExpr, getServiceType());
		} else {
			exprResolver = null;
		}
	}

	@Override
	public RangerPolicyItemRowFilterInfo getRowFilterInfo() {
		return rowFilterPolicyItem == null ? null : rowFilterPolicyItem.getRowFilterInfo();
	}

	@Override
	public void updateAccessResult(RangerPolicyEvaluator policyEvaluator, RangerAccessResult result, RangerPolicyResourceMatcher.MatchType matchType) {
		if (result.getFilterExpr() == null) {
			if (exprResolver != null) {
				result.setFilterExpr(exprResolver.resolveExpressions(result.getAccessRequest()));
			} else if (rowFilterExpr != null) {
				result.setFilterExpr(rowFilterExpr);
			}

			if (result.getFilterExpr() != null) {
				policyEvaluator.updateAccessResult(result, matchType, true, getComments());
				result.setIsAccessDetermined(true);
			}
		}
	}
}
