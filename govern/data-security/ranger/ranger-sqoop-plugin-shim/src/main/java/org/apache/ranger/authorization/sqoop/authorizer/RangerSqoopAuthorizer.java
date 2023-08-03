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

package org.apache.ranger.authorization.sqoop.authorizer;

import java.util.List;

import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.apache.sqoop.common.SqoopException;
import org.apache.sqoop.model.MPrincipal;
import org.apache.sqoop.model.MPrivilege;
import org.apache.sqoop.security.AuthorizationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerSqoopAuthorizer extends AuthorizationValidator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerSqoopAuthorizer.class);

	private static final String RANGER_PLUGIN_TYPE = "sqoop";
	private static final String RANGER_SQOOP_AUTHORIZER_IMPL_CLASSNAME = "org.apache.ranger.authorization.sqoop.authorizer.RangerSqoopAuthorizer";

	private AuthorizationValidator  authorizationValidator  = null;
	private RangerPluginClassLoader rangerPluginClassLoader = null;

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

		try {

			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());

			@SuppressWarnings("unchecked")
			Class<AuthorizationValidator> cls = (Class<AuthorizationValidator>) Class.forName(
					RANGER_SQOOP_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			authorizationValidator = cls.newInstance();
		} catch (Exception e) {
			LOG.error("Error Enabling RangerSqoopAuthorizer", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSqoopAuthorizer.init()");
		}
	}

	@Override
	public void checkPrivileges(MPrincipal principal, List<MPrivilege> privileges) throws SqoopException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSqoopAuthorizer.checkPrivileges()");
		}

		try {
			activatePluginClassLoader();

			authorizationValidator.checkPrivileges(principal, privileges);
		} finally {
			deactivatePluginClassLoader();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSqoopAuthorizer.checkPrivileges()");
		}
	}

	private void activatePluginClassLoader() {
		if (rangerPluginClassLoader != null) {
			rangerPluginClassLoader.activate();
		}
	}

	private void deactivatePluginClassLoader() {
		if (rangerPluginClassLoader != null) {
			rangerPluginClassLoader.deactivate();
		}
	}
}
