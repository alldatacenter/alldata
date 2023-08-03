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

package org.apache.ranger.audit.destination;

import java.util.Properties;

import org.apache.ranger.audit.provider.BaseAuditHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class needs to be extended by anyone who wants to build custom
 * destination
 */
public abstract class AuditDestination extends BaseAuditHandler {
	private static final Logger logger = LoggerFactory.getLogger(AuditDestination.class);

	public AuditDestination() {
		logger.info("AuditDestination() enter");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#init(java.util.Properties,
	 * java.lang.String)
	 */
	@Override
	public void init(Properties prop, String basePropertyName) {
		super.init(prop, basePropertyName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#flush()
	 */
	@Override
	public void flush() {

	}

	@Override
	public void start() {
		
	}

	@Override
	public void stop() {
		
	}

	@Override
	public void waitToComplete() {
		
	}

	@Override
	public void waitToComplete(long timeout) {
		
	}
	
}
