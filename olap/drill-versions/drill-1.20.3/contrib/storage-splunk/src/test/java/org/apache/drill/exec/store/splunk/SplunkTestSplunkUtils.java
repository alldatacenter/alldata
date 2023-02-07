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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.drill.exec.store.splunk;

import org.apache.drill.categories.SlowTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({SlowTest.class})
public class SplunkTestSplunkUtils {

  @Test
  public void testIsSpecialField() {
    assertTrue(SplunkUtils.SPECIAL_FIELDS.includes("sourcetype"));
    assertTrue(SplunkUtils.SPECIAL_FIELDS.includes("earliestTime"));
    assertTrue(SplunkUtils.SPECIAL_FIELDS.includes("latestTime"));
    assertTrue(SplunkUtils.SPECIAL_FIELDS.includes("spl"));
  }

  @Test
  public void testIsNotSpecialField() {
    assertFalse(SplunkUtils.SPECIAL_FIELDS.includes("bob"));
    assertFalse(SplunkUtils.SPECIAL_FIELDS.includes("ip_address"));
    assertFalse(SplunkUtils.SPECIAL_FIELDS.includes("mac_address"));
    assertFalse(SplunkUtils.SPECIAL_FIELDS.includes("latest_Time"));
  }
}
