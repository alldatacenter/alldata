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

package org.apache.ambari.server.view.configuration;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;

/**
 * EntityConfig tests.
 */
public class PermissionConfigTest {

  private final static String xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "  <permission>\n" +
      "    <name>RESTRICTED</name>\n" +
      "    <description>Access permission for a restricted view resource.</description>\n" +
      "  </permission>" +
      "</view>";


  @Test
  public void testGetName() throws Exception {
    List<PermissionConfig> permissions = getPremissionConfig();

    Assert.assertEquals(1, permissions.size());

    Assert.assertEquals("RESTRICTED", permissions.get(0).getName());
  }

  @Test
  public void testGetDescription() throws Exception {
    List<PermissionConfig> permissions = getPremissionConfig();

    Assert.assertEquals(1, permissions.size());

    Assert.assertEquals("Access permission for a restricted view resource.", permissions.get(0).getDescription());
  }

  public static List<PermissionConfig> getPremissionConfig() throws JAXBException {
    ViewConfig config = ViewConfigTest.getConfig(xml);

    return config.getPermissions();
  }
}
