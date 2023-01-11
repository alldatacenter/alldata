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

package org.apache.ranger.service;

import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.view.VXModuleDef;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.entity.XXGroup;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestXGroupPermissionService {
	@Mock
	XGroupPermissionService xGroupPermissionService;

	@Mock
	XXGroupPermission xXGroupPermission;

	@Mock
	XXGroup XXGroup;

	@Mock
	VXModuleDef vXModuleDef;

	@Test
	public void test1PopulateViewBean() {
		xGroupPermissionService.populateViewBean(xXGroupPermission);

	}

	@Test
	public void test2GetPopulatedVXGroupPermissionList() {
		List<XXGroupPermission> xgroupPermissionList = new ArrayList<XXGroupPermission>();
		Map<Long, String> xXGroupNameMap = new HashMap<Long, String>();
		xXGroupNameMap.put(XXGroup.getId(), XXGroup.getName());
		xGroupPermissionService.getPopulatedVXGroupPermissionList(xgroupPermissionList, xXGroupNameMap, vXModuleDef);

	}
}
