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

package org.apache.ranger.authorization.atlas.authorizer;

import org.apache.atlas.authorize.AtlasAdminAccessRequest;
import org.apache.atlas.authorize.AtlasEntityAccessRequest;
import org.apache.atlas.authorize.AtlasSearchResultScrubRequest;
import org.apache.atlas.authorize.AtlasRelationshipAccessRequest;
import org.apache.atlas.authorize.AtlasTypeAccessRequest;
import org.apache.atlas.authorize.AtlasAuthorizationException;
import org.apache.atlas.authorize.AtlasTypesDefFilterRequest;
import org.apache.atlas.authorize.AtlasAuthorizer;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerAtlasAuthorizer implements AtlasAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(RangerAtlasAuthorizer.class);
    private static boolean isDebugEnabled = LOG.isDebugEnabled();

    private static final String   RANGER_PLUGIN_TYPE                      = "atlas";
	private static final String   RANGER_ATLAS_AUTHORIZER_IMPL_CLASSNAME   = "org.apache.ranger.authorization.atlas.authorizer.RangerAtlasAuthorizer";
	
	private AtlasAuthorizer         rangerAtlasAuthorizerImpl = null;
	private RangerPluginClassLoader rangerPluginClassLoader   = null;

	public RangerAtlasAuthorizer() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAtlasAuthorizer.RangerAtlasAuthorizer()");
		}

		this.init0();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAtlasAuthorizer.RangerAtlasAuthorizer()");
		}
	}

    private void init0() {
        LOG.info("Initializing RangerAtlasPlugin");
        try {			
			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());
			
			@SuppressWarnings("unchecked")
			Class<AtlasAuthorizer> cls = (Class<AtlasAuthorizer>) Class.forName(RANGER_ATLAS_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			rangerAtlasAuthorizerImpl = cls.newInstance();
		} catch (Exception e) {
			// check what need to be done
			LOG.error("Error Enabling RangerAtlasPlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAtlasPlugin.init()");
        }
    }

	@Override
    public void init() {
		 if (isDebugEnabled) {
            LOG.debug("==> RangerAtlasAuthorizer.init");
        }

        try {
			activatePluginClassLoader();

			rangerAtlasAuthorizerImpl.init();
		} finally {
			deactivatePluginClassLoader();
		}

        if (isDebugEnabled) {
            LOG.debug("<== RangerAtlasAuthorizer.init()");
        }

	}

	@Override
	public void cleanUp() {
		if (isDebugEnabled) {
			LOG.debug("cleanUp <===");
		}
		try {
			activatePluginClassLoader();
			rangerAtlasAuthorizerImpl.cleanUp();
		} finally {
			deactivatePluginClassLoader();
		}

	}

	@Override
	public boolean isAccessAllowed(AtlasAdminAccessRequest request) throws AtlasAuthorizationException {
		if (isDebugEnabled) {
			LOG.debug("==> isAccessAllowed(AtlasAdminAccessRequest)");
		}

		final boolean ret;

		try {
			activatePluginClassLoader();

			ret = rangerAtlasAuthorizerImpl.isAccessAllowed(request);
		} finally {
			deactivatePluginClassLoader();
		}

		if (isDebugEnabled) {
			LOG.debug("<== isAccessAllowed(AtlasAdminAccessRequest): " + ret);
		}

		return ret;
	}

	@Override
	public boolean isAccessAllowed(AtlasEntityAccessRequest request) throws AtlasAuthorizationException {
		if (isDebugEnabled) {
			LOG.debug("==> isAccessAllowed(AtlasEntityAccessRequest)");
		}

		final boolean ret;

		try {
			activatePluginClassLoader();

			ret = rangerAtlasAuthorizerImpl.isAccessAllowed(request);
		} finally {
			deactivatePluginClassLoader();
		}

		if (isDebugEnabled) {
			LOG.debug("<== isAccessAllowed(AtlasEntityAccessRequest): " + ret);
		}

		return ret;
	}

	@Override
	public boolean isAccessAllowed(AtlasTypeAccessRequest request) throws AtlasAuthorizationException {
		if (isDebugEnabled) {
			LOG.debug("==> isAccessAllowed(AtlasTypeAccessRequest)");
		}

		final boolean ret;

		try {
			activatePluginClassLoader();

			ret = rangerAtlasAuthorizerImpl.isAccessAllowed(request);
		} finally {
			deactivatePluginClassLoader();
		}

		if (isDebugEnabled) {
			LOG.debug("<== isAccessAllowed(AtlasTypeAccessRequest): " + ret);
		}

		return ret;
	}


	@Override
	public boolean isAccessAllowed(AtlasRelationshipAccessRequest request) throws AtlasAuthorizationException {
		if (isDebugEnabled) {
			LOG.debug("==> isAccessAllowed(AtlasTypeAccessRequest)");
		}

		final boolean ret;

		try {
			activatePluginClassLoader();

			ret = rangerAtlasAuthorizerImpl.isAccessAllowed(request);
		} finally {
			deactivatePluginClassLoader();
		}

		if (isDebugEnabled) {
			LOG.debug("<== isAccessAllowed(AtlasTypeAccessRequest): " + ret);
		}

		return ret;
	}

	@Override
	public void scrubSearchResults(AtlasSearchResultScrubRequest request) throws AtlasAuthorizationException {
		if (isDebugEnabled) {
			LOG.debug("==> scrubSearchResults(" + request + ")");
		}

		try {
			activatePluginClassLoader();

			rangerAtlasAuthorizerImpl.scrubSearchResults(request);
		} finally {
			deactivatePluginClassLoader();
		}

		if (isDebugEnabled) {
			LOG.debug("<== scrubSearchResults(): " + request);
		}
	}

	@Override
	public void filterTypesDef(AtlasTypesDefFilterRequest request) throws AtlasAuthorizationException {

		if (isDebugEnabled) {
			LOG.debug("==> filterTypesDef(" + request + ")");
		}

		try {
			activatePluginClassLoader();

			rangerAtlasAuthorizerImpl.filterTypesDef(request);
		} finally {
			deactivatePluginClassLoader();
		}

		if (isDebugEnabled) {
			LOG.debug("<== filterTypesDef(): " + request);
		}

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
