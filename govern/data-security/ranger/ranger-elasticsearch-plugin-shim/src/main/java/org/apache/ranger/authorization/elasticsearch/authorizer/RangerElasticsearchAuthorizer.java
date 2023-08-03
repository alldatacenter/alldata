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

package org.apache.ranger.authorization.elasticsearch.authorizer;

import java.util.List;

import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RangerElasticsearchAuthorizer {

	private static final Logger LOG = LoggerFactory.getLogger(RangerElasticsearchAuthorizer.class);

	private static final String RANGER_PLUGIN_TYPE = "elasticsearch";

	private static final String RANGER_ELASTICSEARCH_AUTHORIZER_IMPL_CLASSNAME = "org.apache.ranger.authorization.elasticsearch.authorizer.RangerElasticsearchAuthorizer";

	private RangerPluginClassLoader          rangerPluginClassLoader          = null;
	private ClassLoader                      esClassLoader                    = null;
	private RangerElasticsearchAccessControl rangerElasticsearchAccessControl = null;

	public RangerElasticsearchAuthorizer() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerElasticsearchAuthorizer.RangerElasticsearchAuthorizer()");
		}

		this.init();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerElasticsearchAuthorizer.RangerElasticsearchAuthorizer()");
		}
	}

	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerElasticsearchAuthorizer.init()");
		}

		try {

			// In elasticsearch this.getClass().getClassLoader() is FactoryURLClassLoader,
			// but Thread.currentThread().getContextClassLoader() is AppClassLoader.
			esClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());
			Thread.currentThread().setContextClassLoader(esClassLoader);

			@SuppressWarnings("unchecked")
			Class<RangerElasticsearchAccessControl> cls = (Class<RangerElasticsearchAccessControl>) Class
					.forName(RANGER_ELASTICSEARCH_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);
			activatePluginClassLoader();
			rangerElasticsearchAccessControl = cls.newInstance();
		} catch (Exception e) {
			LOG.error("Error Enabling RangerElasticsearchAuthorizer", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerElasticsearchAuthorizer.init()");
		}
	}

	public boolean checkPermission(String user, List<String> groups, String index, String action,
			String clientIPAddress) {
		boolean ret = false;

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerElasticsearchAuthorizer.checkPermission()");
		}

		try {
			activatePluginClassLoader();

			ret = rangerElasticsearchAccessControl.checkPermission(user, groups, index, action, clientIPAddress);
		} finally {
			deactivatePluginClassLoader();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerElasticsearchAuthorizer.checkPermission()");
		}

		return ret;
	}

	private void activatePluginClassLoader() {
		if (rangerPluginClassLoader != null) {
			Thread.currentThread().setContextClassLoader(rangerPluginClassLoader);
		}
	}

	private void deactivatePluginClassLoader() {
		if (esClassLoader != null) {
			Thread.currentThread().setContextClassLoader(esClassLoader);
		}
	}
}
