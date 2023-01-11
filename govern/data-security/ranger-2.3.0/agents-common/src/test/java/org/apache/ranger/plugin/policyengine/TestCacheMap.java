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

package org.apache.ranger.plugin.policyengine;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class TestCacheMap {
	private static final Logger LOG = LoggerFactory.getLogger(TestCacheMap.class);
	private static CacheMap<String, String> testCacheMap;
	private static int initialCapacity = 16;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> TestCacheMap.setUpBeforeClass(), initialCapacity:" + initialCapacity);
		}

		testCacheMap = new CacheMap<String, String>(initialCapacity);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== TestCacheMap.setUpBeforeClass(), initialCapacity:" + initialCapacity);
		}
	}
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void runTests() {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> TestCacheMap.runTests(), First batch of " + initialCapacity + " inserts starting from 0");
		}
		for (int i = 0; i < initialCapacity; i++) {
			String key = String.valueOf(i);
			String value = key;

			if (LOG.isDebugEnabled()) {
				LOG.debug("TestCacheMap.runTests(), Inserting into Cache, key:" + key + ", value:" + value);
			}
			testCacheMap.put(key, value);
			if (LOG.isDebugEnabled()) {
				LOG.debug("TestCacheMap.runTests(), Cache Size after insert(): " + testCacheMap.size());
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("TestCacheMap.runTests(), First batch of " + initialCapacity/2 + " retrieves counting down from " + (initialCapacity/2-1));
		}

		for (int i = initialCapacity/2 - 1; i >= 0; i--) {
			String key = String.valueOf(i);
			if (LOG.isDebugEnabled()) {
				LOG.debug("TestCacheMap.runTests(), Searching Cache, key:" + key);
			}
			String value = testCacheMap.get(key);
			if (value == null || !value.equals(key)) {
				LOG.error("TestCacheMap.runTests(), Did not get correct value for key, key:" + key + ", value:" + value);
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("TestCacheMap.runTests(), Second batch of " + initialCapacity/2 + " inserts starting from " + initialCapacity);
		}
		for (int i = initialCapacity; i < initialCapacity+initialCapacity/2; i++) {
			String key = String.valueOf(i);
			String value = key;

			if (LOG.isDebugEnabled()) {
				LOG.debug("TestCacheMap.runTests(), Inserting into Cache, key:" + key + ", value:" + value);
			}
			testCacheMap.put(key, value);
			if (LOG.isDebugEnabled()) {
				LOG.debug("TestCacheMap.runTests(), Cache Size after insert(): " + testCacheMap.size());
			}
		}

		Set<String> keySet = testCacheMap.keySet();

		if (LOG.isDebugEnabled()) {
			LOG.debug("TestCacheMap.runTests(), KeySet Size:" + keySet.size());
			LOG.debug("TestCacheMap.runTests(), printing keys..");

		    int i = 0;

		    for (String key : keySet) {
				LOG.debug("TestCacheMap.runTests(), index:" + i++ + ", key:" + key);
			}

			LOG.debug("<== TestCacheMap.runTests()");
		}

	}
}
