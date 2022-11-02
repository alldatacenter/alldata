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
public class EntityConfigTest {

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


  @Test
  public void testGetClassName() throws Exception {
    List<EntityConfig> entities = getEntityConfigs();

    Assert.assertEquals(2, entities.size());

    Assert.assertEquals("org.apache.ambari.server.view.TestEntity1", entities.get(0).getClassName());
    Assert.assertEquals("org.apache.ambari.server.view.TestEntity2", entities.get(1).getClassName());
  }

  @Test
  public void testGetIdProperty() throws Exception {
    List<EntityConfig> entities = getEntityConfigs();

    Assert.assertEquals(2, entities.size());

    Assert.assertEquals("id", entities.get(0).getIdProperty());
    Assert.assertEquals("name", entities.get(1).getIdProperty());
  }

  public static List<EntityConfig> getEntityConfigs() throws JAXBException {
    ViewConfig config = ViewConfigTest.getConfig(xml);

    PersistenceConfig persistenceConfig = config.getPersistence();

    return persistenceConfig.getEntities();
  }
}
