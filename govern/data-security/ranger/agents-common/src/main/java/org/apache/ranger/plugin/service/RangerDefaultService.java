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

package org.apache.ranger.plugin.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerDefaultService extends RangerBaseService {
	private static final Logger LOG = LoggerFactory.getLogger(RangerDefaultService.class);
		
	@Override
	public  Map<String, Object> validateConfig() throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("RangerDefaultService.validateConfig Service: (" + serviceName + " ), returning empty map");
		}
		return MapUtils.EMPTY_MAP;
	}
	
	@Override
	public List<String> lookupResource(ResourceLookupContext context) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("RangerDefaultService.lookupResource Context: (" + context + "), returning empty list");
		}
		return ListUtils.EMPTY_LIST;
	}

}
