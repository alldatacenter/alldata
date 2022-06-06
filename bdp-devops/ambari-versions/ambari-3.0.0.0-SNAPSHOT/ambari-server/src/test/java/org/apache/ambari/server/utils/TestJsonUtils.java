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
package org.apache.ambari.server.utils;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Test JsonUtils
 */
public class TestJsonUtils {
  /**
   * Tests {@link org.apache.ambari.server.utils.JsonUtils}
   * @throws Exception
   */
  @Test
  public void testIsValidJson() throws Exception {
    Assert.assertFalse(JsonUtils.isValidJson(null));
    Assert.assertFalse(JsonUtils.isValidJson(""));
    Assert.assertFalse(JsonUtils.isValidJson("{"));
    Assert.assertFalse(JsonUtils.isValidJson("}"));
    Assert.assertTrue(JsonUtils.isValidJson("{}"));
    Assert.assertTrue(JsonUtils.isValidJson("{ \"stack\" : \"HDP\" }"));
    Assert.assertTrue(JsonUtils.isValidJson("{\n" +
        "  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n" +
        "  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n" +
        "}"));
    Assert.assertFalse(JsonUtils.isValidJson("{\n" +
        "  \"stack_selector\": [\"hdp-select\", \"/usr/bin/hdp-select\", \"hdp-select\"],\n" +
        "  \"conf_selector\": [\"conf-select\", \"/usr/bin/conf-select\", \"conf-select\"]\n" +
        ""));
  }
}
