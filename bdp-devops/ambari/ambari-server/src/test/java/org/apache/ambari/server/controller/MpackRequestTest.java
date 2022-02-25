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


import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for MpackRequest
 */
public class MpackRequestTest {
  @Test
  public void testBasicGetAndSet() {
    MpackRequest mpackRequest =
            new MpackRequest(1L);
    Assert.assertEquals((Long)1L, mpackRequest.getId());
    mpackRequest.setMpackUri("abc.tar.gz");
    mpackRequest.setRegistryId(new Long(1));
    mpackRequest.setMpackVersion("3.0");
    mpackRequest.setMpackName("testmpack");

    Assert.assertEquals("abc.tar.gz", mpackRequest.getMpackUri());
    Assert.assertEquals(new Long("1"), mpackRequest.getRegistryId());
    Assert.assertEquals("3.0", mpackRequest.getMpackVersion());
    Assert.assertEquals("testmpack", mpackRequest.getMpackName());

  }
}
