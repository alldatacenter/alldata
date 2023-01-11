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

package org.apache.ranger.policyengine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.*;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

public class PerfTestEngine {
	static final Logger LOG      = LoggerFactory.getLogger(PerfTestEngine.class);

	private final URL servicePoliciesFileURL;
	private final RangerPolicyEngineOptions policyEngineOptions;
	private final URL configFileURL;
	private RangerPolicyEngine policyEvaluationEngine;
	private RangerPluginContext rangerPluginContext;

	public PerfTestEngine(final URL servicePoliciesFileURL, RangerPolicyEngineOptions policyEngineOptions, URL configFileURL) {
		this.servicePoliciesFileURL = servicePoliciesFileURL;
		this.policyEngineOptions = policyEngineOptions;
		this.configFileURL = configFileURL;
	}

	public boolean init() {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> init()");
		}

		boolean ret = false;

		Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
				.setPrettyPrinting()
				.create();

		Reader reader = null;
		ServicePolicies servicePolicies;

		try {
			InputStream in = servicePoliciesFileURL.openStream();

			reader = new InputStreamReader(in, Charset.forName("UTF-8"));

			servicePolicies = gsonBuilder.fromJson(reader, ServicePolicies.class);
			RangerServiceDef serviceDef = servicePolicies.getServiceDef();
			String serviceType = (serviceDef != null) ? serviceDef.getName() : "";
			rangerPluginContext = new RangerPluginContext(new RangerPluginConfig(serviceType, null, "perf-test", null, null, policyEngineOptions));
			rangerPluginContext.getConfig().addResource(configFileURL);
			policyEvaluationEngine = new RangerPolicyEngineImpl(servicePolicies, rangerPluginContext, null);

			ret = true;

		} catch (Exception excp) {
			LOG.error("Error opening service-policies file or loading service-policies from file, URL=" + servicePoliciesFileURL, excp);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception excp) {
					LOG.error("Error closing file", excp);
				}
			}
		}


		if (LOG.isDebugEnabled()) {
			LOG.debug("<== init() : " + ret);
		}

		return ret;

	}
	public RangerAccessResult execute(final RangerAccessRequest request) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> execute(" + request + ")");
		}

		RangerAccessResult ret = null;

		if (policyEvaluationEngine != null) {

			ret = policyEvaluationEngine.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_ACCESS, null);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Executed request = {" + request + "}, result={" + ret + "}");
			}
		} else {
			LOG.error("Error executing request: PolicyEngine is null!");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== execute(" + request + ") : " + ret);
		}

		return ret;
	}

	public void cleanUp() {
		if (policyEvaluationEngine != null) {
			((RangerPolicyEngineImpl)policyEvaluationEngine).releaseResources(true);
			policyEvaluationEngine = null;
		}
	}

}
