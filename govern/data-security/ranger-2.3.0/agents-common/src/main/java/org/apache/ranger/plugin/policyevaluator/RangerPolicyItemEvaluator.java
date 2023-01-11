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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.ranger.plugin.conditionevaluator.RangerConditionEvaluator;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;

public interface RangerPolicyItemEvaluator {
	int POLICY_ITEM_TYPE_ALLOW            = 0;
	int POLICY_ITEM_TYPE_DENY             = 1;
	int POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS = 2;
	int POLICY_ITEM_TYPE_DENY_EXCEPTIONS  = 3;
	int POLICY_ITEM_TYPE_DATAMASK         = 4;
	int POLICY_ITEM_TYPE_ROWFILTER        = 5;

	void init();

	RangerPolicyItem getPolicyItem();

	int getPolicyItemType();

	int getPolicyItemIndex();

	String getComments();

	List<RangerConditionEvaluator> getConditionEvaluators();

	int getEvalOrder();

	boolean isMatch(RangerAccessRequest request);

	boolean matchUserGroupAndOwner(String user, Set<String> userGroups, Set<String> roles, String owner);

	boolean matchAccessType(String accessType);

	boolean matchCustomConditions(RangerAccessRequest request);

	class EvalOrderComparator implements Comparator<RangerPolicyItemEvaluator>, Serializable {
		@Override
		public int compare(RangerPolicyItemEvaluator me, RangerPolicyItemEvaluator other) {
			return Integer.compare(me.getEvalOrder(), other.getEvalOrder());
		}
	}
	void updateAccessResult(RangerPolicyEvaluator policyEvaluator, RangerAccessResult result, RangerPolicyResourceMatcher.MatchType matchType);
}
