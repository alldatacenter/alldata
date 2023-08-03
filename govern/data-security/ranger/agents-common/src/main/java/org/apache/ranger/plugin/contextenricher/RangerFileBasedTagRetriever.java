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

package org.apache.ranger.plugin.contextenricher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.util.ServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public class RangerFileBasedTagRetriever extends RangerTagRetriever {
	private static final Logger LOG = LoggerFactory.getLogger(RangerFileBasedTagRetriever.class);


	private URL serviceTagsFileURL;
	private String serviceTagsFileName;
	private Gson gsonBuilder;
	private boolean deDupTags;

	@Override
	public void init(Map<String, String> options) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> init()" );
		}

		gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
				.setPrettyPrinting()
				.create();

		String serviceTagsFileNameProperty = "serviceTagsFileName";
		String serviceTagsDefaultFileName = "/testdata/test_servicetags_hive.json";
		String deDupTagsProperty          = "deDupTags";

		if (StringUtils.isNotBlank(serviceName) && serviceDef != null && StringUtils.isNotBlank(appId)) {
			InputStream serviceTagsFileStream = null;


			// Open specified file from options- it should contain service-tags

			serviceTagsFileName = options != null? options.get(serviceTagsFileNameProperty) : null;
			String deDupTagsVal = options != null? options.get(deDupTagsProperty) : "false";
			deDupTags           = Boolean.parseBoolean(deDupTagsVal);

			serviceTagsFileName = serviceTagsFileName == null ? serviceTagsDefaultFileName : serviceTagsFileName;

			File f = new File(serviceTagsFileName);

			if (f.exists() && f.isFile() && f.canRead()) {
				try {
					serviceTagsFileStream = new FileInputStream(f);
					serviceTagsFileURL = f.toURI().toURL();
				} catch (FileNotFoundException exception) {
					LOG.error("Error processing input file:" + serviceTagsFileName + " or no privilege for reading file " + serviceTagsFileName, exception);
				} catch (MalformedURLException malformedException) {
					LOG.error("Error processing input file:" + serviceTagsFileName + " cannot be converted to URL " + serviceTagsFileName, malformedException);
				}
			} else {
				URL fileURL = getClass().getResource(serviceTagsFileName);
				if (fileURL == null && !serviceTagsFileName.startsWith("/")) {
					fileURL = getClass().getResource("/" + serviceTagsFileName);
				}

				if (fileURL == null) {
					fileURL = ClassLoader.getSystemClassLoader().getResource(serviceTagsFileName);
					if (fileURL == null && !serviceTagsFileName.startsWith("/")) {
						fileURL = ClassLoader.getSystemClassLoader().getResource("/" + serviceTagsFileName);
					}
				}

				if (fileURL != null) {
					try {
						serviceTagsFileStream = fileURL.openStream();
						serviceTagsFileURL = fileURL;
					} catch (Exception exception) {
						LOG.error(serviceTagsFileName + " is not a file", exception);
					}
				} else {
					LOG.warn("Error processing input file: URL not found for " + serviceTagsFileName + " or no privilege for reading file " + serviceTagsFileName);
				}
			}

			if (serviceTagsFileStream != null) {
				try {
					serviceTagsFileStream.close();
				} catch (Exception e) {
					// Ignore
				}
			}

		} else {
			LOG.error("FATAL: Cannot find service/serviceDef/serviceTagsFile to use for retrieving tags. Will NOT be able to retrieve tags.");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== init() : serviceTagsFileName=" + serviceTagsFileName);
		}
	}

	@Override
	public ServiceTags retrieveTags(long lastKnownVersion, long lastActivationTimeInMillis) throws Exception {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> retrieveTags(lastKnownVersion=" + lastKnownVersion + ", lastActivationTimeInMillis=" + lastActivationTimeInMillis + ", serviceTagsFilePath=" + serviceTagsFileName);
		}

		ServiceTags serviceTags = null;

		if (serviceTagsFileURL != null) {
			try (
				InputStream serviceTagsFileStream = serviceTagsFileURL.openStream();
				Reader reader = new InputStreamReader(serviceTagsFileStream, Charset.forName("UTF-8"))
			) {

				serviceTags = gsonBuilder.fromJson(reader, ServiceTags.class);

				if (serviceTags.getTagVersion() <= lastKnownVersion) {
					// No change in serviceTags
					serviceTags = null;
				} else {
					if (deDupTags) {
						final int countOfDuplicateTags = serviceTags.dedupTags();
						LOG.info("Number of duplicate tags removed from the received serviceTags:[" + countOfDuplicateTags + "]. Number of tags in the de-duplicated serviceTags :[" + serviceTags.getTags().size() + "].");
					}
				}
			} catch (IOException e) {
				LOG.warn("Error processing input file: or no privilege for reading file " + serviceTagsFileName);
				throw e;
			}
		} else {
			LOG.error("Error reading file: " + serviceTagsFileName);
			throw new Exception("serviceTagsFileURL is null!");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== retrieveTags(lastKnownVersion=" + lastKnownVersion + ", lastActivationTimeInMillis=" + lastActivationTimeInMillis);
		}

		return serviceTags;
	}

}

