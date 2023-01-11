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

 package org.apache.ranger.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.ranger.entity.XXAuthSession;
import org.apache.ranger.entity.XXPortalUser;

public class UserSessionBase implements Serializable {

	private static final long serialVersionUID = 1L;

	XXPortalUser xXPortalUser;
	XXAuthSession xXAuthSession;
	private boolean userAdmin;
    private boolean userAuditAdmin = false;
    private boolean auditKeyAdmin = false;
	private boolean keyAdmin = false;
	private int authProvider = RangerConstants.USER_APP;
	private List<String> userRoleList = new ArrayList<String>();
	private RangerUserPermission rangerUserPermission;
	int clientTimeOffsetInMinute = 0;
	private Boolean isSSOEnabled;
	private Boolean isSpnegoEnabled = false;

	public Long getUserId() {
		if (xXPortalUser != null) {
			return xXPortalUser.getId();
		}
		return null;
	}

	public String getLoginId() {
		if (xXPortalUser != null) {
			return xXPortalUser.getLoginId();
		}
		return null;
	}

	public Long getSessionId() {
		if (xXAuthSession != null) {
			return xXAuthSession.getId();
		}
		return null;
	}

	public boolean isUserAdmin() {
		return userAdmin;
	}
	
    public boolean isAuditUserAdmin() {
        return userAuditAdmin;
    }

    public void setAuditUserAdmin(boolean userAuditAdmin) {
        this.userAuditAdmin = userAuditAdmin;
    }

	public void setUserAdmin(boolean userAdmin) {
		this.userAdmin = userAdmin;
	}

	public XXPortalUser getXXPortalUser() {
		return xXPortalUser;
	}

	public void setXXAuthSession(XXAuthSession gjAuthSession) {
		this.xXAuthSession = gjAuthSession;
	}

	public void setXXPortalUser(XXPortalUser gjUser) {
		this.xXPortalUser = gjUser;
	}

	public void setAuthProvider(int userSource) {
		this.authProvider = userSource;
	}

	public void setUserRoleList(List<String> strRoleList) {
		this.userRoleList = strRoleList;
	}
	public List<String> getUserRoleList() {
		return this.userRoleList;
	}

	public int getAuthProvider() {
		return this.authProvider;
	}

	public int getClientTimeOffsetInMinute() {
		return clientTimeOffsetInMinute;
	}

	public void setClientTimeOffsetInMinute(int clientTimeOffsetInMinute) {
		this.clientTimeOffsetInMinute = clientTimeOffsetInMinute;
	}

	public boolean isKeyAdmin() {
		return keyAdmin;
	}

	public void setKeyAdmin(boolean keyAdmin) {
		this.keyAdmin = keyAdmin;
	}

    public boolean isAuditKeyAdmin() {
        return auditKeyAdmin;
    }

    public void setAuditKeyAdmin(boolean auditKeyAdmin) {
        this.auditKeyAdmin = auditKeyAdmin;
    }
	/**
	 * @return the rangerUserPermission
	 */
	public RangerUserPermission getRangerUserPermission() {
		return rangerUserPermission;
	}

	/**
	 * @param rangerUserPermission the rangerUserPermission to set
	 */
	public void setRangerUserPermission(RangerUserPermission rangerUserPermission) {
		this.rangerUserPermission = rangerUserPermission;
	}



	public Boolean isSSOEnabled() {
		return isSSOEnabled;
	}

	public void setSSOEnabled(Boolean isSSOEnabled) {
		this.isSSOEnabled = isSSOEnabled;
	}

	public Boolean isSpnegoEnabled() {
		return isSpnegoEnabled;
	}

	public void setSpnegoEnabled(Boolean isSpnegoEnabled) {
		this.isSpnegoEnabled = isSpnegoEnabled;
	}

	public static class RangerUserPermission implements Serializable {
		private static final long serialVersionUID = 1L;

		protected CopyOnWriteArraySet<String> userPermissions;
		protected Long lastUpdatedTime;

		/**
		 * @return the userPermissions
		 */
		public CopyOnWriteArraySet<String> getUserPermissions() {
			return userPermissions;
		}
		/**
		 * @param userPermissions the userPermissions to set
		 */
		public void setUserPermissions(CopyOnWriteArraySet<String> userPermissions) {
			this.userPermissions = userPermissions;
		}
		/**
		 * @return the lastUpdatedTime
		 */
		public Long getLastUpdatedTime() {
			return lastUpdatedTime;
		}
		/**
		 * @param lastUpdatedTime the lastUpdatedTime to set
		 */
		public void setLastUpdatedTime(Long lastUpdatedTime) {
			this.lastUpdatedTime = lastUpdatedTime;
		}

	}

}
