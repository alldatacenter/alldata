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

import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXTagDef;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRangerTagDefServiceBase {
	Long id = 1L;
	String guid = "989898_01_1";
	String name = "test";
	Long version = 5L;
	String type = "typo";
	Long totalCount = 50L;

	@InjectMocks
	RangerTagDefService rangerTagDefService = new RangerTagDefService();
	
	@Mock
	RangerDaoManager daoMgr;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void test1mapViewToEntityBean() {
		RangerTagDef rangerTagDef = new RangerTagDef();
		rangerTagDef.setId(id);
		rangerTagDef.setGuid(guid);
		
		XXTagDef xxTagDef = new XXTagDef();
		xxTagDef.setId(id);
		xxTagDef.setGuid(guid);
		xxTagDef.setName(name);
		xxTagDef.setVersion(version);
		int operationContext = 1;

		XXTagDef result = rangerTagDefService.mapViewToEntityBean(rangerTagDef,xxTagDef,operationContext);
		Assert.assertNotNull(result);
		Assert.assertEquals(result, xxTagDef);
		Assert.assertEquals(result.getGuid(), xxTagDef.getGuid());
		Assert.assertEquals(result.getName(), xxTagDef.getName());
		Assert.assertEquals(result.getId(), xxTagDef.getId());
		Assert.assertEquals(result.getVersion(), xxTagDef.getVersion());
	}
} 
