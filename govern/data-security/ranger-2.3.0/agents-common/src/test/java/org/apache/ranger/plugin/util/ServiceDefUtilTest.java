/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.plugin.util;

import org.apache.ranger.plugin.contextenricher.RangerAdminUserStoreRetriever;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemDataMaskInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemRowFilterInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.ServicePolicies.SecurityZoneInfo;
import org.junit.Test;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceDefUtilTest {
	private static final String REF_USER                    = "USER.dept";
	private static final String REF_UG                      = "UG['test-group1'].dept";
	private static final String REF_UGA                     = "UGA.sVal['dept']";
	private static final String REF_GET_UG_ATTR_CSV         = "GET_UG_ATTR_CSV('dept')";
	private static final String REF_GET_UG_ATTR_Q_CSV       = "GET_UG_ATTR_Q_CSV('dept')";
	private static final String REF_UG_ATTR_NAMES_CSV       = "UG_ATTR_NAMES_CSV";
	private static final String REF_UG_ATTR_NAMES_Q_CSV     = "UG_ATTR_NAMES_Q_CSV";
	private static final String REF_USER_ATTR_NAMES_CSV     = "USER_ATTR_NAMES_CSV";
	private static final String REF_USER_ATTR_NAMES_Q_CSV   = "USER_ATTR_NAMES_Q_CSV";
	private static final String REF_GET_UG_ATTR_CSV_F       = "ctx.ugAttrCsv('dept')";
	private static final String REF_GET_UG_ATTR_Q_CSV_F     = "ctx.ugAttrCsvQ('dept')";
	private static final String REF_UG_ATTR_NAMES_CSV_F     = "ctx.ugAttrNamesCsv()";
	private static final String REF_UG_ATTR_NAMES_Q_CSV_F   = "ctx.ugAttrNamesCsvQ()";
	private static final String REF_USER_ATTR_NAMES_CSV_F   = "ctx.userAttrNamesCsv()";
	private static final String REF_USER_ATTR_NAMES_Q_CSV_F = "ctx.userAttrNamesCsvQ()";

	private static final String[] UGA_ATTR_EXPRESSIONS = new String[] {
			REF_USER, REF_UG, REF_UGA,
			REF_GET_UG_ATTR_CSV, REF_GET_UG_ATTR_Q_CSV,
			REF_UG_ATTR_NAMES_CSV, REF_UG_ATTR_NAMES_Q_CSV,
			REF_USER_ATTR_NAMES_CSV, REF_USER_ATTR_NAMES_Q_CSV,
			REF_GET_UG_ATTR_CSV_F, REF_GET_UG_ATTR_Q_CSV_F,
			REF_UG_ATTR_NAMES_CSV_F, REF_UG_ATTR_NAMES_Q_CSV_F,
			REF_USER_ATTR_NAMES_CSV_F, REF_USER_ATTR_NAMES_Q_CSV_F
	};

	@Test
	public void testNoUserGroupAttrRef() {
		ServicePolicies svcPolicies = getServicePolicies();
		RangerPolicy    policy      = getPolicy(svcPolicies);

		svcPolicies.getPolicies().add(policy);
		assertFalse("policy doesn't have any reference to user/group attribute", ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

		policy.getResources().put("database", new RangerPolicyResource("/departments/USER.dept/")); // expressions must be within ${{}}
		assertFalse("policy doesn't have any reference to user/group attribute", ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

		policy.getRowFilterPolicyItems().get(0).getRowFilterInfo().setFilterExpr("dept in USER.dept"); // expressions must be within ${{}}
		assertFalse("policy doesn't have any reference to user/group attribute", ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));
	}

	@Test
	public void testResourceUserGroupAttrRef() {
		for (String attrExpr : UGA_ATTR_EXPRESSIONS) {
			String          resource    = "test_" + "${{" + attrExpr + "}}";
			ServicePolicies svcPolicies = getServicePolicies();
			RangerPolicy    policy      = getPolicy(svcPolicies);

			policy.getResources().put("database", new RangerPolicyResource(resource));

			svcPolicies.getPolicies().add(policy);
			assertTrue("policy resource refers to user/group attribute: " + resource, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicies().clear();
			svcPolicies.getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("policy-delta resource refers to user/group attribute: " + resource, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicyDeltas().clear();
			svcPolicies.getSecurityZones().put("zone1", getSecurityZoneInfo("zone1"));
			svcPolicies.getSecurityZones().get("zone1").getPolicies().add(policy);
			assertTrue("zone-policy resource refers to user/group attribute: " + resource, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicies().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("zone-policy-delta resource refers to user/group attribute: " + resource, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));
		}
	}

	@Test
	public void testPolicyConditionUserGroupAttrRef() {
		for (String attrExpr : UGA_ATTR_EXPRESSIONS) {
			String          condExpr    = attrExpr + " != null";
			ServicePolicies svcPolicies = getServicePolicies();
			RangerPolicy    policy      = getPolicy(svcPolicies);

			policy.getConditions().add(new RangerPolicyItemCondition("expr", Collections.singletonList(condExpr)));

			svcPolicies.getPolicies().add(policy);
			assertTrue("policy condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicies().clear();
			svcPolicies.getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("policy-delta condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicyDeltas().clear();
			svcPolicies.getSecurityZones().put("zone1", getSecurityZoneInfo("zone1"));
			svcPolicies.getSecurityZones().get("zone1").getPolicies().add(policy);
			assertTrue("zone-policy condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicies().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("zone-policy-delta condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicies().clear();
			svcPolicies.getPolicyDeltas().clear();
			svcPolicies.getSecurityZones().clear();
			svcPolicies.getTagPolicies().getPolicies().add(policy);
			assertTrue("tag-policy condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));
		}
	}

	@Test
	public void testPolicyItemConditionUserGroupRef() {
		int i = 0;

		for (String attrExpr : UGA_ATTR_EXPRESSIONS) {
			String          condExpr    = attrExpr + " != null";
			ServicePolicies svcPolicies = getServicePolicies();
			RangerPolicy    policy      = getPolicy(svcPolicies);

			final List<? extends RangerPolicyItem> policyItems;

			switch (i % 6) {
				case 0:
					policyItems = policy.getPolicyItems();
				break;

				case 1:
					policyItems = policy.getDenyPolicyItems();
				break;

				case 2:
					policyItems = policy.getAllowExceptions();
				break;

				case 3:
					policyItems = policy.getDenyExceptions();
				break;

				case 4:
					policyItems = policy.getRowFilterPolicyItems();
				break;

				case 5:
					policyItems = policy.getDataMaskPolicyItems();
				break;

				default:
					policyItems = policy.getPolicyItems();
				break;
			}

			policyItems.get(0).getConditions().add(new RangerPolicyItemCondition("expr", Collections.singletonList(condExpr)));

			svcPolicies.getPolicies().add(policy);
			assertTrue("policyItem condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicies().clear();
			svcPolicies.getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("policy-delta-item condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicyDeltas().clear();
			svcPolicies.getSecurityZones().put("zone1", getSecurityZoneInfo("zone1"));
			svcPolicies.getSecurityZones().get("zone1").getPolicies().add(policy);
			assertTrue("zone-policy-item condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicies().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("zone-policy-delta-item condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicies().clear();
			svcPolicies.getTagPolicies().getPolicies().add(policy);

			assertTrue("tag-policyItem condition refers to user/group attribute: " + condExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			i++;
		}
	}

	@Test
	public void testPolicyItemRowFilterExprUserGroupRef() {
		for (String attrExpr : UGA_ATTR_EXPRESSIONS) {
			String          filterExpr  = "${{" + attrExpr + "}}";
			ServicePolicies svcPolicies = getServicePolicies();
			RangerPolicy    policy      = getPolicy(svcPolicies);

			policy.getRowFilterPolicyItems().get(0).setRowFilterInfo(new RangerPolicyItemRowFilterInfo("dept in (" + filterExpr + ")"));

			svcPolicies.getPolicies().add(policy);
			assertTrue("policy row-filter refers to user/group attribute: " + filterExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicies().clear();
			svcPolicies.getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("policy-delta row-filter refers to user/group attribute: " + filterExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getPolicyDeltas().clear();
			svcPolicies.getSecurityZones().put("zone1", getSecurityZoneInfo("zone1"));
			svcPolicies.getSecurityZones().get("zone1").getPolicies().add(policy);
			assertTrue("zone-policy row-filter refers to user/group attribute: " + filterExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));

			svcPolicies.getServiceDef().getContextEnrichers().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicies().clear();
			svcPolicies.getSecurityZones().get("zone1").getPolicyDeltas().add(new RangerPolicyDelta(1L, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, 1L, policy));
			assertTrue("zone-policy-delta row-filter refers to user/group attribute: " + filterExpr, ServiceDefUtil.addUserStoreEnricherIfNeeded(svcPolicies, RangerAdminUserStoreRetriever.class.getCanonicalName(), "60000"));
		}
	}


	private ServicePolicies getServicePolicies() {
		ServicePolicies ret = new ServicePolicies();

		ret.setServiceName("dev_hive");
		ret.setServiceId(1L);
		ret.setPolicyVersion(1L);
		ret.setServiceDef(getServiceDef("hive"));
		ret.setPolicies(new ArrayList<>());
		ret.setPolicyDeltas(new ArrayList<>());
		ret.setSecurityZones(new HashMap<>());

		ret.setTagPolicies(new ServicePolicies.TagPolicies());
		ret.getTagPolicies().setServiceDef(getServiceDef("tag"));
		ret.getTagPolicies().setPolicies(new ArrayList<>());

		return ret;
	}

	private RangerServiceDef getServiceDef(String serviceType) {
		RangerServiceDef ret = null;

		try {
			RangerServiceDef serviceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(serviceType);

			if (serviceDef != null) { // make a copy
				ret = new RangerServiceDef();

				ret.updateFrom(serviceDef);
			}
		} catch (Exception excp) {
			// ignore
		}

		return ret;
	}

	private RangerPolicy getPolicy(ServicePolicies svcPolicies) {
		RangerPolicy ret = new RangerPolicy();

		ret.setConditions(new ArrayList<>());

		ret.setService(svcPolicies.getServiceName());
		ret.setServiceType(svcPolicies.getServiceDef().getName());
		ret.getResources().put("database", new RangerPolicyResource("testdb"));
		ret.getConditions().add(new RangerPolicyItemCondition("expr", Collections.singletonList("TAG.attr1 == 'value1'")));
		ret.getPolicyItems().add(getPolicyItem());
		ret.getAllowExceptions().add(getPolicyItem());
		ret.getDenyPolicyItems().add(getPolicyItem());
		ret.getDenyExceptions().add(getPolicyItem());
		ret.getDataMaskPolicyItems().add(getDataMaskPolicyItem());
		ret.getRowFilterPolicyItems().add(getRowFilterPolicyItem());

		return ret;
	}

	private RangerPolicyItem getPolicyItem() {
		RangerPolicyItem ret = new RangerPolicyItem();

		ret.getUsers().add("testUser");
		ret.getGroups().add("testGroup");
		ret.getRoles().add("testRole");
		ret.getConditions().add(new RangerPolicyItemCondition("expr", Collections.singletonList("TAG.attr1 == 'value1'")));

		return ret;
	}

	private RangerDataMaskPolicyItem getDataMaskPolicyItem() {
		RangerDataMaskPolicyItem ret = new RangerDataMaskPolicyItem();

		ret.getUsers().add("testUser");
		ret.getGroups().add("testGroup");
		ret.getRoles().add("testRole");
		ret.getConditions().add(new RangerPolicyItemCondition("expr", Collections.singletonList("TAG.attr1 == 'value1'")));
		ret.setDataMaskInfo(new RangerPolicyItemDataMaskInfo("MASK_NULL", null, null));

		return ret;
	}

	private RangerRowFilterPolicyItem getRowFilterPolicyItem() {
		RangerRowFilterPolicyItem ret = new RangerRowFilterPolicyItem();

		ret.getUsers().add("testUser");
		ret.getGroups().add("testGroup");
		ret.getRoles().add("testRole");
		ret.getConditions().add(new RangerPolicyItemCondition("expr", Collections.singletonList("TAG.attr1 == 'value1'")));
		ret.setRowFilterInfo(new RangerPolicyItemRowFilterInfo("dept in ('dept1','dept2')"));

		return ret;
	}

	private SecurityZoneInfo getSecurityZoneInfo(String zoneName) {
		SecurityZoneInfo ret = new SecurityZoneInfo();

		ret.setZoneName(zoneName);
		ret.setPolicies(new ArrayList<>());
		ret.setPolicyDeltas(new ArrayList<>());

		return ret;
	}
}
