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
package org.apache.ambari.server.view.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * EventImpl tests.
 */
public class EventImplTest {

  private static String view_xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "</view>";

  @Test
  public void testGetId() throws Exception {
    EventImpl event = getEvent("MyEvent", Collections.emptyMap(), view_xml);
    Assert.assertEquals("MyEvent", event.getId());
  }

  @Test
  public void testGetProperties() throws Exception {
    Map<String, String> properties = new HashMap<>();
    properties.put("p1", "v1");
    properties.put("p2", "v2");

    EventImpl event = getEvent("MyEvent", properties, view_xml);
    Assert.assertEquals(properties, event.getProperties());
  }

  @Test
  public void testGetViewSubject() throws Exception {
    EventImpl event = getEvent("MyEvent", Collections.emptyMap(), view_xml);

    Assert.assertEquals("MY_VIEW", event.getViewSubject().getViewName());
    Assert.assertEquals("My View!", event.getViewSubject().getLabel());
    Assert.assertEquals("1.0.0", event.getViewSubject().getVersion());
  }

  @Test
  public void testGetViewInstanceSubject() throws Exception {
    EventImpl event = getEvent("MyEvent", Collections.emptyMap(), view_xml);
    Assert.assertNull(event.getViewInstanceSubject());

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();
    event = getEvent("MyEvent", Collections.emptyMap(), viewInstanceEntity);
    Assert.assertEquals(viewInstanceEntity, event.getViewInstanceSubject());
  }

  public static EventImpl getEvent(String id, Map<String, String> properties, String xml) throws Exception{
    ViewConfig viewConfig = ViewConfigTest.getConfig(xml);
    ViewEntity viewEntity = ViewEntityTest.getViewEntity(viewConfig);
    return new EventImpl(id, properties, viewEntity);
  }


  public static EventImpl getEvent(String id, Map<String, String> properties, ViewInstanceEntity instanceEntity) throws Exception{
    return new EventImpl(id, properties, instanceEntity);
  }
}
