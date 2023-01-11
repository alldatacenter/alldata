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
import javax.persistence.Query;

import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.common.view.VList;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.entity.XXAccessAudit;
import org.apache.ranger.view.VXAccessAuditList;
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
public class TestXAccessAuditService {

	@InjectMocks
	XAccessAuditService xAccessAuditService;

	@Mock
	SearchCriteria searchCriteria;

	@Mock
	SearchUtil searchUtil;

	@Mock
	SortField sortField;

	@Mock
	SearchField searchField;

	@Mock
	BaseDao entityDao;

	@Mock
	Query query;

	@Mock
	VList vList;

	@Mock
	XXAccessAudit xXAccessAudit;

	@Mock
	VXAccessAuditList vXAccessAuditList;

	@Mock
	EntityManager em;

	@Mock
	RangerDaoManager daoManager;

	@Mock
	XXServiceDao xXServiceDao;

	@Test
	public void test1SearchXAccessAudits() {
		SearchCriteria testSearchCriteria = createsearchCriteria();
		xAccessAuditService.searchXAccessAudits(testSearchCriteria);

	}

	@Test
	public void test2PopulateViewBean() {
		Mockito.when(daoManager.getXXService()).thenReturn(xXServiceDao);
		xAccessAuditService.populateViewBean(xXAccessAudit);

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
