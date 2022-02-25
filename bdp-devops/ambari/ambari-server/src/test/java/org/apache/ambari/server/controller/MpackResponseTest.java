/**
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
package org.apache.ambari.server.controller;


import java.util.ArrayList;
import java.util.HashMap;

import org.apache.ambari.server.state.Module;
import org.apache.ambari.server.state.Mpack;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for MpackResponse
 */
public class MpackResponseTest {
  @Test
  public void testBasicGetAndSet() {
    MpackResponse mpackResponse = new MpackResponse(setupMpack());
    Assert.assertEquals((Long)100L, mpackResponse.getId());
    Assert.assertEquals((Long)100L, mpackResponse.getRegistryId());
    Assert.assertEquals("3.0",mpackResponse.getMpackVersion());
    Assert.assertEquals("abc.tar.gz",mpackResponse.getMpackUri());
    Assert.assertEquals("testMpack", mpackResponse.getMpackName());

  }

  public Mpack setupMpack() {
    Mpack mpack = new Mpack();
    mpack.setResourceId(100L);
    mpack.setModules(new ArrayList<Module>());
    mpack.setPrerequisites(new HashMap<String, String>());
    mpack.setRegistryId(100L);
    mpack.setVersion("3.0");
    mpack.setMpackUri("abc.tar.gz");
    mpack.setDescription("Test mpack");
    mpack.setName("testMpack");

    return mpack;
  }
}
