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

package org.apache.ranger.authorization.kylin.authorizer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.util.Pair;
import org.apache.kylin.metadata.project.ProjectInstance;
import org.apache.kylin.metadata.project.ProjectManager;
import org.apache.kylin.rest.security.AclEntityType;
import org.apache.kylin.rest.security.AclPermission;
import org.apache.kylin.rest.security.ExternalAclProvider;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.services.kylin.client.KylinResourceMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.acls.model.Permission;

import com.google.common.collect.Sets;

public class RangerKylinAuthorizer extends ExternalAclProvider {
	private static final Logger LOG = LoggerFactory.getLogger(RangerKylinAuthorizer.class);

	private static volatile RangerKylinPlugin kylinPlugin = null;

	private static String clientIPAddress = null;

	@Override
	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKylinAuthorizer.init()");
		}

		RangerKylinPlugin plugin = kylinPlugin;

		if (plugin == null) {
			synchronized (RangerKylinAuthorizer.class) {
				plugin = kylinPlugin;

				if (plugin == null) {
					plugin = new RangerKylinPlugin();
					plugin.init();
					kylinPlugin = plugin;

					clientIPAddress = getClientIPAddress();
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKylinAuthorizer.init()");
		}
	}

	@Override
	public boolean checkPermission(String user, List<String> groups, String entityType, String entityUuid,
			Permission permission) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKylinAuthorizer.checkPermission( user=" + user + ", groups=" + groups
					+ ", entityType=" + entityType + ", entityUuid=" + entityUuid + ", permission=" + permission + ")");
		}

		boolean ret = false;

		if (kylinPlugin != null) {
			String projectName = null;
			KylinConfig kylinConfig = KylinConfig.getInstanceFromEnv();
			if (AclEntityType.PROJECT_INSTANCE.equals(entityType)) {
				ProjectInstance projectInstance = ProjectManager.getInstance(kylinConfig).getPrjByUuid(entityUuid);
				if (projectInstance != null) {
					projectName = projectInstance.getName();
				} else {
					if (LOG.isWarnEnabled()) {
						LOG.warn("Could not find kylin project for given uuid=" + entityUuid);
					}
				}
			}

			String accessType = ExternalAclProvider.transformPermission(permission);
			RangerKylinAccessRequest request = new RangerKylinAccessRequest(projectName, user, groups, accessType,
					clientIPAddress);

			RangerAccessResult result = kylinPlugin.isAccessAllowed(request);
			if (result != null && result.getIsAllowed()) {
				ret = true;
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKylinAuthorizer.checkPermission(): result=" + ret);
		}

		return ret;
	}

	private String getClientIPAddress() {
		InetAddress ip = null;
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Failed to get client IP address." + e);
			}
		}

		String ret = null;
		if (ip != null) {
			ret = ip.getHostAddress();
		}
		return ret;
	}

	@Override
	public List<Pair<String, AclPermission>> getAcl(String entityType, String entityUuid) {
		// No need to implement
		return null;
	}
}

class RangerKylinPlugin extends RangerBasePlugin {
	public RangerKylinPlugin() {
		super("kylin", "kylin");
	}

	@Override
	public void init() {
		super.init();

		RangerDefaultAuditHandler auditHandler = new RangerDefaultAuditHandler(getConfig());

		super.setResultProcessor(auditHandler);
	}
}

class RangerKylinResource extends RangerAccessResourceImpl {
	public RangerKylinResource(String projectName) {
		if (StringUtils.isEmpty(projectName)) {
			projectName = "*";
		}

		setValue(KylinResourceMgr.PROJECT, projectName);
	}
}

class RangerKylinAccessRequest extends RangerAccessRequestImpl {
	public RangerKylinAccessRequest(String projectName, String user, List<String> groups, String accessType,
			String clientIPAddress) {
		super.setResource(new RangerKylinResource(projectName));
		super.setAccessType(accessType);
		super.setUser(user);
		super.setUserGroups(Sets.newHashSet(groups));
		super.setAccessTime(new Date());
		super.setClientIPAddress(clientIPAddress);
		super.setAction(accessType);
	}
}
