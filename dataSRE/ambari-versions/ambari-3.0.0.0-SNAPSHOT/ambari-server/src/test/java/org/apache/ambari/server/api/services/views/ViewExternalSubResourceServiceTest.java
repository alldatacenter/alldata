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
package org.apache.ambari.server.api.services.views;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * ViewExternalSubResourceService tests.
 */
public class ViewExternalSubResourceServiceTest {
  @Test
  public void testAddResourceService() throws Exception {
    Resource.Type type = new Resource.Type("resource");

    ViewInstanceEntity definition = ViewInstanceEntityTest.getViewInstanceEntity();
    ViewExternalSubResourceService service = new ViewExternalSubResourceService(type, definition);

    Object fooService = new Object();

    service.addResourceService("foo", fooService);

    Assert.assertEquals(fooService, service.getResource("foo"));

    try {
      service.getResource("bar");
      Assert.fail("Expected IllegalArgumentException for unknown service name.");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
