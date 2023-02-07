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

package org.apache.drill.exec.store.xml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestXMLUtils {

  @Test
  public void testRemoveField() {
    String test1 = "field1_field2_field3";
    assertEquals(XMLUtils.removeField(test1, "field3"), "field1_field2");

    // Test with underscores
    String test2 = "field_1_field_2_field_3";
    assertEquals(XMLUtils.removeField(test2, "field_3"), "field_1_field_2");

    // Test with missing field
    String test3 = "field_1_field_2_field_3";
    assertEquals(XMLUtils.removeField(test3, "field_4"), "");

    // Test with empty string
    String test4 = "";
    assertEquals(XMLUtils.removeField(test4, "field_4"), "");
  }
}
