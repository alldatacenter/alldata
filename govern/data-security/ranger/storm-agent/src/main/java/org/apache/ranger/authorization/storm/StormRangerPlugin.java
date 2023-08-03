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

package org.apache.ranger.authorization.storm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.ranger.authorization.storm.StormRangerPlugin.StormConstants.PluginConfiguration;
import org.apache.ranger.authorization.storm.StormRangerPlugin.StormConstants.ResourceName;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class StormRangerPlugin extends RangerBasePlugin {
	
	private static final Logger LOG = LoggerFactory.getLogger(StormRangerPlugin.class);
	boolean initialized = false;

	private final Map<String,String> impliedAccessTypes;

	public StormRangerPlugin() {
		super(PluginConfiguration.ServiceType, PluginConfiguration.AuditApplicationType);

		Map<String, String> impliedTypes = new HashMap<String, String>();
		// In future this has to be part of Ranger Storm Service Def.
		impliedTypes.put("getTopologyPageInfo","getTopologyInfo");
		impliedTypes.put("getComponentPageInfo","getTopologyInfo");
		impliedTypes.put("setWorkerProfiler","getTopologyInfo");
		impliedTypes.put("getWorkerProfileActionExpiry","getTopologyInfo");
		impliedTypes.put("getComponentPendingProfileActions","getTopologyInfo");
		impliedTypes.put("startProfiling","getTopologyInfo");
		impliedTypes.put("stopProfiling","getTopologyInfo");
		impliedTypes.put("dumpProfile","getTopologyInfo");
		impliedTypes.put("dumpJstack","getTopologyInfo");
		impliedTypes.put("dumpHeap","getTopologyInfo");
		impliedTypes.put("setLogConfig","getTopologyInfo");
		impliedTypes.put("getLogConfig","getTopologyInfo");
		impliedTypes.put("debug","getTopologyInfo");

		this.impliedAccessTypes = Collections.unmodifiableMap(impliedTypes);
	}

	// this method isn't expected to be invoked often.  Per knox design this would be invoked ONCE right after the authorizer servlet is loaded
	@Override
	synchronized public void init() {
		if (!initialized) {
			// mandatory call to base plugin
			super.init();
			// One time call to register the audit hander with the policy engine.
			super.setResultProcessor(new RangerDefaultAuditHandler(getConfig()));
			// this needed to set things right in the nimbus process
			if (KerberosName.getRules() == null) {
				KerberosName.setRules("DEFAULT");
			}

			initialized = true;
			LOG.info("StormRangerPlugin initialized!");
		}
	}

	public RangerAccessRequest buildAccessRequest(String _user, String[] _groups, String _clientIp, String _topology, String _operation) {
		
		RangerAccessRequestImpl request = new RangerAccessRequestImpl();
		request.setUser(_user);
		if (_groups != null && _groups.length > 0) {
			Set<String> groups = Sets.newHashSet(_groups);
			request.setUserGroups(groups);
		}

		request.setAccessType(getAccessType(_operation));
		request.setClientIPAddress(_clientIp);
		request.setAction(_operation);
		// build resource and connect stuff into request
		RangerAccessResourceImpl resource = new RangerAccessResourceImpl();
		resource.setValue(ResourceName.Topology, _topology);
		request.setResource(resource);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Returning request: " + request.toString());
		}
		
		return request;
	}

	private String getAccessType(String _operation) {
		String ret = null;
		ret = impliedAccessTypes.get(_operation);
		if ( ret == null) {
			ret = _operation;
		}
		return ret;
	}

	static public class StormConstants {
		// Plugin parameters
		static class PluginConfiguration {
			static final String ServiceType = "storm";
			static final String AuditApplicationType = "storm";
		}
		
		// must match the corresponding string used in service definition file
		static class ResourceName {
			static final String Topology = "topology";
		}
	}

}
