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
package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRangerPolicyService {

	private static Long Id = 8L;

	@InjectMocks
	RangerPolicyService policyService = new RangerPolicyService();

	@Mock
	RangerDaoManager daoManager;

	@Mock
	RangerServiceService svcService;

	@Mock
	JSONUtil jsonUtil;

	@Mock
	RangerServiceDefService serviceDefService;

	@Mock
	StringUtil stringUtil;

	@Mock
	XUserService xUserService;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public void setup() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil
				.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
	}

	private RangerPolicy rangerPolicy() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();
		List<String> users = new ArrayList<String>();
		List<String> groups = new ArrayList<String>();
		List<RangerPolicyItemCondition> conditions = new ArrayList<RangerPolicyItemCondition>();
		List<RangerPolicyItem> policyItems = new ArrayList<RangerPolicyItem>();
		RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
		rangerPolicyItem.setAccesses(accesses);
		rangerPolicyItem.setConditions(conditions);
		rangerPolicyItem.setGroups(groups);
		rangerPolicyItem.setUsers(users);
		rangerPolicyItem.setDelegateAdmin(false);

		policyItems.add(rangerPolicyItem);

		Map<String, RangerPolicyResource> policyResource = new HashMap<String, RangerPolicyResource>();
		RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
		rangerPolicyResource.setIsExcludes(true);
		rangerPolicyResource.setIsRecursive(true);
		rangerPolicyResource.setValue("1");
		rangerPolicyResource.setValues(users);
		RangerPolicy policy = new RangerPolicy();
		policy.setId(Id);
		policy.setCreateTime(new Date());
		policy.setDescription("policy");
		policy.setGuid("policyguid");
		policy.setIsEnabled(true);
		policy.setName("HDFS_1-1-20150316062453");
		policy.setUpdatedBy("Admin");
		policy.setUpdateTime(new Date());
		policy.setService("HDFS_1-1-20150316062453");
		policy.setIsAuditEnabled(true);
		policy.setPolicyItems(policyItems);
		policy.setResources(policyResource);
		policy.setPolicyType(0);

		return policy;
	}

	private XXPolicy policy() {
		XXPolicy xxPolicy = new XXPolicy();
		xxPolicy.setId(Id);
		xxPolicy.setName("HDFS_1-1-20150316062453");
		xxPolicy.setAddedByUserId(Id);
		xxPolicy.setCreateTime(new Date());
		xxPolicy.setDescription("test");
		xxPolicy.setIsAuditEnabled(false);
		xxPolicy.setIsEnabled(false);
		xxPolicy.setService(1L);
		xxPolicy.setUpdatedByUserId(Id);
		xxPolicy.setUpdateTime(new Date());
		return xxPolicy;
	}

	private XXService xService() {
		XXService xService = new XXService();
		xService.setAddedByUserId(Id);
		xService.setCreateTime(new Date());
		xService.setDescription("Hdfs service");
		xService.setGuid("serviceguid");
		xService.setId(Id);
		xService.setIsEnabled(true);
		xService.setName("Hdfs");
		xService.setPolicyUpdateTime(new Date());
		xService.setPolicyVersion(1L);
		xService.setType(1L);
		xService.setUpdatedByUserId(Id);
		xService.setUpdateTime(new Date());

		return xService;
	}

	@Test
	public void test1ValidateForCreate() {
		RangerPolicy rangerPolicy = rangerPolicy();
		policyService.validateForCreate(rangerPolicy);
		Assert.assertNotNull(rangerPolicy);
	}

	@Test
	public void test2ValidateForUpdate() {
		RangerPolicy rangerPolicy = rangerPolicy();
		XXPolicy policy = policy();
		policyService.validateForUpdate(rangerPolicy, policy);

		Assert.assertNotNull(rangerPolicy);
	}

	@Test
	public void test8getTransactionLog() {
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		RangerPolicy rangerPolicy = rangerPolicy();
		XXService xService = xService();

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByName(rangerPolicy.getService()))
				.thenReturn(xService);

		List<XXTrxLog> dbXXTrxLogList = policyService.getTransactionLog(
				rangerPolicy, 1);
		Assert.assertNotNull(dbXXTrxLogList);
	}
}
