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

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.entity.XXAuthSession;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.view.VXAuthSession;
import org.apache.ranger.view.VXAuthSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class AuthSessionService extends
		AbstractBaseResourceService<XXAuthSession, VXAuthSession> {
	@Autowired
	StringUtil stringUtil;

	public static final String NAME = "AuthSession";

	public static final List<SortField> AUTH_SESSION_SORT_FLDS = new ArrayList<SortField>();
	static {
		AUTH_SESSION_SORT_FLDS.add(new SortField("id", "obj.id"));
		AUTH_SESSION_SORT_FLDS.add(new SortField("authTime", "obj.authTime",
				true, SortField.SORT_ORDER.DESC));
	}

	public static List<SearchField> AUTH_SESSION_SEARCH_FLDS = new ArrayList<SearchField>();
	static {
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createLong("id", "obj.id"));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createString("loginId",
				"obj.loginId", SearchField.SEARCH_TYPE.PARTIAL,
				StringUtil.VALIDATION_LOGINID));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createLong("userId",
				"obj.userId"));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createEnum("authStatus",
				"obj.authStatus", "statusList", XXAuthSession.AuthStatus_MAX));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createEnum("authType",
				"obj.authType", "Authentication Type",
				XXAuthSession.AuthType_MAX));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createEnum("deviceType",
				"obj.deviceType", "Device Type", RangerConstants.DeviceType_MAX));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createString("requestIP",
				"obj.requestIP", SearchField.SEARCH_TYPE.PARTIAL,
				StringUtil.VALIDATION_IP_ADDRESS));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createString(
				"requestUserAgent", "obj.requestUserAgent",
				SearchField.SEARCH_TYPE.PARTIAL, null));
		AUTH_SESSION_SEARCH_FLDS.add(new SearchField("firstName",
				"obj.user.firstName", SearchField.DATA_TYPE.STRING,
				SearchField.SEARCH_TYPE.PARTIAL));
		AUTH_SESSION_SEARCH_FLDS.add(new SearchField("lastName",
				"obj.user.lastName", SearchField.DATA_TYPE.STRING,
				SearchField.SEARCH_TYPE.PARTIAL));
		AUTH_SESSION_SEARCH_FLDS.add(SearchField.createString("requestIP",
				"obj.requestIP", SearchField.SEARCH_TYPE.PARTIAL,
				StringUtil.VALIDATION_IP_ADDRESS));	
		AUTH_SESSION_SEARCH_FLDS.add(new SearchField("startDate", "obj.createTime",
				SearchField.DATA_TYPE.DATE, SearchField.SEARCH_TYPE.GREATER_EQUAL_THAN));
		AUTH_SESSION_SEARCH_FLDS.add(new SearchField("endDate", "obj.createTime",
				SearchField.DATA_TYPE.DATE, SearchField.SEARCH_TYPE.LESS_EQUAL_THAN));
	}

	@Override
	protected String getResourceName() {
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected XXAuthSession createEntityObject() {
		return new XXAuthSession();
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected VXAuthSession createViewObject() {
		return new VXAuthSession();
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected void validateForCreate(VXAuthSession vXAuthSession) {
		logger.error("This method is not required and shouldn't be called.",
				new Throwable().fillInStackTrace());
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected void validateForUpdate(VXAuthSession vXAuthSession,
			XXAuthSession mObj) {
		logger.error("This method is not required and shouldn't be called.",
				new Throwable().fillInStackTrace());
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected XXAuthSession mapViewToEntityBean(VXAuthSession vXAuthSession,
			XXAuthSession t, int OPERATION_CONTEXT) {
		logger.error("This method is not required and shouldn't be called.",
				new Throwable().fillInStackTrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected VXAuthSession mapEntityToViewBean(VXAuthSession viewObj,
			XXAuthSession resource) {
		viewObj.setLoginId(resource.getLoginId());
		viewObj.setAuthTime(resource.getAuthTime());
		viewObj.setAuthStatus(resource.getAuthStatus());
		viewObj.setAuthType(resource.getAuthType());
		viewObj.setDeviceType(resource.getDeviceType());
		viewObj.setId(resource.getId());
		viewObj.setRequestIP(resource.getRequestIP());

		viewObj.setRequestUserAgent(resource.getRequestUserAgent());

		if (resource.getUserId() != null) {
			viewObj.setUserId(resource.getUserId());

			XXPortalUser gjUser = daoManager.getXXPortalUser().getById(resource.getUserId());
			if (gjUser != null) {
				viewObj.setEmailAddress(gjUser.getEmailAddress());
				viewObj.setFamilyScreenName(gjUser.getLastName());
				viewObj.setFirstName(gjUser.getFirstName());
				viewObj.setLastName(gjUser.getLastName());
				viewObj.setPublicScreenName(gjUser.getPublicScreenName());
			}
		}

		return viewObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXAuthSessionList search(SearchCriteria searchCriteria) {
		VXAuthSessionList returnList = new VXAuthSessionList();
		List<VXAuthSession> viewList = new ArrayList<VXAuthSession>();

		List<XXAuthSession> resultList = searchResources(searchCriteria,
				AUTH_SESSION_SEARCH_FLDS, AUTH_SESSION_SORT_FLDS, returnList);

		// Iterate over the result list and create the return list
		for (XXAuthSession gjObj : resultList) {
			VXAuthSession viewObj = populateViewBean(gjObj);
			viewList.add(viewObj);
		}

		returnList.setVXAuthSessions(viewList);
		return returnList;
	}
}
