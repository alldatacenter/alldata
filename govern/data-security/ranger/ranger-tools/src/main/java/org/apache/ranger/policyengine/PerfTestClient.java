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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.ranger.plugin.policyengine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

public class PerfTestClient extends Thread {
	static final Logger LOG      = LoggerFactory.getLogger(PerfTestClient.class);

	final PerfTestEngine perfTestEngine;
	final int clientId;
	final URL requestFileURL;
	final int maxCycles;

	List<RequestData> requests = null;
	private static Gson gson  = null;

	static {
		GsonBuilder builder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z");
		gson = builder
				.setPrettyPrinting()
				.registerTypeAdapter(RangerAccessRequest.class, new RangerAccessRequestDeserializer(builder))
				.registerTypeAdapter(RangerAccessResource.class, new RangerResourceDeserializer(builder))
				.create();
	}

	public PerfTestClient(final PerfTestEngine perfTestEngine, final int clientId,  final URL requestFileURL, final int maxCycles) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> PerfTestClient(clientId=" + clientId + ", maxCycles=" + maxCycles +")" );
		}

		this.perfTestEngine = perfTestEngine;
		this.clientId = clientId;
		this.requestFileURL = requestFileURL;
		this.maxCycles = maxCycles;

		setName("PerfTestClient-" + clientId);
		setDaemon(true);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== PerfTestClient(clientId=" + clientId + ", maxCycles=" + maxCycles +")" );
		}
	}

	public boolean init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> init()" );
		}

		boolean ret = false;

		Reader reader = null;

		try {

			InputStream in = requestFileURL.openStream();

			reader = new InputStreamReader(in, Charset.forName("UTF-8"));

			Type listType = new TypeToken<List<RequestData>>() {
			}.getType();

			requests = gson.fromJson(reader, listType);

			ret = true;
		}
		catch (Exception excp) {
			LOG.error("Error opening request data stream or loading load request data from file, URL=" + requestFileURL, excp);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception excp) {
					LOG.error("Error closing file ", excp);
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== init() : " + ret );
		}
		return ret;
	}

	@Override
	public void run() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> run()" );
		}

		try {
			for (int i = 0; i < maxCycles; i++) {
				for (RequestData data : requests) {
					data.setResult(perfTestEngine.execute(data.getRequest()));
				}
			}
		} catch (Exception excp) {
			LOG.error("PerfTestClient.run() : interrupted! Exiting thread", excp);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== run()" );
		}
	}

	private static class RequestData {
		private String              name;
		private RangerAccessRequest request;
		private RangerAccessResult result;

		public RequestData() {
			this(null, null, null);
		}

		public RequestData(String name, RangerAccessRequest request, RangerAccessResult result) {
			setName(name);
			setRequest(request);
			setResult(result);
		}

		public String getName() {return name;}
		public RangerAccessRequest getRequest() { return request;}
		public RangerAccessResult getResult() { return result;}

		public void setName(String name) { this.name = name;}
		public void setRequest(RangerAccessRequest request) { this.request = request;}
		public void setResult(RangerAccessResult result) { this.result = result;}
	}
}
