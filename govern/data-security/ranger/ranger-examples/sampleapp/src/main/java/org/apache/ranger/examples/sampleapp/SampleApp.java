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

package org.apache.ranger.examples.sampleapp;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleApp {
	private static final Logger LOG = LoggerFactory.getLogger(SampleApp.class);

	private static final Set<String> VALID_ACCESS_TYPES = new HashSet<String>();

	private IAuthorizer authorizer = null;

	public static void main(String[] args) {
		SampleApp app = new SampleApp();

		app.init();

		app.run();
	}

	public void init() {
		VALID_ACCESS_TYPES.add("read");
		VALID_ACCESS_TYPES.add("write");
		VALID_ACCESS_TYPES.add("execute");

		authorizer = createAuthorizer();
	}

	public void run() {
		LOG.debug("==> SampleApp.run()");

		do {
			String input = getInput();

			if(input == null) {
				break;
			}

			if(input.trim().isEmpty()) {
				continue;
			}

			String[]    args       = input.split("\\s+");
			String      accessType = getStringArg(args, 0);
			String      fileName   = getStringArg(args, 1);
			String      userName   = getStringArg(args, 2);
			Set<String> userGroups = new HashSet<String>();

			for(int i = 3; i < args.length; i++) {
				userGroups.add(args[i]);
			}

			if(fileName == null || accessType == null || userName == null) {
				LOG.info("Insufficient arguments. Usage: <accessType> <fileName> <userName> [userGroup1 userGroup2 userGroup3 ..]");

				continue;
			}

			if(! VALID_ACCESS_TYPES.contains(accessType)) {
				LOG.info(accessType + ": invalid accessType");

				continue;
			}

			if(authorizer.authorize(fileName, accessType, userName, userGroups)) {
				LOG.info("Authorized!");
			} else {
				LOG.info("Not authorized!");
			}
		} while(true);

		LOG.debug("<== SampleApp.run()");
	}

	private IAuthorizer createAuthorizer() {
		IAuthorizer ret = null;

		String authzClassName = System.getProperty("sampleapp.authorizer");

		if(authzClassName != null) {
			try {
				Class<IAuthorizer> clz = (Class<IAuthorizer>) Class.forName(authzClassName);

				ret = clz.newInstance();
			} catch(Exception excp) {
				LOG.warn("Failed to create authorizer of type '" + authzClassName + "'", excp);
			}
		}

		if(ret == null) {
			LOG.info("Using default authorizer");
			ret = new DefaultAuthorizer();
		}

		ret.init();

		return ret;
	}

	private String getStringArg(String[] args, int index) {
		if(args == null || args.length <= index) {
			return null;
		}

		return args[index];
	}

	private String getInput() {

		try (Scanner inputReader = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
			System.out.print("command> ");
			System.out.flush();
			return inputReader.nextLine();
		} catch(Exception excp) {
			// ignore
		}

		return null;
	}
}
