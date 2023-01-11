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

package org.apache.ranger.authorization.storm.authorizer;



import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;

import org.apache.storm.security.auth.IAuthorizer;
import org.apache.storm.security.auth.ReqContext;

public class RangerStormAuthorizer implements IAuthorizer {
	private static final Logger LOG  = LoggerFactory.getLogger(RangerStormAuthorizer.class);

	private static final String   RANGER_PLUGIN_TYPE                      = "storm";
	private static final String   RANGER_STORM_AUTHORIZER_IMPL_CLASSNAME   = "org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer";

	private IAuthorizer 	        rangerStormAuthorizerImpl = null;
	private RangerPluginClassLoader rangerPluginClassLoader   = null;
	
	public RangerStormAuthorizer() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerStormAuthorizer.RangerStormAuthorizer()");
		}

		this.init();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerStormAuthorizer.RangerStormAuthorizer()");
		}
	}
	
	private void init(){
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerStormAuthorizer.init()");
		}

		try {
			
			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());
			
			@SuppressWarnings("unchecked")
			Class<IAuthorizer> cls = (Class<IAuthorizer>) Class.forName(RANGER_STORM_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			rangerStormAuthorizerImpl = cls.newInstance();
		} catch (Exception e) {
			// check what need to be done
			LOG.error("Error Enabling RangerStormPlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerStormAuthorizer.init()");
		}
	}

	@Override
	public void prepare(Map storm_conf) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerStormAuthorizer.prepare()");
		}

		try {
			activatePluginClassLoader();

			rangerStormAuthorizerImpl.prepare(storm_conf);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerStormAuthorizer.prepare()");
		}

	}

	@Override
	public boolean permit(ReqContext context, String operation, Map topology_conf) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerStormAuthorizer.permit()");
		}
		
		boolean ret = false;

		try {
			activatePluginClassLoader();

			ret = rangerStormAuthorizerImpl.permit(context, operation, topology_conf);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerStormAuthorizer.permit()");
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
