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

 package org.apache.ranger.security.handler;

/**
 *
 */

import java.io.Serializable;

import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXDBBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RangerDomainObjectSecurityHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(RangerDomainObjectSecurityHandler.class);

	@Autowired
	public RangerDaoManager daoManager;

	@Autowired
	RangerBizUtil msBizUtil;

	boolean checkParentObject = false;

	public RangerDomainObjectSecurityHandler() {
		checkParentObject = PropertiesUtil.getBooleanProperty(
				"xa.db.access.filter.check.parentobject", checkParentObject);
	}

	/**
	 * @return the daoManager
	 */
	public RangerDaoManager getDaoManager() {
		return daoManager;
	}

	public <T extends XXDBBase> boolean hasAccess(T targetDomainObject,
			Permission.permissionType permission) {
		//TODO: Need to review this method and reimplement it properly
		return true;
	}

	public boolean hasAccess(String targetType, Serializable targetId,
			Permission.permissionType permission) {
		try {
			Class<?> clazz = Class.forName(targetType);
			Class<? extends XXDBBase> gjClazz = clazz.asSubclass(XXDBBase.class);
			return hasAccess(gjClazz, targetId, permission);

		} catch (ClassNotFoundException cfe) {
			logger.error("class not found:" + targetType, cfe);
		} catch (Exception e) {
			logger.error("Excepion targetType:" + targetType + " targetId:"
					+ targetId, e);
		}

		return false;
	}

	public boolean hasAccess(Class<? extends XXDBBase> targetClass,
			Serializable targetId, Permission.permissionType permission) {
		try {
			Class<? extends XXDBBase> gjClazz = targetClass
					.asSubclass(XXDBBase.class);
			XXDBBase base = getDaoManager().getEntityManager().find(gjClazz,
					targetId);
			return hasAccess(base, permission);

		} catch (Exception e) {
			logger.error("Excepion targetType:" + targetClass + " targetId:"
					+ targetId, e);
		}

		return false;
	}

	public boolean hasModeratorPermission() {
		UserSessionBase sess = ContextUtil.getCurrentUserSession();
		if (sess != null && sess.isUserAdmin()) {
			return true;
		}
		return false;
	}

}
