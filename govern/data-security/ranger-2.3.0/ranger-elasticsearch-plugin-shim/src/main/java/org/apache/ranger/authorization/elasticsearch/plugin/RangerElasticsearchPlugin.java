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

package org.apache.ranger.authorization.elasticsearch.plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import org.apache.ranger.authorization.elasticsearch.plugin.action.filter.RangerSecurityActionFilter;
import org.apache.ranger.authorization.elasticsearch.plugin.rest.filter.RangerSecurityRestFilter;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerElasticsearchPlugin extends Plugin implements ActionPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(RangerElasticsearchPlugin.class);

	private static final String RANGER_ELASTICSEARCH_PLUGIN_CONF_NAME = "ranger-elasticsearch-plugin";

	private final Settings settings;

	private RangerSecurityActionFilter rangerSecurityActionFilter;

	public RangerElasticsearchPlugin(Settings settings) {
		this.settings = settings;
		LOG.debug("settings:"+this.settings);
	}

	@Override
	public List<ActionFilter> getActionFilters() {
		return Collections.singletonList(rangerSecurityActionFilter);
	}

	@Override
	public UnaryOperator<RestHandler> getRestHandlerWrapper(ThreadContext threadContext) {
		return (UnaryOperator<RestHandler>) (handler -> new RangerSecurityRestFilter(threadContext, handler));
	}

	@Override
	public Collection<Object> createComponents(final Client client, final ClusterService clusterService,
			final ThreadPool threadPool, final ResourceWatcherService resourceWatcherService,
			final ScriptService scriptService, final NamedXContentRegistry xContentRegistry,
			final Environment environment, final NodeEnvironment nodeEnvironment,
			final NamedWriteableRegistry namedWriteableRegistry) {

		addPluginConfig2Classpath(environment);

		rangerSecurityActionFilter = new RangerSecurityActionFilter(threadPool.getThreadContext());
		return Collections.singletonList(rangerSecurityActionFilter);
	}

	/**
	 * Add ranger elasticsearch plugin config directory to classpath,
	 * then the plugin can load its configuration files from classpath.
	 */
	private void addPluginConfig2Classpath(Environment environment) {
		Path configPath = environment.configFile().resolve(RANGER_ELASTICSEARCH_PLUGIN_CONF_NAME);
		if (configPath == null) {
			LOG.error(
					"Failed to add ranger elasticsearch plugin config directory [ranger-elasticsearch-plugin] to classpath.");
			return;
		}
		File configFile = configPath.toFile();

		try {
			if (configFile.exists()) {
				ClassLoader classLoader = this.getClass().getClassLoader();
				// This classLoader is FactoryURLClassLoader in elasticsearch
				if (classLoader instanceof URLClassLoader) {
					URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
					Class<? extends URLClassLoader> urlClass = urlClassLoader.getClass();
					Method method = urlClass.getSuperclass().getDeclaredMethod("addURL", new Class[] { URL.class });
					method.setAccessible(true);
					method.invoke(urlClassLoader, new Object[] { configFile.toURI().toURL() });
					LOG.info("Success to add ranger elasticsearch plugin config directory [{}] to classpath.",
							configFile.getCanonicalPath());
				}
			}
		} catch (Exception e) {
			LOG.error(
					"Failed to add ranger elasticsearch plugin config directory [ranger-elasticsearch-plugin] to classpath.",
					e);
			throw new RuntimeException(e);
		}
	}
}
