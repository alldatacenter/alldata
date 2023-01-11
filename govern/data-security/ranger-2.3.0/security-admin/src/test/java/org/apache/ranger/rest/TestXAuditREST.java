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
package org.apache.ranger.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.ranger.biz.XAuditMgr;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.SortField;
import org.apache.ranger.service.XAccessAuditService;
import org.apache.ranger.service.XTrxLogService;
import org.apache.ranger.view.VXAccessAuditList;
import org.apache.ranger.view.VXLong;
import org.apache.ranger.view.VXTrxLog;
import org.apache.ranger.view.VXTrxLogList;
import org.junit.Assert;
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
public class TestXAuditREST {

	@InjectMocks
	XAuditREST auditREST = new XAuditREST();

	@Mock
	XAuditMgr xAuditMgr;

	@Mock
	SearchUtil searchUtil;

	@Mock
	XTrxLogService xLog;

	@Mock
	XAccessAuditService xAccessAuditSrv;

	@Mock
	VXTrxLogList vxExpList;

	@Mock
	HttpServletRequest request;

	@Mock
	SearchCriteria searchCriteria;

	Long id = 5L;
	String name = "test";

	@Test
	public void Test1getXTrxLog() {
		VXTrxLog vxExp = new VXTrxLog();
		vxExp.setId(id);
		vxExp.setObjectName(name);
		Mockito.when(xAuditMgr.getXTrxLog(id)).thenReturn(vxExp);
		VXTrxLog vxAct = auditREST.getXTrxLog(id);
		Assert.assertNotNull(vxAct);
		Assert.assertEquals(vxExp, vxAct);
		Assert.assertEquals(vxExp.getId(), vxAct.getId());
		Assert.assertEquals(vxExp.getObjectName(), vxAct.getObjectName());
		Mockito.verify(xAuditMgr).getXTrxLog(id);
	}

	@Test
	public void Test2createXTrxLog() {
		VXTrxLog vxExp = new VXTrxLog();
		vxExp.setId(id);
		vxExp.setObjectName(name);
		Mockito.when(xAuditMgr.createXTrxLog(vxExp)).thenReturn(vxExp);
		VXTrxLog vxAct = auditREST.createXTrxLog(vxExp);
		Assert.assertNotNull(vxAct);
		Assert.assertEquals(vxExp, vxAct);
		Assert.assertEquals(vxExp.getId(), vxAct.getId());
		Assert.assertEquals(vxExp.getObjectName(), vxAct.getObjectName());
		Mockito.verify(xAuditMgr).createXTrxLog(vxExp);
	}

	@Test
	public void Test3updateXTrxLog() {
		VXTrxLog vxPrev = new VXTrxLog();
		vxPrev.setId(id);
		vxPrev.setObjectName(name);
		VXTrxLog vxExp = new VXTrxLog();
		vxExp.setId(id);
		vxExp.setObjectName("test1");

		Mockito.when(xAuditMgr.updateXTrxLog(vxPrev)).thenReturn(vxExp);

		VXTrxLog vxAct = auditREST.updateXTrxLog(vxPrev);

		Assert.assertNotNull(vxAct);
		Assert.assertEquals(vxExp, vxAct);
		Assert.assertEquals(vxExp.getObjectName(), vxAct.getObjectName());

		Mockito.verify(xAuditMgr).updateXTrxLog(vxPrev);
	}

	@Test
	public void Test4deleteXTrxLog() {
		Mockito.doNothing().when(xAuditMgr).deleteXTrxLog(id, false);

		auditREST.deleteXTrxLog(id, request);

		Mockito.verify(xAuditMgr).deleteXTrxLog(id, false);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void Test5searchXTrxLog() {
		VXTrxLogList vxExpList = new VXTrxLogList();
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		searchCriteria.addParam("name", name);

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any())).thenReturn(searchCriteria);
		Mockito.when(xAuditMgr.searchXTrxLogs(searchCriteria)).thenReturn(vxExpList);

		VXTrxLogList vxActList = auditREST.searchXTrxLogs(request);

		Assert.assertNotNull(vxActList);
		Assert.assertEquals(vxExpList, vxActList);

		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any());
		Mockito.verify(xAuditMgr).searchXTrxLogs(searchCriteria);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void Test6countXTrxLogs() {
		VXLong vxLongExp = new VXLong();
		vxLongExp.setValue(id);

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any())).thenReturn(searchCriteria);
		Mockito.when(xAuditMgr.getXTrxLogSearchCount(searchCriteria)).thenReturn(vxLongExp);

		VXLong vxLongAct = auditREST.countXTrxLogs(request);

		Assert.assertNotNull(vxLongAct);
		Assert.assertEquals(vxLongExp, vxLongAct);
		Assert.assertEquals(vxLongExp.getValue(), vxLongAct.getValue());

		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any());
		Mockito.verify(xAuditMgr).getXTrxLogSearchCount(searchCriteria);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void Test7searchXAccessAudits() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		searchCriteria.addParam("name", name);
		VXAccessAuditList vxAAListExp = new VXAccessAuditList();
		vxAAListExp.setTotalCount(6L);

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any())).thenReturn(searchCriteria);
		Mockito.when(xAuditMgr.searchXAccessAudits(searchCriteria)).thenReturn(vxAAListExp);

		VXAccessAuditList vxAAListAct = auditREST.searchXAccessAudits(request);

		Assert.assertNotNull(vxAAListAct);
		Assert.assertEquals(vxAAListExp, vxAAListAct);
		Assert.assertEquals(vxAAListExp.getTotalCount(), vxAAListAct.getTotalCount());

		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any());
		Mockito.verify(xAuditMgr).searchXAccessAudits(searchCriteria);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void Test8countXAccessAudits() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		searchCriteria.addParam("name", name);
		VXLong vxLongExp = new VXLong();
		vxLongExp.setValue(id);

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any())).thenReturn(searchCriteria);
		Mockito.when(xAuditMgr.getXAccessAuditSearchCount(searchCriteria)).thenReturn(vxLongExp);

		VXLong vxLongAct = auditREST.countXAccessAudits(request);

		Assert.assertNotNull(vxLongAct);
		Assert.assertEquals(vxLongExp, vxLongAct);
		Assert.assertEquals(vxLongExp.getValue(), vxLongAct.getValue());

		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest) Mockito.any(),
				(List<SortField>) Mockito.any());
		Mockito.verify(xAuditMgr).getXAccessAuditSearchCount(searchCriteria);
	}

}
