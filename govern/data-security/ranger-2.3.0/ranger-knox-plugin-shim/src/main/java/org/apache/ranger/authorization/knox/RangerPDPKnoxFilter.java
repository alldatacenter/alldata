/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.knox;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerPDPKnoxFilter implements Filter {

	private static final Logger LOG  = LoggerFactory.getLogger(RangerPDPKnoxFilter.class);

	private static final String   RANGER_PLUGIN_TYPE                      = "knox";
	private static final String   RANGER_PDP_KNOX_FILTER_IMPL_CLASSNAME   = "org.apache.ranger.authorization.knox.RangerPDPKnoxFilter";
	
	private Filter                  rangerPDPKnoxFilteImpl  = null;
	private RangerPluginClassLoader rangerPluginClassLoader = null;
	
	public RangerPDPKnoxFilter() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPDPKnoxFilter.RangerPDPKnoxFilter()");
		}

		this.init0();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPDPKnoxFilter.RangerPDPKnoxFilter()");
		}
	}

	private void init0(){
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPDPKnoxFilter.init()");
		}

		try {
			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());
	
			@SuppressWarnings("unchecked")
			Class<Filter> cls = (Class<Filter>) Class.forName(RANGER_PDP_KNOX_FILTER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			rangerPDPKnoxFilteImpl = cls.newInstance();
		} catch (Exception e) {
			// check what need to be done
			LOG.error("Error Enabling RangerKnoxPlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPDPKnoxFilter.init()");
		}
	}

	@Override
	public void destroy() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPDPKnoxFilter.destroy()");
		}

		try {
			activatePluginClassLoader();

			rangerPDPKnoxFilteImpl.destroy();
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPDPKnoxFilter.destroy()");
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPDPKnoxFilter.doFilter()");
		}

		try {
			activatePluginClassLoader();

			rangerPDPKnoxFilteImpl.doFilter(servletRequest, servletResponse, filterChain);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPDPKnoxFilter.doFilter()");
		}
	}

	@Override
	public void init(FilterConfig fiterConfig) throws ServletException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPDPKnoxFilter.init()");
		}

		try {
			activatePluginClassLoader();

			rangerPDPKnoxFilteImpl.init(fiterConfig);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPDPKnoxFilter.init()");
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

