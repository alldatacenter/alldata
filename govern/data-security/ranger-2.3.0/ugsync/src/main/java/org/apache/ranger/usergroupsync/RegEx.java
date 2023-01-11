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

package org.apache.ranger.usergroupsync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;

public class RegEx extends AbstractMapper {
	private UserGroupSyncConfig config = UserGroupSyncConfig.getInstance();
	private LinkedHashMap<String, String> replacementPattern;

	public LinkedHashMap<String, String> getReplacementPattern() {
		return replacementPattern;
	}

	@Override
	public void init (String baseProperty) {
		logger.info("Initializing for " + baseProperty);
		try {
			List<String> regexPatterns = config.getAllRegexPatterns(baseProperty);
			populateReplacementPatterns(baseProperty, regexPatterns);
		} catch (Throwable t) {
			logger.error("Failed to initialize " + baseProperty, t.fillInStackTrace());
		}
	}

	protected void populateReplacementPatterns(String baseProperty, List<String> regexPatterns) throws Throwable{
		replacementPattern = new LinkedHashMap<String, String>();
		Pattern p = Pattern.compile("s/([^/]*)/([^/]*)/(g)?");
		for (String regexPattern : regexPatterns) {
			Matcher m = p.matcher(regexPattern);
			if (!m.matches()) {
				logger.warn("Invalid RegEx " + regexPattern + " and hence skipping this regex property");
			}
			m = m.reset();
			while (m.find()) {
				String matchPattern = m.group(1);
				String replacement = m.group(2);
				if (matchPattern != null && !matchPattern.isEmpty() && replacement != null) {
					replacementPattern.put(matchPattern, replacement);
					if (logger.isDebugEnabled()) {
						logger.debug(baseProperty + " match pattern = " + matchPattern + " and replacement string = " + replacement);
					}
				}
			}
		}
	}

	@Override
	public String transform (String attrValue) {
		String result = attrValue;
		try {
			if (replacementPattern != null && !replacementPattern.isEmpty()) {
				for (String matchPattern : replacementPattern.keySet()) {
					Pattern p = Pattern.compile(matchPattern);
					Matcher m = p.matcher(result);
					if (m.find()) {
						String replacement = replacementPattern.get(matchPattern);
						if (replacement != null) {
							result = m.replaceAll(replacement);
						}
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Failed to transform " + attrValue, t.fillInStackTrace());
		}
		return result;
	}
}
