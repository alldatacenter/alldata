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

package org.apache.ranger.tagsync.process;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.Properties;


public class TestTagSynchronizer {

	private static TagSynchronizer tagSynchronizer;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("setUpBeforeClass() called");

		TagSyncConfig config = TagSyncConfig.getInstance();

		TagSyncConfig.dumpConfiguration(config, new BufferedWriter(new OutputStreamWriter(System.out)));
		System.out.println();

		Properties props = config.getProperties();

		tagSynchronizer = new TagSynchronizer(props);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		System.out.println("tearDownAfterClass() called");

	}

	@Test
	public void testTagSynchronizer() {

		System.out.println("testTagSynchronizer() called");

		boolean initDone = tagSynchronizer.initialize();

		System.out.println("TagSynchronizer initialization result=" + initDone);

		if (initDone) {
			tagSynchronizer.shutdown("From testTagSynchronizer: time=up");
		}

		System.out.println("Exiting test");

		assert(initDone);

	}
}
