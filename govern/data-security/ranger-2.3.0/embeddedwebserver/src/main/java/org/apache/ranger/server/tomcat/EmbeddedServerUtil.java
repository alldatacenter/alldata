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

package org.apache.ranger.server.tomcat;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.util.XMLUtils;

public class EmbeddedServerUtil {

	private static final Logger LOG = Logger.getLogger(EmbeddedServerUtil.class.getName());
	private static final String CONFIG_FILE = "ranger-admin-site.xml";
	private static final String CORE_SITE_CONFIG_FILENAME = "core-site.xml";
	private static final String DEFAULT_CONFIG_FILENAME = "ranger-admin-default-site.xml";
	private static Properties rangerConfigProperties = new Properties();

	private EmbeddedServerUtil() {
		loadRangerConfigProperties(CONFIG_FILE);
	}

	public static void loadRangerConfigProperties(String configFile) {
		if (CONFIG_FILE.equalsIgnoreCase(configFile)) {
			XMLUtils.loadConfig(DEFAULT_CONFIG_FILENAME, rangerConfigProperties);
		}
		XMLUtils.loadConfig(CORE_SITE_CONFIG_FILENAME, rangerConfigProperties);
		XMLUtils.loadConfig(configFile, rangerConfigProperties);
	}

	public static Properties getRangerConfigProperties() {
		if (rangerConfigProperties.isEmpty()) {
			loadRangerConfigProperties(CONFIG_FILE);
		}
		return rangerConfigProperties;
	}

	public static String getConfig(String key, String defaultValue) {
		String ret = getConfig(key);
		if (ret == null) {
			ret = defaultValue;
		}
		return ret;
	}

	public static boolean getBooleanConfig(String key, boolean defaultValue) {
		boolean ret = defaultValue;
		String retStr = getConfig(key);
		try {
			if (retStr != null) {
				ret = Boolean.parseBoolean(retStr);
			}
		} catch (Exception err) {
			LOG.severe(retStr + " can't be parsed to int. Reason: " + err.toString());
		}
		return ret;
	}

	public static int getIntConfig(String key, int defaultValue) {
		int ret = defaultValue;
		String retStr = getConfig(key);
		try {
			if (retStr != null) {
				ret = Integer.parseInt(retStr);
			}
		} catch (Exception err) {
			LOG.severe(retStr + " can't be parsed to int. Reason: " + err.toString());
		}
		return ret;
	}

	public static Long getLongConfig(String key, Long defaultValue) {
		Long ret = defaultValue;
		String retStr = getConfig(key);
		try {
			if (retStr != null) {
				ret = Long.parseLong(retStr);
			}
		} catch (Exception err) {
			LOG.severe(retStr + " can't be parsed to long. Reason: " + err.toString());
		}
		return ret;
	}

	public static String getConfig(String key) {
		String value = getRangerConfigProperties().getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			value = System.getProperty(key);
		}
		return value;
	}

	public static String getHosts(String urls) {
		if (urls != null) {
			urls = urls.trim();
			if ("NONE".equalsIgnoreCase(urls)) {
				urls = null;
			}
		}
		return urls;
	}

	public static List<String> toArray(String destListStr, String delim) {
		List<String> list = new ArrayList<String>();
		if (StringUtils.isNotBlank(destListStr)) {
			StringTokenizer tokenizer = new StringTokenizer(destListStr, delim.trim());
			while (tokenizer.hasMoreTokens()) {
				list.add(tokenizer.nextToken());
			}
		}
		return list;
	}

}
