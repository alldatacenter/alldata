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

package org.apache.ranger.services.storm.client;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StormConnectionMgr {

	private static final Logger LOG = LoggerFactory.getLogger(StormConnectionMgr.class);

	public static StormClient getStormClient(final String stormUIURL, String userName, String password, String lookupPrincipal, String lookupKeytab, String nameRules) {
        StormClient stormClient = null;
        if (stormUIURL == null || stormUIURL.isEmpty()) {
        	LOG.error("Can not create StormClient: stormUIURL is empty");
        } else if(StringUtils.isEmpty(lookupPrincipal) || StringUtils.isEmpty(lookupKeytab)){
        	if (userName == null || userName.isEmpty()) {
        		LOG.error("Can not create StormClient: stormAdminUser is empty");
        	} else if (password == null || password.isEmpty()) {
        		LOG.error("Can not create StormClient: stormAdminPassword is empty");
        	}
        }else {
            stormClient =  new StormClient(stormUIURL, userName, password, lookupPrincipal, lookupKeytab, nameRules);
        }
        return stormClient;
    }

}
