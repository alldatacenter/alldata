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

import java.util.Date;

import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.entity.XXAsset;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXAsset;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestXAssetService {

	@InjectMocks
	XAssetService xAssetService;

	@Mock
	VXAsset vXAsset;

	@Mock
	XXAsset xXAsset;

	@Mock
	RangerEnumUtil xaEnumUtil;

	@Mock
	JSONUtil jsonUtil;

	@Test
	public void test1ValidateConfig() {
		xAssetService.validateConfig(vXAsset);
	}

	@Test
	public void test2GetTransactionLog() {
		xAssetService.getTransactionLog(vXAsset, "update");

	}

	@Test
	public void test3GetTransactionLog() {
		VXAsset vXAsset = new VXAsset();
		XXAsset xXAsset = createXXAssetObject();
		xAssetService.getTransactionLog(vXAsset, xXAsset, "delete");

	}

	@Test
	public void test4GetConfigWithEncryptedPassword() {
		xAssetService.getConfigWithEncryptedPassword("testconfig", false);

	}

	@Test
	public void test5GetConfigWithDecryptedPassword() {
		xAssetService.getConfigWithDecryptedPassword("testConfig");

	}

	public XXAsset createXXAssetObject() {
		XXAsset xXAsset = new XXAsset();
		xXAsset.setId(1L);
		xXAsset.setAssetType(1);
		xXAsset.setName("testName");
		xXAsset.setConfig("testconfig");
		xXAsset.setActiveStatus(1);
		xXAsset.setAddedByUserId(1L);
		xXAsset.setSupportNative(true);
		xXAsset.setUpdatedByUserId(1L);
		Date date = new Date();
		xXAsset.setCreateTime(date);
		xXAsset.setUpdateTime(date);
		xXAsset.setDescription("this is test description");
		return xXAsset;
	}
}
