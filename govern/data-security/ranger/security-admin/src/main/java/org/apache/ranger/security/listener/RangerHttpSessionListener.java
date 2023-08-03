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

package org.apache.ranger.security.listener;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class RangerHttpSessionListener implements HttpSessionListener {

	private static CopyOnWriteArrayList<HttpSession> listOfSession = new CopyOnWriteArrayList<HttpSession>();

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		listOfSession.add(event.getSession());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		if (!listOfSession.isEmpty()) {
			listOfSession.remove(event.getSession());
		}
	}

	public static CopyOnWriteArrayList<HttpSession> getActiveSessionOnServer() {
		return listOfSession;
	}

}
