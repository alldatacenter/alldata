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

import org.junit.Assert;
import org.junit.Test;

/**
 * PersistenceConfig tests.
 */
public class PersistenceConfigTest {
  private final static String xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "    <persistence>\n" +
      "      <entity>\n" +
      "        <class>org.apache.ambari.server.view.TestEntity1</class>\n" +
      "        <id-property>id</id-property>\n" +
      "      </entity>\n" +
      "      <entity>\n" +
      "        <class>org.apache.ambari.server.view.TestEntity2</class>\n" +
      "        <id-property>name</id-property>\n" +
      "      </entity>\n" +
      "    </persistence>" +
      "</view>";

  private final static String xml_no_entities = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "    <persistence>\n" +
      "    </persistence>" +
      "</view>";

  private final static String xml_no_persistence = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "    <persistence>\n" +
      "    </persistence>" +
      "</view>";

  @Test
  public void testGetEntities() throws Exception {
    ViewConfig config = ViewConfigTest.getConfig(xml);
    PersistenceConfig persistenceConfig = config.getPersistence();
    Assert.assertEquals(2, persistenceConfig.getEntities().size());

    config = ViewConfigTest.getConfig(xml_no_entities);
    persistenceConfig = config.getPersistence();
    Assert.assertTrue(persistenceConfig.getEntities().isEmpty());

    config = ViewConfigTest.getConfig(xml_no_persistence);
    persistenceConfig = config.getPersistence();
    Assert.assertTrue(persistenceConfig.getEntities().isEmpty());
  }
}
