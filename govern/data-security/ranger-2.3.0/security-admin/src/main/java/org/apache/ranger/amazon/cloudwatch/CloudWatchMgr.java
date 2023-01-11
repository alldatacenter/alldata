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

package org.apache.ranger.amazon.cloudwatch;

import static org.apache.ranger.audit.destination.AmazonCloudWatchAuditDestination.CONFIG_PREFIX;
import static org.apache.ranger.audit.destination.AmazonCloudWatchAuditDestination.PROP_REGION;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.ranger.common.PropertiesUtil;
import org.springframework.stereotype.Component;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;

/**
 * This class initializes the CloudWatch client
 *
 */
@Component
public class CloudWatchMgr {

	private static final Logger LOGGER = Logger.getLogger(CloudWatchMgr.class);

	private AWSLogs client = null;
	private String regionName;

	synchronized void connect() {
		if (client == null) {
			synchronized (CloudWatchMgr.class) {
				if (client == null) {
					regionName = PropertiesUtil.getProperty(CONFIG_PREFIX + "." + PROP_REGION);
					try {
						client = newClient();
					} catch (Throwable t) {
						LOGGER.fatal("Can't connect to CloudWatch region: " + regionName, t);
					}
				}
			}
		}
	}

	public AWSLogs getClient() {
		if (client == null) {
			synchronized (CloudWatchMgr.class) {
				if (client == null) {
					connect();
				}
			}
		}
		return client;
	}

	private AWSLogs newClient() {
		if (StringUtils.isBlank(regionName)) {
			return AWSLogsClientBuilder.standard().build();
		}
		return AWSLogsClientBuilder.standard().withRegion(regionName).build();
	}
}
