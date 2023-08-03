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

package org.apache.ranger.authorization.hive.authorizer;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizer;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizerFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzPluginException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveMetastoreClientFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RangerHiveAuthorizerFactory implements HiveAuthorizerFactory {
	
	private static final Logger LOG  = LoggerFactory.getLogger(RangerHiveAuthorizerFactory.class);
	
	private static final String   RANGER_PLUGIN_TYPE                      = "hive";
	private static final String   RANGER_HIVE_AUTHORIZER_IMPL_CLASSNAME   = "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory";

	private HiveAuthorizerFactory 	rangerHiveAuthorizerFactoryImpl		  = null;
	private RangerPluginClassLoader rangerPluginClassLoader 			  = null;

	
	public RangerHiveAuthorizerFactory() {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerHiveAuthorizerFactory.RangerHiveAuthorizerFactory()");
		}

		this.init();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerHiveAuthorizerFactory.RangerHiveAuthorizerFactory()");
		}
	}
	
	public void init(){
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerHiveAuthorizerFactory.init()");
		}

		try {

			rangerPluginClassLoader =  RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());

			@SuppressWarnings("unchecked")
			Class<HiveAuthorizerFactory> cls = (Class<HiveAuthorizerFactory>) Class.forName(RANGER_HIVE_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();
			
			rangerHiveAuthorizerFactoryImpl  = cls.newInstance();

		} catch (Exception e) {
            // check what need to be done
           LOG.error("Error Enabling RangerHivePlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerHiveAuthorizerFactory.init()");
		}
	}
	
	@Override
	public HiveAuthorizer createHiveAuthorizer(HiveMetastoreClientFactory metastoreClientFactory,
											   HiveConf                   conf,
											   HiveAuthenticationProvider hiveAuthenticator,
											   HiveAuthzSessionContext    sessionContext)
													   throws HiveAuthzPluginException {

		HiveAuthorizer ret = null;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerHiveAuthorizerFactory.createHiveAuthorizer()");
		}
		
		try {
			activatePluginClassLoader();
			ret = rangerHiveAuthorizerFactoryImpl.createHiveAuthorizer(metastoreClientFactory, conf, hiveAuthenticator, sessionContext);
		} finally {
			deactivatePluginClassLoader();
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerHiveAuthorizerFactory.createHiveAuthorizer()");
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

