/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.checks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

public class HardcodedStackVersionPropertiesCheckTest {

  private static final String currentVersion = "2.3.4.0-1234";

  @Test
  public void testGetHardcodeSearchPattern() throws Exception {
    Pattern p = HardcodedStackVersionPropertiesCheck.getHardcodeSearchPattern(currentVersion);
    assertEquals(p.pattern(), "(?<!-Dhdp\\.version=)2\\.3\\.4\\.0-1234");
  }

  @Test
  public void testStringContainsVersionHardcode() throws Exception {
    Pattern pattern = HardcodedStackVersionPropertiesCheck.getHardcodeSearchPattern(currentVersion);

    // Check various cases
    String content = "";
    assertFalse(HardcodedStackVersionPropertiesCheck.stringContainsVersionHardcode(content, pattern));

    content = "2.3.4.0-1234";
    assertTrue(HardcodedStackVersionPropertiesCheck.stringContainsVersionHardcode(content, pattern));

    content = "dfsdfds fdsfds -Dhdp.version=2.3.4.0-1234 sfdfdsfds";
    assertFalse(HardcodedStackVersionPropertiesCheck.stringContainsVersionHardcode(content, pattern));

    content = "dfsdfds fdsfds -Dhdp.version=2.3.4.0-1234 \n sfdfdsfds 2.3.4.0-1234 \n fdsfds";
    assertTrue(HardcodedStackVersionPropertiesCheck.stringContainsVersionHardcode(content, pattern));

    content = "hdp.version=2.3.4.0-1234";
    assertTrue(HardcodedStackVersionPropertiesCheck.stringContainsVersionHardcode(content, pattern));

    content = "kgflkfld fdf\nld;ls;f d hdp.version=2.3.4.0-1234 \n sfdfdsfds \n fdsfds";
    assertTrue(HardcodedStackVersionPropertiesCheck.stringContainsVersionHardcode(content, pattern));
  }
}
