
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

package org.apache.ranger.authorization.yarn.authorizer;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.AccessControlList;
import org.apache.hadoop.yarn.security.AccessRequest;
import org.apache.hadoop.yarn.security.Permission;
import org.apache.hadoop.yarn.security.YarnAuthorizationProvider;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class RangerYarnAuthorizer extends YarnAuthorizationProvider {
	private static final Logger LOG  = LoggerFactory.getLogger(RangerYarnAuthorizer.class);

	private static final String   RANGER_PLUGIN_TYPE                      = "yarn";
	private static final String   RANGER_YARN_AUTHORIZER_IMPL_CLASSNAME   = "org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer";

	private YarnAuthorizationProvider yarnAuthorizationProviderImpl = null;
	private RangerPluginClassLoader   rangerPluginClassLoader       = null;
	
	public RangerYarnAuthorizer() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerYarnAuthorizer.RangerYarnAuthorizer()");
		}

		this.init();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerYarnAuthorizer.RangerYarnAuthorizer()");
		}
	}

	private void init(){
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerYarnAuthorizer.init()");
		}

		try {
			
			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());
			
			@SuppressWarnings("unchecked")
			Class<YarnAuthorizationProvider> cls = (Class<YarnAuthorizationProvider>) Class.forName(RANGER_YARN_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			yarnAuthorizationProviderImpl = cls.newInstance();
		} catch (Exception e) {
			// check what need to be done
			LOG.error("Error Enabling RangerYarnPlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerYarnAuthorizer.init()");
		}
	}

	@Override
	public void init(Configuration conf) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerYarnAuthorizer.init()");
		}

		try {
			activatePluginClassLoader();

			yarnAuthorizationProviderImpl.init(conf);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerYarnAuthorizer.start()");
		}
	}

	@Override
	public boolean checkPermission(AccessRequest accessRequest) {
		
		boolean ret = false;
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerYarnAuthorizer.checkPermission()");
		}

		try {
			activatePluginClassLoader();

			ret = yarnAuthorizationProviderImpl.checkPermission(accessRequest);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerYarnAuthorizer.checkPermission()");
		}
		
		return ret;
	}

	@Override
	public void setPermission(List<Permission> permissions, UserGroupInformation ugi) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerYarnAuthorizer.setPermission()");
		}

		try {
			activatePluginClassLoader();

			yarnAuthorizationProviderImpl.setPermission(permissions, ugi);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerYarnAuthorizer.setPermission()");
		}
	}

	@Override
	public void setAdmins(AccessControlList acls, UserGroupInformation ugi) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerYarnAuthorizer.setAdmins()");
		}

		try {
			activatePluginClassLoader();

			yarnAuthorizationProviderImpl.setAdmins(acls, ugi);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerYarnAuthorizer.setAdmins()");
		}
	}

	@Override
	public boolean isAdmin(UserGroupInformation ugi) {
		
		boolean ret = false;
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerYarnAuthorizer.setAdmins()");
		}

		try {
			activatePluginClassLoader();

			ret = yarnAuthorizationProviderImpl.isAdmin(ugi);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerYarnAuthorizer.setAdmins()");
		}
		
		return ret;
	}
	
	private void activatePluginClassLoader() {
		if(rangerPluginClassLoader != null) {
			rangerPluginClassLoader.activate();
		}
	}

	private void deactivatePluginClassLoader() {
		if(rangerPluginClassLoader != null) {
			rangerPluginClassLoader.deactivate();
		}
	}

	
}
