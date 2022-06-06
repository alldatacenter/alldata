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

package org.apache.ambari.server.controller.internal;

import java.util.HashSet;

import org.apache.ambari.server.controller.spi.PageResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

/**
 * PageResponseImpl tests.
 */
public class PageResponseImplTest {

  @Test
  public void testGetIterable() throws Exception {
    Iterable<Resource> iterable = new HashSet<>();
    Resource prev = new ResourceImpl(Resource.Type.Cluster);
    Resource next = new ResourceImpl(Resource.Type.Cluster);

    PageResponse response = new PageResponseImpl(iterable, 99, prev, next, 0);

    Assert.assertEquals(iterable, response.getIterable());
  }

  @Test
  public void testGetOffset() throws Exception {
    Iterable<Resource> iterable = new HashSet<>();
    Resource prev = new ResourceImpl(Resource.Type.Cluster);
    Resource next = new ResourceImpl(Resource.Type.Cluster);

    PageResponse response = new PageResponseImpl(iterable, 99, prev, next, 0);

    Assert.assertEquals(99, response.getOffset());
  }

  @Test
  public void testGetPreviousResource() throws Exception {
    Iterable<Resource> iterable = new HashSet<>();
    Resource prev = new ResourceImpl(Resource.Type.Cluster);
    Resource next = new ResourceImpl(Resource.Type.Cluster);

    PageResponse response = new PageResponseImpl(iterable, 99, prev, next, 0);

    Assert.assertEquals(prev, response.getPreviousResource());
  }

  @Test
  public void testGetNextResource() throws Exception {
    Iterable<Resource> iterable = new HashSet<>();
    Resource prev = new ResourceImpl(Resource.Type.Cluster);
    Resource next = new ResourceImpl(Resource.Type.Cluster);

    PageResponse response = new PageResponseImpl(iterable, 99, prev, next, 0);

    Assert.assertEquals(next, response.getNextResource());
  }
}
