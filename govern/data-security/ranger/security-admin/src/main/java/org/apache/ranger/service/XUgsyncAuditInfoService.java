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

import org.apache.ranger.common.*;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.common.SortField.SORT_ORDER;
import org.apache.ranger.entity.XXUgsyncAuditInfo;
import org.apache.ranger.view.VXUgsyncAuditInfo;
import org.apache.ranger.view.VXUgsyncAuditInfoList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("singleton")
public class XUgsyncAuditInfoService extends XUgsyncAuditInfoServiceBase<XXUgsyncAuditInfo, VXUgsyncAuditInfo>{
	@Autowired
	JSONUtil jsonUtil;

	public static final String NAME = "XUgsyncAuditInfo";
	protected static final String distinctCountQueryStr = "SELECT COUNT(distinct obj.id) FROM XXUgsyncAuditInfo obj ";
	protected static final String distinctQueryStr = "SELECT distinct obj FROM XXUgsyncAuditInfo obj ";

	public XUgsyncAuditInfoService() {
		countQueryStr = "SELECT COUNT(obj) FROM XXUgsyncAuditInfo  obj ";
		queryStr = "SELECT obj FROM XXUgsyncAuditInfo obj ";

		sortFields.add(new SortField("eventTime", "obj.eventTime", true, SORT_ORDER.DESC));
		searchFields.add(new SearchField("userName", "obj.userName",
				DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("sessionId", "obj.sessionId",
				DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("syncSource", "obj.syncSource",
				DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("noOfNewUsers", "obj.noOfNewUsers",
				DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("noOfNewGroups", "obj.noOfNewGroups",
				DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("noOfModifiedUsers", "obj.noOfModifiedUsers",
				DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("noOfModifiedGroups", "obj.noOfModifiedGroups",
				DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("syncSourceInfo", "obj.syncSourceInfo", DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("startDate", "obj.eventTime",
				DATA_TYPE.DATE, SEARCH_TYPE.GREATER_EQUAL_THAN));
		searchFields.add(new SearchField("endDate", "obj.eventTime",
				DATA_TYPE.DATE, SEARCH_TYPE.LESS_EQUAL_THAN));
}

	protected XXUgsyncAuditInfo mapViewToEntityBean(VXUgsyncAuditInfo vObj, XXUgsyncAuditInfo mObj, int OPERATION_CONTEXT) {
		mObj.setId(vObj.getId());
		mObj.setEventTime(vObj.getEventTime());
		mObj.setUserName(vObj.getUserName());
		mObj.setSyncSource(vObj.getSyncSource());
		mObj.setNoOfNewUsers(vObj.getNoOfNewUsers());
		mObj.setNoOfNewGroups(vObj.getNoOfNewGroups());
		mObj.setNoOfModifiedUsers(vObj.getNoOfModifiedUsers());
		mObj.setNoOfModifiedGroups(vObj.getNoOfModifiedGroups());
		mObj.setSyncSourceInfo(jsonUtil.readMapToString(vObj.getSyncSourceInfo()));
		mObj.setSessionId(vObj.getSessionId());
		return mObj;
	}

	protected VXUgsyncAuditInfo mapEntityToViewBean(VXUgsyncAuditInfo vObj, XXUgsyncAuditInfo mObj) {
		vObj.setId(mObj.getId());
		vObj.setEventTime(mObj.getEventTime());
		vObj.setUserName(mObj.getUserName());
		vObj.setSyncSource(mObj.getSyncSource());
		vObj.setNoOfNewUsers(mObj.getNoOfNewUsers());
		vObj.setNoOfNewGroups(mObj.getNoOfNewGroups());
		vObj.setNoOfModifiedUsers(mObj.getNoOfModifiedUsers());
		vObj.setNoOfModifiedGroups(mObj.getNoOfModifiedGroups());
		String jsonString = mObj.getSyncSourceInfo();
		vObj.setSyncSourceInfo(jsonUtil.jsonToMap(jsonString));
		vObj.setSessionId( mObj.getSessionId());

		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXUgsyncAuditInfoList searchXUgsyncAuditInfoList(SearchCriteria searchCriteria) {
        VXUgsyncAuditInfoList returnList = new VXUgsyncAuditInfoList();
        List<VXUgsyncAuditInfo> xUgsyncAuditInfoList = new ArrayList<VXUgsyncAuditInfo>();

        List<XXUgsyncAuditInfo> resultList = (List<XXUgsyncAuditInfo>) searchResources(searchCriteria,
                searchFields, sortFields, returnList);

        // Iterate over the result list and create the return list
        for (XXUgsyncAuditInfo gjXUgsyncAuditInfo : resultList) {
            VXUgsyncAuditInfo vxUgsyncAuditInfo = populateViewBean(gjXUgsyncAuditInfo);

            if(vxUgsyncAuditInfo != null) {
				xUgsyncAuditInfoList.add(vxUgsyncAuditInfo);
            }
        }

        returnList.setVxUgsyncAuditInfoList(xUgsyncAuditInfoList);
        return returnList;
    }

	public VXUgsyncAuditInfoList searchXUgsyncAuditInfoBySyncSource(String syncSource) {
		VXUgsyncAuditInfoList returnList = new VXUgsyncAuditInfoList();
		List<VXUgsyncAuditInfo> xUgsyncAuditInfoList = new ArrayList<VXUgsyncAuditInfo>();

		List<XXUgsyncAuditInfo> resultList = daoManager.getXXUgsyncAuditInfo().findBySyncSource(syncSource);

		// Iterate over the result list and create the return list
		for (XXUgsyncAuditInfo gjXUgsyncAuditInfo : resultList) {
			VXUgsyncAuditInfo vxUgsyncAuditInfo = populateViewBean(gjXUgsyncAuditInfo);

			if(vxUgsyncAuditInfo != null) {
				xUgsyncAuditInfoList.add(vxUgsyncAuditInfo);
			}
		}

		returnList.setVxUgsyncAuditInfoList(xUgsyncAuditInfoList);
		return returnList;
	}

	public VXUgsyncAuditInfo createUgsyncAuditInfo(VXUgsyncAuditInfo vxUgsyncAuditInfo) {

		Long sessionId = ContextUtil.getCurrentUserSession() != null ? ContextUtil.getCurrentUserSession().getSessionId() : null;
		if (sessionId != null) {
			vxUgsyncAuditInfo.setSessionId("" + sessionId);
		}
		vxUgsyncAuditInfo.setEventTime(DateUtil.getUTCDate());
		vxUgsyncAuditInfo.setUserName(ContextUtil.getCurrentUserLoginId());

		// Process the sync source information
		if (vxUgsyncAuditInfo.getUnixSyncSourceInfo() != null) {
			vxUgsyncAuditInfo.setSyncSourceInfo(jsonUtil.jsonToMap(vxUgsyncAuditInfo.getUnixSyncSourceInfo().toString()));
		} else if (vxUgsyncAuditInfo.getFileSyncSourceInfo() != null) {
			vxUgsyncAuditInfo.setSyncSourceInfo(jsonUtil.jsonToMap(vxUgsyncAuditInfo.getFileSyncSourceInfo().toString()));
		} else if (vxUgsyncAuditInfo.getLdapSyncSourceInfo() != null) {
			vxUgsyncAuditInfo.setSyncSourceInfo(jsonUtil.jsonToMap(vxUgsyncAuditInfo.getLdapSyncSourceInfo().toString()));
		}

		return createResource(vxUgsyncAuditInfo);
	}
}
