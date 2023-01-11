/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.plugin.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MacroProcessorTest {

	@Test
	public void testMacros() {
		Map<String, String> macros = new HashMap<>();

		macros.put("USER", "getUser()");
		macros.put("USER_NAME", "getUserName()");
		macros.put("GROUPS", "getGroups()");
		macros.put("GET_GROUP_ATTR", "getGroupAttr");

		MacroProcessor processor = new MacroProcessor(macros);

		Map<String, String> testInputOutput = new HashMap<>();

		testInputOutput.putAll(macros);
		testInputOutput.put("USER != null", "getUser() != null");
		testInputOutput.put("USER_NAME=='testUser'", "getUserName()=='testUser'");
		testInputOutput.put("USER != null && USER_NAME=='testUser'", "getUser() != null && getUserName()=='testUser'");
		testInputOutput.put("GROUPS.length() > 1", "getGroups().length() > 1");
		testInputOutput.put("GET_GROUP_ATTR('group1', 'attr1')", "getGroupAttr('group1', 'attr1')");

		for (Map.Entry<String, String> inputOutput : testInputOutput.entrySet()) {
			String input  = inputOutput.getKey();
			String output = inputOutput.getValue();

			assertEquals(output, processor.expandMacros(input));
		}
	}
}
