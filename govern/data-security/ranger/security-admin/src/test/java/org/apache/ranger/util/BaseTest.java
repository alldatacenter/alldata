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

 /**
 *
 */
package org.apache.ranger.util;

import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml",
		"classpath:asynctask-applicationContext.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class,
		TransactionalTestExecutionListener.class })
abstract public class BaseTest {

	/**
	 * MockHttpSession, SubStitute of HttpSession
	 */
	protected MockHttpSession session;
	/**
	 * MockHttpServletRequest, SubStitute of HttpServletRequest
	 */
	protected MockHttpServletRequest request;

	public BaseTest() {
		init();
	}

	public void authenticate() throws Exception {
		CLIUtil cliUtil = (CLIUtil) CLIUtil.getBean(CLIUtil.class);
		cliUtil.authenticate();
	}

	/*
	 * Start New MockHttpSession
	 */
	protected void startSession() {
		session = new MockHttpSession();
	}

	/*
	 * Destroy MockHttpSession, if exists
	 */
	protected void endSession() {
		if (session != null) {
			session.clearAttributes();
		}
		session = null;
	}

	/*
	 * Create New MockHttpServletRequest
	 */
	protected MockHttpServletRequest startRequest() {
		request = new MockHttpServletRequest();
		request.setSession(session);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
				request));
		return request;
	}

	/*
	 * terminate existing MockHttpServletRequest
	 */
	protected void endRequest() {
		((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.requestCompleted();
		RequestContextHolder.resetRequestAttributes();
		request = null;
	}

	public void init() {

	}

}
