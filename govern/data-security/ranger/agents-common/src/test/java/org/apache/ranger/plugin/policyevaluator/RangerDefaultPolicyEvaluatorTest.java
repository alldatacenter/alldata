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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.junit.After;
import org.junit.Before;


public class RangerDefaultPolicyEvaluatorTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	RangerPolicyItem getMockPolicyItem(String[] strings) {
		RangerPolicyItem policyItem = mock(RangerPolicyItem.class);
		if (strings == null) {
			when(policyItem.getConditions()).thenReturn(null);
		} else if (strings.length == 0) {
			when(policyItem.getConditions()).thenReturn(new ArrayList<RangerPolicy.RangerPolicyItemCondition>());
		} else {
			List<RangerPolicyItemCondition> conditions = new ArrayList<RangerPolicy.RangerPolicyItemCondition>(strings.length);
			for (String name : strings) {
				RangerPolicyItemCondition aCondition = mock(RangerPolicyItemCondition.class);
				when(aCondition.getType()).thenReturn(name);
				when(aCondition.getValues()).thenReturn(null); // values aren't used/needed so set it to a predictable value
				conditions.add(aCondition);
			}
			when(policyItem.getConditions()).thenReturn(conditions);
		}
		return policyItem;
	}

	RangerServiceDef getMockServiceDef(List<RangerPolicyConditionDef> conditionDefs) {
		// create a service def
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		when(serviceDef.getPolicyConditions()).thenReturn(conditionDefs);
		return serviceDef;
	}
	
	RangerServiceDef getMockServiceDef(Map<String, String[]> pairs) {
		// create a service def
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		if (pairs == null) {
			return serviceDef;
		}
		List<RangerPolicyConditionDef> conditions = getMockPolicyConditionDefs(pairs);
		when(serviceDef.getPolicyConditions()).thenReturn(conditions);
		return serviceDef;
	}
	
	// takes in a map of condition name to a an two element array where 1st element is evaluator-class-name and second is evaluator-options if any
	List<RangerPolicyConditionDef> getMockPolicyConditionDefs(Map<String, String[]> pairs) {
		List<RangerPolicyConditionDef> conditions = new ArrayList<>();
		// null policy condition def collection should behave sensibly
		for (Map.Entry<String, String[]> anEntry : pairs.entrySet()) {
			RangerPolicyConditionDef aCondition = mock(RangerPolicyConditionDef.class);
			when(aCondition.getName()).thenReturn(anEntry.getKey());
			when(aCondition.getEvaluator()).thenReturn(anEntry.getValue()[0]);

			Map<String, String> evaluatorOptions = new HashMap<>();
			evaluatorOptions.put(anEntry.getValue()[1], anEntry.getValue()[1]);

			when(aCondition.getEvaluatorOptions()).thenReturn(evaluatorOptions);

			conditions.add(aCondition);
		}
		return conditions;
	}
	
	RangerPolicyConditionDef getMockPolicyConditionDef(String name, String evaluatorClassName, Map<String, String> evaluatorOptions) {
		// null policy condition def collection should behave sensibly
		RangerPolicyConditionDef aCondition = mock(RangerPolicyConditionDef.class);
		when(aCondition.getName()).thenReturn(name);
		when(aCondition.getEvaluator()).thenReturn(evaluatorClassName);
		when(aCondition.getEvaluatorOptions()).thenReturn(evaluatorOptions);
		return aCondition;
	}
	
	RangerPolicyItem createPolicyItemForConditions(String[] conditions) {

		List<RangerPolicyItemCondition> itemConditions = new ArrayList<RangerPolicy.RangerPolicyItemCondition>(conditions.length);
		for (String conditionName : conditions) {
			RangerPolicyItemCondition condition = mock(RangerPolicyItemCondition.class);
			when(condition.getType()).thenReturn(conditionName);
			itemConditions.add(condition);
		}

		RangerPolicyItem policyItem = mock(RangerPolicyItem.class);
		when(policyItem.getConditions()).thenReturn(itemConditions);
		
		return policyItem;
	}
	
	RangerAccessRequest createAccessRequestWithConditions(String[] conditionNames) {
		// let's first create a request with 2 different conditions
		Map<String, Object> context = new HashMap<String, Object>(conditionNames.length);
		for (String conditionName: conditionNames) {
			// value is not important for our test
			context.put(conditionName, conditionName + "-value");
		}
		RangerAccessRequest request = mock(RangerAccessRequest.class);
		when(request.getContext()).thenReturn(context);
		
		return request;
	}
}
