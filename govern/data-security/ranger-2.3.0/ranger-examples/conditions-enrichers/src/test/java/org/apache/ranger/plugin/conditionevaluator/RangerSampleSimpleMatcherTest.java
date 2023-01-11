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

package org.apache.ranger.plugin.conditionevaluator;


import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

public class RangerSampleSimpleMatcherTest {

	final Map<String, String> _conditionOptions = new HashMap<String, String>();

	{
		_conditionOptions.put(RangerSampleSimpleMatcher.CONTEXT_NAME, RangerSampleSimpleMatcher.CONTEXT_NAME);
	}

	@Test
	public void testIsMatched_happyPath() {
		// this documents some unexpected behavior of the ip matcher
		RangerSampleSimpleMatcher ipMatcher = createMatcher(new String[]{"US", "C*"} );
		Assert.assertTrue(ipMatcher.isMatched(createRequest("US")));
		Assert.assertTrue(ipMatcher.isMatched(createRequest("CA")));
		Assert.assertTrue(ipMatcher.isMatched(createRequest("C---")));
		Assert.assertFalse(ipMatcher.isMatched(createRequest(" US ")));
		Assert.assertFalse(ipMatcher.isMatched(createRequest("Us")));
		Assert.assertFalse(ipMatcher.isMatched(createRequest("ca")));
	}
	
	@Test
	public void test_firewallings() {
		
		// create a request for some policyValue, say, country and use it to match against matcher initialized with all sorts of bad data
		RangerAccessRequest request = createRequest("AB");

		RangerSampleSimpleMatcher matcher = new RangerSampleSimpleMatcher();
		// Matcher initialized with null policy should behave sensibly!  It matches everything!
		matcher.setConditionDef(null);
		matcher.setPolicyItemCondition(null);
		matcher.init();
		Assert.assertTrue(matcher.isMatched(request));
		
		RangerPolicyItemCondition policyItemCondition = Mockito.mock(RangerPolicyItemCondition.class);
		matcher.setConditionDef(null);
		matcher.setPolicyItemCondition(policyItemCondition);
		matcher.init();
		Assert.assertTrue(matcher.isMatched(request));
		
		RangerPolicyConditionDef conditionDef = Mockito.mock(RangerPolicyConditionDef.class);
		matcher.setConditionDef(conditionDef);
		matcher.setPolicyItemCondition(null);
		matcher.init();
		Assert.assertTrue(matcher.isMatched(request));
		
		// so should a policy item condition with initialized with null list of values
		Mockito.when(policyItemCondition.getValues()).thenReturn(null);
		matcher.setConditionDef(conditionDef);
		matcher.setPolicyItemCondition(policyItemCondition);
		matcher.init();
		Assert.assertTrue(matcher.isMatched(request));

		// not null item condition with empty condition list
		List<String> values = new ArrayList<String>();
		Mockito.when(policyItemCondition.getValues()).thenReturn(values);
		matcher.setConditionDef(conditionDef);
		matcher.setPolicyItemCondition(policyItemCondition);
		matcher.init();
		Assert.assertTrue(matcher.isMatched(request));

		// values as sensible items in it, however, the conditionDef has null evaluator option, so that too suppresses any check
		values.add("AB");
		Mockito.when(policyItemCondition.getValues()).thenReturn(values);
		Mockito.when(conditionDef.getEvaluatorOptions()).thenReturn(null);
		matcher.setConditionDef(conditionDef);
		matcher.setPolicyItemCondition(policyItemCondition);
		matcher.init();
		Assert.assertTrue(matcher.isMatched(request));

		// If evaluator option on the condition def is non-null then it starts to evaluate for real
		Mockito.when(conditionDef.getEvaluatorOptions()).thenReturn(_conditionOptions);
		matcher.setConditionDef(conditionDef);
		matcher.setPolicyItemCondition(policyItemCondition);
		matcher.init();
		Assert.assertTrue(matcher.isMatched(request));
	}
	
	RangerSampleSimpleMatcher createMatcher(String[] ipArray) {
		RangerSampleSimpleMatcher matcher = new RangerSampleSimpleMatcher();

		if (ipArray == null) {
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(null);
			matcher.init();
		} else {
			RangerPolicyItemCondition condition = Mockito.mock(RangerPolicyItemCondition.class);
			List<String> addresses = Arrays.asList(ipArray);
			Mockito.when(condition.getValues()).thenReturn(addresses);
			
			RangerPolicyConditionDef conditionDef = Mockito.mock(RangerPolicyConditionDef.class);

			Mockito.when(conditionDef.getEvaluatorOptions()).thenReturn(_conditionOptions);
			matcher.setConditionDef(conditionDef);
			matcher.setPolicyItemCondition(condition);
			matcher.init();
		}
		
		return matcher;
	}
	
	RangerAccessRequest createRequest(String value) {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put(RangerSampleSimpleMatcher.CONTEXT_NAME, value);
		RangerAccessRequest request = Mockito.mock(RangerAccessRequest.class);
		Mockito.when(request.getContext()).thenReturn(context);
		return request;
	}
}
