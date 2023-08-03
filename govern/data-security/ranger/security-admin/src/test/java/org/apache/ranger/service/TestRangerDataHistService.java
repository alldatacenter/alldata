/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.apache.ranger.plugin.model.RangerBaseModelObject;
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
public class TestRangerDataHistService {

	@InjectMocks
	RangerDataHistService rangerDataHistService;

	@Mock
	RangerBaseModelObject baseModelObj;

	@Mock
	RangerDaoManager daoMgr;
	@Mock
	org.apache.ranger.db.XXDataHistDao XXDataHistDao;

	@Test
	public void test1CreateObjectDataHistory() {
		String action = "create";
		RangerBaseModelObject baseModelObj = new RangerBaseModelObject();
		Mockito.when(daoMgr.getXXDataHist()).thenReturn(XXDataHistDao);
		rangerDataHistService.createObjectDataHistory(baseModelObj, action);

	}
}
