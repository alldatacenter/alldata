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

package org.apache.ranger.authorization.sqoop.authorizer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.services.sqoop.client.SqoopResourceMgr;
import org.apache.sqoop.common.SqoopException;
import org.apache.sqoop.model.MPrincipal;
import org.apache.sqoop.model.MPrivilege;
import org.apache.sqoop.model.MResource;
import org.apache.sqoop.security.AuthorizationValidator;
import org.apache.sqoop.security.SecurityError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class RangerSqoopAuthorizer extends AuthorizationValidator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerSqoopAuthorizer.class);

	private static volatile RangerSqoopPlugin sqoopPlugin = null;

	private static String clientIPAddress = null;

	public RangerSqoopAuthorizer() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSqoopAuthorizer.RangerSqoopAuthorizer()");
		}

		this.init();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSqoopAuthorizer.RangerSqoopAuthorizer()");
		}
	}

	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSqoopAuthorizer.init()");
		}

		RangerSqoopPlugin plugin = sqoopPlugin;

		if (plugin == null) {
			synchronized (RangerSqoopAuthorizer.class) {
				plugin = sqoopPlugin;

				if (plugin == null) {
					plugin = new RangerSqoopPlugin();
					plugin.init();
					sqoopPlugin = plugin;

					clientIPAddress = getClientIPAddress();
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSqoopAuthorizer.init()");
		}
	}

	@Override
	public void checkPrivileges(MPrincipal principal, List<MPrivilege> privileges) throws SqoopException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSqoopAuthorizer.checkPrivileges( principal=" + principal + ", privileges="
					+ privileges + ")");
		}

		if (CollectionUtils.isEmpty(privileges)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerSqoopAuthorizer.checkPrivileges() return because privileges is empty.");
			}
			return;
		}

		RangerSqoopPlugin plugin = sqoopPlugin;

		if (plugin != null) {
			for (MPrivilege privilege : privileges) {
				RangerSqoopAccessRequest request = new RangerSqoopAccessRequest(principal, privilege, clientIPAddress);

				RangerAccessResult result = plugin.isAccessAllowed(request);
				if (result != null && !result.getIsAllowed()) {
					throw new SqoopException(SecurityError.AUTH_0014, "principal=" + principal
							+ " does not have privileges for : " + privilege);
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSqoopAuthorizer.checkPrivileges() success without exception.");
		}
	}

	private String getClientIPAddress() {
		InetAddress ip = null;
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Failed to get Client IP Address" + e);
			}
		}

		String ret = null;
		if (ip != null) {
			ret = ip.getHostAddress();
		}
		return ret;
	}
}

class RangerSqoopPlugin extends RangerBasePlugin {
	public RangerSqoopPlugin() {
		super("sqoop", "sqoop");
	}

	@Override
	public void init() {
		super.init();

		RangerDefaultAuditHandler auditHandler = new RangerDefaultAuditHandler(getConfig());

		super.setResultProcessor(auditHandler);
	}
}

class RangerSqoopResource extends RangerAccessResourceImpl {
	public RangerSqoopResource(MResource resource) {
		if (MResource.TYPE.CONNECTOR.name().equals(resource.getType())) {
			setValue(SqoopResourceMgr.CONNECTOR, resource.getName());
		}
		if (MResource.TYPE.LINK.name().equals(resource.getType())) {
			setValue(SqoopResourceMgr.LINK, resource.getName());
		}
		if (MResource.TYPE.JOB.name().equals(resource.getType())) {
			setValue(SqoopResourceMgr.JOB, resource.getName());
		}
	}
}

class RangerSqoopAccessRequest extends RangerAccessRequestImpl {
	public RangerSqoopAccessRequest(MPrincipal principal, MPrivilege privilege,String clientIPAddress) {
		super.setResource(new RangerSqoopResource(privilege.getResource()));
		if (MPrincipal.TYPE.USER.name().equals(principal.getType())) {
			super.setUser(principal.getName());
		}
		if (MPrincipal.TYPE.GROUP.name().equals(principal.getType())) {
			super.setUserGroups(Sets.newHashSet(principal.getName()));
		}

		String action = privilege.getAction();
		super.setAccessType(action);
		super.setAction(action);

		super.setAccessTime(new Date());
		super.setClientIPAddress(clientIPAddress);
	}
}
