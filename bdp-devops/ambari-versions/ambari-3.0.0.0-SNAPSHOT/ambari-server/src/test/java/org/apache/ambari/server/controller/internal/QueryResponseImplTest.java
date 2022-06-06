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
import java.util.Set;

import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

/**
 * QueryResponseImpl tests
 */
public class QueryResponseImplTest {

  @Test
  public void testGetResources() throws Exception {
    Set<Resource> resources = new HashSet<>();
    resources.add(new ResourceImpl(Resource.Type.Stage));

    QueryResponse queryResponse = new QueryResponseImpl(resources);
    Assert.assertEquals(resources, queryResponse.getResources());
  }

  @Test
  public void testIsSortedResponse() throws Exception {
    Set<Resource> resources = new HashSet<>();
    resources.add(new ResourceImpl(Resource.Type.Stage));

    QueryResponse queryResponse = new QueryResponseImpl(resources);
    Assert.assertFalse(queryResponse.isSortedResponse());

    queryResponse = new QueryResponseImpl(resources, false, true, 0);
    Assert.assertFalse(queryResponse.isSortedResponse());

    queryResponse = new QueryResponseImpl(resources, true, true, 0);
    Assert.assertTrue(queryResponse.isSortedResponse());
  }

  @Test
  public void testIsPagedResponse() throws Exception {
    Set<Resource> resources = new HashSet<>();
    resources.add(new ResourceImpl(Resource.Type.Stage));

    QueryResponse queryResponse = new QueryResponseImpl(resources);
    Assert.assertFalse(queryResponse.isPagedResponse());

    queryResponse = new QueryResponseImpl(resources, true, false, 0);
    Assert.assertFalse(queryResponse.isPagedResponse());

    queryResponse = new QueryResponseImpl(resources, true, true, 0);
    Assert.assertTrue(queryResponse.isPagedResponse());
  }

  @Test
  public void testGetTotalResourceCount() throws Exception {
    Set<Resource> resources = new HashSet<>();
    resources.add(new ResourceImpl(Resource.Type.Stage));

    QueryResponse queryResponse = new QueryResponseImpl(resources);
    Assert.assertEquals(0, queryResponse.getTotalResourceCount());

    queryResponse = new QueryResponseImpl(resources, true, false, 99);
    Assert.assertEquals(99, queryResponse.getTotalResourceCount());
  }
}