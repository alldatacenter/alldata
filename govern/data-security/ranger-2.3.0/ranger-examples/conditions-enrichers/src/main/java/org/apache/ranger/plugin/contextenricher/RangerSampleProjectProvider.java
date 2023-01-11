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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

/**
 * This is a sample implementation of a Context Enricher.  It works in conjunction with a sample Condition Evaluator
 * <code>RangerSampleSimpleMatcher</code>. It This is how it would be used in service definition:
 {
    ... service def
    ...
    "contextEnrichers": [
		{
		 "itemId": 1, "name": "project-provider",
		 "enricher": "org.apache.ranger.plugin.contextenricher.RangerSampleProjectProvider",
		 "enricherOptions": { "contextName" : "PROJECT", "dataFile":"/etc/ranger/data/userProject.txt"}
		}
 	...
 }

 contextName: is used to specify the name under which the enricher would push value into context.
           For purposes of this example the default value of this parameter, if unspecified is PROJECT.  This default
           can be seen specified in <code>init()</code>.
 dataFile: is the file which contains the lookup data that this particular enricher would use to
           ascertain which value to insert into the context.  For purposes of this example the default value of
           this parameter, if unspecified is /etc/ranger/data/userProject.txt.  This default can be seen specified
           in <code>init()</code>.  Format of lookup data is in the form of standard java properties list.

 @see <a href="http://docs.oracle.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)">Java Properties List</a>
 */
public class RangerSampleProjectProvider extends RangerAbstractContextEnricher {
	private static final Logger LOG = LoggerFactory.getLogger(RangerSampleProjectProvider.class);

	private String     contextName    = "PROJECT";
	private Properties userProjectMap = null;
	
	@Override
	public void init() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSampleProjectProvider.init(" + enricherDef + ")");
		}
		
		super.init();
		
		contextName = getOption("contextName", "PROJECT");

		String dataFile = getOption("dataFile", "/etc/ranger/data/userProject.txt");

		userProjectMap = readProperties(dataFile);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSampleProjectProvider.init(" + enricherDef + ")");
		}
	}

	@Override
	public void enrich(RangerAccessRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSampleProjectProvider.enrich(" + request + ")");
		}
		
		if(request != null && userProjectMap != null && request.getUser() != null) {
			Map<String, Object> context = request.getContext();
			String              project = userProjectMap.getProperty(request.getUser());
	
			if(context != null && !StringUtils.isEmpty(project)) {
				request.getContext().put(contextName, project);
			} else {
				if(LOG.isDebugEnabled()) {
					LOG.debug("RangerSampleProjectProvider.enrich(): skipping due to unavailable context or project. context=" + context + "; project=" + project);
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSampleProjectProvider.enrich(" + request + ")");
		}
	}
}
