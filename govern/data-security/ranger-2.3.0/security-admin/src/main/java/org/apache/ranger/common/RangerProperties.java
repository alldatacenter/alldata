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

package org.apache.ranger.common;

import java.util.HashMap;

import org.apache.ranger.plugin.util.XMLUtils;

public class RangerProperties extends HashMap<Object, Object> {
	
	private static final long serialVersionUID = -4094378755892810987L;

	private static final String XMLCONFIG_FILENAME_DELIMITOR = ",";

	private String xmlConfigFileNames = null;

	public RangerProperties(String xmlConfigFileNames) {
		this.xmlConfigFileNames = xmlConfigFileNames;
		initProperties();
	}

	private void initProperties() {
		
		if (xmlConfigFileNames == null || xmlConfigFileNames.isEmpty()) {
			return;
		}

		String[] fnList = xmlConfigFileNames.split(XMLCONFIG_FILENAME_DELIMITOR);

		for (String fn : fnList) {
		    XMLUtils.loadConfig(fn, this);
		}

	}

	
}
