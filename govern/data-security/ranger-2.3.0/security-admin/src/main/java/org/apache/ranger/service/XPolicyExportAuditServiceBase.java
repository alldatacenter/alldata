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

import org.apache.ranger.common.MapUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXPolicyExportAudit;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.view.VXPolicyExportAudit;
import org.apache.ranger.view.VXPolicyExportAuditList;

public abstract class XPolicyExportAuditServiceBase<T extends XXPolicyExportAudit, V extends VXPolicyExportAudit>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XPolicyExportAudit";

	public XPolicyExportAuditServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		mObj.setClientIP( vObj.getClientIP());
		mObj.setAgentId( vObj.getAgentId());
		mObj.setRequestedEpoch( vObj.getRequestedEpoch());
		mObj.setLastUpdated( vObj.getLastUpdated());
		mObj.setRepositoryName( vObj.getRepositoryName());
		mObj.setExportedJson( vObj.getExportedJson());
		mObj.setHttpRetCode( vObj.getHttpRetCode());
		mObj.setClusterName( vObj.getClusterName());
		mObj.setZoneName( vObj.getZoneName());
		mObj.setPolicyVersion( vObj.getPolicyVersion());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setClientIP( mObj.getClientIP());
		vObj.setAgentId( mObj.getAgentId());
		vObj.setRequestedEpoch( mObj.getRequestedEpoch());
		vObj.setLastUpdated( mObj.getLastUpdated());
		vObj.setRepositoryName( mObj.getRepositoryName());
		vObj.setExportedJson( mObj.getExportedJson());
		vObj.setHttpRetCode( mObj.getHttpRetCode());
		vObj.setSyncStatus( MapUtil.getPolicyExportAuditSyncStatus(mObj.getHttpRetCode()));
		vObj.setClusterName( mObj.getClusterName());
		vObj.setZoneName( mObj.getZoneName());
		vObj.setPolicyVersion( mObj.getPolicyVersion());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXPolicyExportAuditList searchXPolicyExportAudits(SearchCriteria searchCriteria) {
		VXPolicyExportAuditList returnList = new VXPolicyExportAuditList();
		List<VXPolicyExportAudit> xPolicyExportAuditList = new ArrayList<VXPolicyExportAudit>();

		List<T> resultList = searchResources(searchCriteria,
				searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXPolicyExportAudit : resultList) {
			VXPolicyExportAudit vXPolicyExportAudit = populateViewBean(gjXPolicyExportAudit);
			XXService xxService = daoManager.getXXService().findByName(vXPolicyExportAudit.getRepositoryName());

			if (xxService != null) {
				vXPolicyExportAudit.setRepositoryDisplayName(xxService.getDisplayName());
			}
			xPolicyExportAuditList.add(vXPolicyExportAudit);
		}

		returnList.setVXPolicyExportAudits(xPolicyExportAuditList);
		return returnList;
	}

}
