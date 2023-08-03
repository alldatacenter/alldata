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


import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.apache.ranger.plugin.util.RangerCommonConstants.SCRIPT_OPTION_ENABLE_JSON_CTX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RangerCustomConditionMatcherTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testScriptConditionEvaluator() {
		RangerAccessRequest request = createRequest(Arrays.asList("PCI", "PII"));

		RangerScriptConditionEvaluator resourceDbCondition     = createScriptConditionEvaluator("_ctx.request.resource.database == 'db1'");
		RangerScriptConditionEvaluator resourceDbCondition2    = createScriptConditionEvaluator("_ctx.request.resource.database != 'db2'");
		RangerScriptConditionEvaluator resourceTblCondition    = createScriptConditionEvaluator("_ctx.request.resource.table == 'tbl1'");
		RangerScriptConditionEvaluator resourceColCondition    = createScriptConditionEvaluator("_ctx.request.resource.column == 'col1'");
		RangerScriptConditionEvaluator accessTypeCondition     = createScriptConditionEvaluator("_ctx.request.accessType == 'select'");
		RangerScriptConditionEvaluator actionCondition         = createScriptConditionEvaluator("_ctx.request.action == 'query'");
		RangerScriptConditionEvaluator userCondition           = createScriptConditionEvaluator("_ctx.request.user == 'test-user'");
		RangerScriptConditionEvaluator userGroupsLenCondition  = createScriptConditionEvaluator("_ctx.request.userGroups.length == 2");
		RangerScriptConditionEvaluator userGroupsHas1Condition = createScriptConditionEvaluator("_ctx.request.userGroups.indexOf('test-group1') != -1");
		RangerScriptConditionEvaluator userGroupsHas2Condition = createScriptConditionEvaluator("_ctx.request.userGroups.indexOf('test-group2') != -1");
		RangerScriptConditionEvaluator userRolesLenCondition   = createScriptConditionEvaluator("_ctx.request.userRoles.length == 2");
		RangerScriptConditionEvaluator userRolesHas1Condition  = createScriptConditionEvaluator("_ctx.request.userRoles.indexOf('test-role1') != -1");
		RangerScriptConditionEvaluator userRolesHas2Condition  = createScriptConditionEvaluator("_ctx.request.userRoles.indexOf('test-role2') != -1");
		RangerScriptConditionEvaluator userAttrLenCondition    = createScriptConditionEvaluator("Object.keys(_ctx.request.userAttributes).length == 3");
		RangerScriptConditionEvaluator userAttr1Condition      = createScriptConditionEvaluator("_ctx.request.userAttributes['attr1'] == 'test-user-value1'");
		RangerScriptConditionEvaluator userAttr2Condition      = createScriptConditionEvaluator("_ctx.request.userAttributes['attr2'] == 'test-user-value2'");
		RangerScriptConditionEvaluator userGroup1Attr1Condition = createScriptConditionEvaluator("_ctx.request.userGroupAttributes['test-group1']['attr1'] == 'test-group1-value1'");
		RangerScriptConditionEvaluator userGroup1Attr2Condition = createScriptConditionEvaluator("_ctx.request.userGroupAttributes['test-group1']['attr2'] == 'test-group1-value2'");
		RangerScriptConditionEvaluator userGroup2Attr1Condition = createScriptConditionEvaluator("_ctx.request.userGroupAttributes['test-group2']['attr1'] == 'test-group2-value1'");
		RangerScriptConditionEvaluator userGroup2Attr2Condition = createScriptConditionEvaluator("_ctx.request.userGroupAttributes['test-group2']['attr2'] == 'test-group2-value2'");
		RangerScriptConditionEvaluator tagsLengthCondition     = createScriptConditionEvaluator("Object.keys(_ctx.tags).length == 2");
		RangerScriptConditionEvaluator tagTypeCondition        = createScriptConditionEvaluator("_ctx.tag._type == 'PCI'");
		RangerScriptConditionEvaluator tagAttributesCondition  = createScriptConditionEvaluator("_ctx.tag.attr1 == 'PCI_value'");
		RangerScriptConditionEvaluator tagsTypeCondition       = createScriptConditionEvaluator("_ctx.tags['PII']._type == 'PII' && _ctx.tags['PCI']._type == 'PCI'");
		RangerScriptConditionEvaluator tagsAttributesCondition = createScriptConditionEvaluator("_ctx.tags['PII'].attr1 == 'PII_value' && _ctx.tags['PCI'].attr1 == 'PCI_value'");

		Assert.assertTrue("request.resource.database should be db1", resourceDbCondition.isMatched(request));
		Assert.assertTrue("request.resource.database should not be db2", resourceDbCondition2.isMatched(request));
		Assert.assertTrue("request.resource.table should be tbl1", resourceTblCondition.isMatched(request));
		Assert.assertTrue("request.resource.column should be col1", resourceColCondition.isMatched(request));
		Assert.assertTrue("request.accessType should be select", accessTypeCondition.isMatched(request));
		Assert.assertTrue("request.action should be query", actionCondition.isMatched(request));
		Assert.assertTrue("request.user should be testUser", userCondition.isMatched(request));
		Assert.assertTrue("request.userGroups should have 2 entries", userGroupsLenCondition.isMatched(request));
		Assert.assertTrue("request.userGroups should have test-group1", userGroupsHas1Condition.isMatched(request));
		Assert.assertTrue("request.userGroups should have test-group2", userGroupsHas2Condition.isMatched(request));
		Assert.assertTrue("request.userRoles should have 2 entries", userRolesLenCondition.isMatched(request));
		Assert.assertTrue("request.userRoles should have test-role1", userRolesHas1Condition.isMatched(request));
		Assert.assertTrue("request.userRoles should have test-role2", userRolesHas2Condition.isMatched(request));
		Assert.assertTrue("request.userAttributes should have 3 entries", userAttrLenCondition.isMatched(request));
		Assert.assertTrue("request.userAttributes[attr1] should be test-user-value1", userAttr1Condition.isMatched(request));
		Assert.assertTrue("request.userAttributes[attr2] should be test-user-value2", userAttr2Condition.isMatched(request));
		Assert.assertTrue("request.userGroup1Attributes[attr1] should be test-group1-value1", userGroup1Attr1Condition.isMatched(request));
		Assert.assertTrue("request.userGroup1Attributes[attr2] should be test-group1-value2", userGroup1Attr2Condition.isMatched(request));
		Assert.assertTrue("request.userGroup2Attributes[attr1] should be test-group2-value1", userGroup2Attr1Condition.isMatched(request));
		Assert.assertTrue("request.userGroup2Attributes[attr2] should be test-group2-value2", userGroup2Attr2Condition.isMatched(request));
		Assert.assertTrue("tag._type should be PCI", tagTypeCondition.isMatched(request));
		Assert.assertTrue("tag.attr1 should be PCI_value", tagAttributesCondition.isMatched(request));
		Assert.assertTrue("should have 2 tags", tagsLengthCondition.isMatched(request));
		Assert.assertTrue("tags PCI and PII should be found", tagsTypeCondition.isMatched(request));
		Assert.assertTrue("tag attributes for PCI and PII should be found", tagsAttributesCondition.isMatched(request));
	}

	@Test
	public void testRangerAnyOfExpectedTagsPresentConditionEvaluator() {
		List<String> policyConditionTags = Arrays.asList("PCI", "PII");
		RangerAnyOfExpectedTagsPresentConditionEvaluator tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);

		// When any tag in the resourceTags matches policyConditionTags it should return TRUE
		List<String> resourceTags = Arrays.asList("PCI", "PHI");
		Assert.assertTrue(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));
		resourceTags = Arrays.asList("PHI", "PII" ,"HIPPA");
		Assert.assertTrue(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When no Tag Matches between resourceTags and PolicyConditionTags it should return FALSE
		resourceTags = Arrays.asList("HIPPA", "PHI");
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags and resourceTags contains empty set it should return TRUE as empty set matches.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("");
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags empty it should return FALSE as there is no any match.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = Arrays.asList("");
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is empty and resourceTags in not empty it should return FALSE as there is no any match.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("PCI", "PII");
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags is NULL it should return FALSE as there is no any match.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = null;
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));
	}


	@Test
	public void testRangerTagsNotPresentConditionEvaluator() {

		List<String> policyConditionTags = Arrays.asList("PCI", "PII");
		RangerNoneOfExpectedTagsPresentConditionEvaluator tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);

		// When no Tag Matches between resourceTags and PolicyConditionTags it should return TRUE
		List<String> resourceTags = Arrays.asList("HIPPA", "PHI");
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When any Tag Matches between resourceTags and PolicyConditionTags it should return FALSE
		resourceTags = Arrays.asList("HIPPA", "PII", "");
		Assert.assertFalse(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags and resourceTags both are empty is should return FALSE as both matches.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags empty it should return TRUE as there is no tag match between these two sets.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = Arrays.asList("");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is  empty and resourceTags in not empty it should return TRUE as there is no tag match between these two sets.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("PCI", "PII");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags is NULL it should return TRUE as there is no tag match between these two sets.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = Arrays.asList("");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));
	}

	RangerAnyOfExpectedTagsPresentConditionEvaluator createRangerAnyOfExpectedTagsPresentConditionEvaluator(List<String> policyConditionTags) {
		RangerAnyOfExpectedTagsPresentConditionEvaluator matcher = new RangerAnyOfExpectedTagsPresentConditionEvaluator();

		if (policyConditionTags == null) {
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(null);
		} else {
			RangerPolicyItemCondition condition = mock(RangerPolicyItemCondition.class);
			when(condition.getValues()).thenReturn(policyConditionTags);
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(condition);
		}

		matcher.init();

		return matcher;
	}

	RangerNoneOfExpectedTagsPresentConditionEvaluator createRangerTagsNotPresentConditionEvaluator(List<String> policyConditionTags) {
		RangerNoneOfExpectedTagsPresentConditionEvaluator matcher = new RangerNoneOfExpectedTagsPresentConditionEvaluator();

		if (policyConditionTags == null) {
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(null);
		} else {
			RangerPolicyItemCondition condition = mock(RangerPolicyItemCondition.class);
			when(condition.getValues()).thenReturn(policyConditionTags);
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(condition);
		}

		matcher.init();

		return matcher;
	}

	RangerScriptConditionEvaluator createScriptConditionEvaluator(String script) {
		RangerScriptConditionEvaluator ret = new RangerScriptConditionEvaluator();

		RangerServiceDef          serviceDef   = mock(RangerServiceDef.class);
		RangerPolicyConditionDef  conditionDef = mock(RangerPolicyConditionDef.class);
		RangerPolicyItemCondition condition    = mock(RangerPolicyItemCondition.class);

		when(serviceDef.getName()).thenReturn("test");
		when(conditionDef.getEvaluatorOptions()).thenReturn(Collections.singletonMap(SCRIPT_OPTION_ENABLE_JSON_CTX, "true"));
		when(condition.getValues()).thenReturn(Arrays.asList(script));

		ret.setServiceDef(serviceDef);
		ret.setConditionDef(conditionDef);
		ret.setPolicyItemCondition(condition);

		ret.init();

		return ret;
	}

	RangerAccessRequest createRequest(List<String> resourceTags) {
		RangerAccessResource resource = mock(RangerAccessResource.class);

		Map<String, Object> resourceMap = new HashMap<>();

		resourceMap.put("database", "db1");
		resourceMap.put("table", "tbl1");
		resourceMap.put("column", "col1");

		when(resource.getAsString()).thenReturn("db1/tbl1/col1");
		when(resource.getOwnerUser()).thenReturn("testUser");
		when(resource.getAsMap()).thenReturn(resourceMap);
		when(resource.getReadOnlyCopy()).thenReturn(resource);

		RangerAccessRequestImpl request = new RangerAccessRequestImpl();

		request.setResource(resource);
		request.setResourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF);
		request.setAccessType("select");
		request.setAction("query");
		request.setUser("test-user");
		request.setUserGroups(new HashSet<>(Arrays.asList("test-group1", "test-group2")));
		request.setUserRoles(new HashSet<>(Arrays.asList("test-role1", "test-role2")));

		if (resourceTags != null) {
			Set<RangerTagForEval> rangerTagForEvals = new HashSet<>();
			RangerTagForEval      currentTag        = null;

			for (String resourceTag : resourceTags) {
				RangerTag        tag        = new RangerTag(UUID.randomUUID().toString(), resourceTag, Collections.singletonMap("attr1", resourceTag + "_value"), null, null, null);
				RangerTagForEval tagForEval = new RangerTagForEval(tag, RangerPolicyResourceMatcher.MatchType.SELF);

				rangerTagForEvals.add(tagForEval);

				if (currentTag == null) {
					currentTag = tagForEval;
				}
			}

			RangerAccessRequestUtil.setRequestTagsInContext(request.getContext(), rangerTagForEvals);
			RangerAccessRequestUtil.setCurrentTagInContext(request.getContext(), currentTag);
		}  else {
			RangerAccessRequestUtil.setRequestTagsInContext(request.getContext(), null);
		}

		Map<String, Map<String, String>> userAttrMapping  = new HashMap<>();
		Map<String, Map<String, String>> groupAttrMapping = new HashMap<>();
		Map<String, String>              testUserAttrs    = new HashMap<>();
		Map<String, String>              testGroup1Attrs  = new HashMap<>();
		Map<String, String>              testGroup2Attrs  = new HashMap<>();

		testUserAttrs.put("attr1", "test-user-value1");
		testUserAttrs.put("attr2", "test-user-value2");
		testGroup1Attrs.put("attr1", "test-group1-value1");
		testGroup1Attrs.put("attr2", "test-group1-value2");
		testGroup2Attrs.put("attr1", "test-group2-value1");
		testGroup2Attrs.put("attr2", "test-group2-value2");

		userAttrMapping.put("test-user", testUserAttrs);
		groupAttrMapping.put("test-group1", testGroup1Attrs);
		groupAttrMapping.put("test-group2", testGroup2Attrs);

		RangerUserStore userStore = mock(RangerUserStore.class);

		when(userStore.getUserAttrMapping()).thenReturn(userAttrMapping);
		when(userStore.getGroupAttrMapping()).thenReturn(groupAttrMapping);

		RangerAccessRequestUtil.setRequestUserStoreInContext(request.getContext(), userStore);

		return request;
	}
}
