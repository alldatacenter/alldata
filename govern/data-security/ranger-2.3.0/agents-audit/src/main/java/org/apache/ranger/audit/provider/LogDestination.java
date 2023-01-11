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
package org.apache.ranger.audit.provider;

import org.apache.ranger.audit.model.AuditEventBase;

public interface LogDestination<T> {
	void start();

	void stop();

	boolean isAvailable();

	boolean send(AuditEventBase log) throws AuditMessageException;

	boolean send(AuditEventBase[] logs) throws AuditMessageException;

	boolean sendStringified(String log) throws AuditMessageException;

	boolean sendStringified(String[] logs) throws AuditMessageException;

	boolean flush();

	/**
	 * Name for the destination
	 *
	 * @return
	 */
	String getName();
}
