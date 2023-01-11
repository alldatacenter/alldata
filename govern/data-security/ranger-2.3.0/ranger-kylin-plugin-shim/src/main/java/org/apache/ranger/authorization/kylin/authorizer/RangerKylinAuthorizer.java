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

package org.apache.ranger.authorization.kylin.authorizer;

import java.util.List;

import org.apache.kylin.common.util.Pair;
import org.apache.kylin.rest.security.AclPermission;
import org.apache.kylin.rest.security.ExternalAclProvider;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.acls.model.Permission;

public class RangerKylinAuthorizer extends ExternalAclProvider {
	private static final Logger LOG = LoggerFactory.getLogger(RangerKylinAuthorizer.class);

	private static final String RANGER_PLUGIN_TYPE = "kylin";
	private static final String RANGER_KYLIN_AUTHORIZER_IMPL_CLASSNAME = "org.apache.ranger.authorization.kylin.authorizer.RangerKylinAuthorizer";

	private ExternalAclProvider     externalAclProvider     = null;
	private RangerPluginClassLoader rangerPluginClassLoader = null;

	public RangerKylinAuthorizer() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKylinAuthorizer.RangerKylinAuthorizer()");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKylinAuthorizer.RangerKylinAuthorizer()");
		}
	}

	@Override
	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKylinAuthorizer.init()");
		}

		try {

			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());

			@SuppressWarnings("unchecked")
			Class<ExternalAclProvider> cls = (Class<ExternalAclProvider>) Class.forName(
					RANGER_KYLIN_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			externalAclProvider = cls.newInstance();
			externalAclProvider.init();
		} catch (Exception e) {
			LOG.error("Error Enabling RangerKylinPlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKylinAuthorizer.init()");
		}
	}

	@Override
	public boolean checkPermission(String user, List<String> groups, String entityType, String entityUuid,
			Permission permission) {
		boolean ret = false;

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKylinAuthorizer.checkPermission()");
		}

		try {
			activatePluginClassLoader();

			ret = externalAclProvider.checkPermission(user, groups, entityType, entityUuid, permission);
		} finally {
			deactivatePluginClassLoader();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKylinAuthorizer.checkPermission()");
		}

		return ret;
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

	@Override
	public List<Pair<String, AclPermission>> getAcl(String entityType, String entityUuid) {
		// No need to implement
		return null;
	}
}
