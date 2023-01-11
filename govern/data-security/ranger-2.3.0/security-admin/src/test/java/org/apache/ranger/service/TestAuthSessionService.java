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

import javax.persistence.EntityManager;

import org.apache.ranger.common.SearchCriteria;

import org.apache.ranger.common.SearchUtil;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXAuthSession;
import org.apache.ranger.view.VXAuthSession;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAuthSessionService {
	@InjectMocks
	AuthSessionService authSessionService = new AuthSessionService();

	@Mock
	VXAuthSession vXAuthSession;

	@Mock
	XXAuthSession mObj;

	@Mock
	XXAuthSession xXAuthSession;

	@Mock
	SearchCriteria searchCriteria;

	@Mock
	SearchUtil searchUtil;

	@Mock
	BaseDao entityDao;

	@Test
	public void test1GetResourceName() {
		authSessionService.getResourceName();
	}

	@Test
	public void test2CreateEntityObject() {
		authSessionService.createEntityObject();
	}

	@Test
	public void test3CreateViewObject() {
		authSessionService.createViewObject();
	}

	@Test
	public void test4search() {
		SearchCriteria testSearchCriteria = createsearchCriteria();
		EntityManager em = null;
		Mockito.when(authSessionService.getDao().getEntityManager()).thenReturn(em);
		authSessionService.search(testSearchCriteria);
	}

	private SearchCriteria createsearchCriteria() {
		SearchCriteria testsearchCriteria = new SearchCriteria();
		testsearchCriteria.setStartIndex(0);
		testsearchCriteria.setMaxRows(Integer.MAX_VALUE);
		testsearchCriteria.setSortBy("id");
		testsearchCriteria.setSortType("asc");
		testsearchCriteria.setGetCount(true);
		testsearchCriteria.setOwnerId(null);
		testsearchCriteria.setGetChildren(false);
		testsearchCriteria.setDistinct(false);
		return testsearchCriteria;
	}
}
