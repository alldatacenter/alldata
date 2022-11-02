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

import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * PageRequestImpl tests.
 */
public class PageRequestImplTest {
  @Test
  public void testGetStartingPoint() throws Exception {
    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.Beginning, 20, 99, null, null);
    Assert.assertEquals(PageRequest.StartingPoint.Beginning, pageRequest.getStartingPoint());
  }

  @Test
  public void testGetPageSize() throws Exception {
    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.Beginning, 20, 99, null, null);
    Assert.assertEquals(20, pageRequest.getPageSize());
  }

  @Test
  public void testGetOffset() throws Exception {
    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.Beginning, 20, 99, null, null);
    Assert.assertEquals(99, pageRequest.getOffset());
  }

  @Test
  public void testGetPredicate() throws Exception {
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("id").equals(20).toPredicate();

    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.Beginning, 20, 99, predicate, null);
    Assert.assertEquals(predicate, pageRequest.getPredicate());
  }
}
