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

/**
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXAccessAudit;
import org.apache.ranger.view.VXAccessAudit;
import org.apache.ranger.view.VXAccessAuditList;

public abstract class XAccessAuditServiceBase<T extends XXAccessAudit, V extends VXAccessAudit>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XAccessAudit";

	public XAccessAuditServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		mObj.setAuditType( vObj.getAuditType());
		mObj.setAccessResult( vObj.getAccessResult());
		mObj.setAccessType( vObj.getAccessType());
		mObj.setAclEnforcer( vObj.getAclEnforcer());
		mObj.setAgentId( vObj.getAgentId());
		mObj.setClientIP( vObj.getClientIP());
		mObj.setClientType( vObj.getClientType());
		mObj.setPolicyId( vObj.getPolicyId());
		mObj.setRepoName( vObj.getRepoName());
		mObj.setRepoType( vObj.getRepoType());
		mObj.setResultReason( vObj.getResultReason());
		mObj.setSessionId( vObj.getSessionId());
		mObj.setEventTime( vObj.getEventTime());
		mObj.setRequestUser( vObj.getRequestUser());
		mObj.setAction( vObj.getAction());
		mObj.setRequestData( vObj.getRequestData());
		mObj.setResourcePath( vObj.getResourcePath());
		mObj.setResourceType( vObj.getResourceType());
		mObj.setSequenceNumber( vObj.getSequenceNumber());
		mObj.setEventCount( vObj.getEventCount());
		mObj.setEventDuration( vObj.getEventDuration());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setAuditType( mObj.getAuditType());
		vObj.setAccessResult( mObj.getAccessResult());
		vObj.setAccessType( mObj.getAccessType());
		vObj.setAclEnforcer( mObj.getAclEnforcer());
		vObj.setAgentId( mObj.getAgentId());
		vObj.setClientIP( mObj.getClientIP());
		vObj.setClientType( mObj.getClientType());
		vObj.setPolicyId( mObj.getPolicyId());
		vObj.setRepoName( mObj.getRepoName());
		vObj.setRepoType( mObj.getRepoType());
		vObj.setResultReason( mObj.getResultReason());
		vObj.setSessionId( mObj.getSessionId());
		vObj.setEventTime( mObj.getEventTime());
		vObj.setRequestUser( mObj.getRequestUser());
		vObj.setAction( mObj.getAction());
		vObj.setRequestData( mObj.getRequestData());
		vObj.setResourcePath( mObj.getResourcePath());
		vObj.setResourceType( mObj.getResourceType());
		vObj.setSequenceNumber( mObj.getSequenceNumber());
		vObj.setEventCount( mObj.getEventCount());
		vObj.setEventDuration( mObj.getEventDuration());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXAccessAuditList searchXAccessAudits(SearchCriteria searchCriteria) {
		VXAccessAuditList returnList = new VXAccessAuditList();
		List<VXAccessAudit> xAccessAuditList = new ArrayList<VXAccessAudit>();

		List<T> resultList = searchResources(searchCriteria,
				searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXAccessAudit : resultList) {
			VXAccessAudit vXAccessAudit = populateViewBean(gjXAccessAudit);
			xAccessAuditList.add(vXAccessAudit);
		}

		returnList.setVXAccessAudits(xAccessAuditList);
		return returnList;
	}

}
