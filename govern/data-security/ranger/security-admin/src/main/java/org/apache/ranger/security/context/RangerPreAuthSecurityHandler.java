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

package org.apache.ranger.security.context;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.biz.SessionMgr;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.view.VXResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("rangerPreAuthSecurityHandler")
public class RangerPreAuthSecurityHandler {
	Logger logger = LoggerFactory.getLogger(RangerPreAuthSecurityHandler.class);

	@Autowired
	RangerDaoManager daoManager;

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	RangerAPIMapping rangerAPIMapping;
	
	@Autowired
	SessionMgr sessionMgr;

	public boolean isAPIAccessible(String methodName) throws Exception {

		if (methodName == null) {
			return false;
		}

		UserSessionBase userSession = ContextUtil.getCurrentUserSession();
		if (userSession == null) {
			logger.warn("WARNING: UserSession found null. Some non-authorized user might be trying to access the API.");
			return false;
		}

		if (userSession.isUserAdmin()) {
			if (logger.isDebugEnabled()) {
				logger.debug("WARNING: Logged in user is System Admin, System Admin is allowed to access all the tabs except Key Manager."
						+ "Reason for returning true is, In few cases system admin needs to have access on Key Manager tabs as well.");
			}
			return true;
		}

		Set<String> associatedTabs = rangerAPIMapping.getAssociatedTabsWithAPI(methodName);
		if (CollectionUtils.isEmpty(associatedTabs)) {
			return true;
		}
                if(associatedTabs.contains(RangerAPIMapping.TAB_PERMISSIONS) && userSession.isAuditUserAdmin()){
                        return true;
                }
		return isAPIAccessible(associatedTabs);
	}

	public boolean isAPIAccessible(Set<String> associatedTabs) throws Exception {

		UserSessionBase userSession = ContextUtil.getCurrentUserSession();
		if (userSession != null) {
			sessionMgr.refreshPermissionsIfNeeded(userSession);
			if (userSession.getRangerUserPermission() != null) {
				CopyOnWriteArraySet<String> accessibleModules = userSession.getRangerUserPermission().getUserPermissions();
				if (CollectionUtils.containsAny(accessibleModules, associatedTabs)) {
					return true;
				}
			}
		}
		VXResponse gjResponse = new VXResponse();
        gjResponse.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
        gjResponse.setMsgDesc("User is not allowed to access the API");
        throw restErrorUtil.generateRESTException(gjResponse);
	}

	public boolean isAPISpnegoAccessible(){
		UserSessionBase userSession = ContextUtil.getCurrentUserSession();
                if (userSession != null && (userSession.isSpnegoEnabled() || userSession.isUserAdmin() || userSession.isAuditUserAdmin())) {
			return true;
                }else if(userSession != null && (userSession.isUserAdmin() || userSession.isKeyAdmin() || userSession.isAuditKeyAdmin())){
			return true;
		}
        VXResponse gjResponse = new VXResponse();
        gjResponse.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
        gjResponse.setMsgDesc("User is not allowed to access the API");
        throw restErrorUtil.generateRESTException(gjResponse);
	}
	
	public boolean isAdminOrKeyAdminRole(){
		UserSessionBase userSession = ContextUtil.getCurrentUserSession();
		if (userSession != null && (userSession.isKeyAdmin() || userSession.isUserAdmin())) {
			return true;
		}
		VXResponse gjResponse = new VXResponse();
        gjResponse.setStatusCode(HttpServletResponse.SC_FORBIDDEN); // assert user is authenticated.
        gjResponse.setMsgDesc("User is not allowed to access the API");
        throw restErrorUtil.generateRESTException(gjResponse);
	}
}
