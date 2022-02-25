/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests {@link org.apache.ambari.server.utils.SetUtils}
 */
public class SetUtilsTest {

  @Test
  public void testSplit() {

    try {
      SetUtils.split(null, 0);
      Assert.fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // expected
    }

    int size = 10;
    List<Set<Integer>> subsets = SetUtils.split(null, size);
    Assert.assertEquals(0, subsets.size());

    subsets = SetUtils.split(Collections.<Integer>emptySet(), size);
    Assert.assertEquals(0, subsets.size());

    subsets = SetUtils.split(Collections.singleton(0), size);
    Assert.assertEquals(1, subsets.size());
    Assert.assertEquals(1, subsets.get(0).size());

    Set<Integer> set = new LinkedHashSet<>(5);
    for(int i = 0; i < 5; i++) {
      set.add(i);
    }
    subsets = SetUtils.split(set, size);
    Assert.assertEquals(1, subsets.size());
    Assert.assertEquals(5, subsets.get(0).size());


    set = new LinkedHashSet<>(10);
    for(int i = 0; i < 10; i++) {
      set.add(i);
    }
    subsets = SetUtils.split(set, size);
    Assert.assertEquals(1, subsets.size());
    Assert.assertEquals(10, subsets.get(0).size());

    set = new LinkedHashSet<>(11);
    for(int i = 0; i < 11; i++) {
      set.add(i);
    }
    subsets = SetUtils.split(set, size);
    Assert.assertEquals(2, subsets.size());
    Assert.assertEquals(10, subsets.get(0).size());
    Assert.assertEquals(1, subsets.get(1).size());

    set = new LinkedHashSet<>(20);
    for(int i = 0; i < 20; i++) {
      set.add(i);
    }
    subsets = SetUtils.split(set, size);
    Assert.assertEquals(2, subsets.size());
    Assert.assertEquals(10, subsets.get(0).size());
    Assert.assertEquals(10, subsets.get(1).size());

    set = new LinkedHashSet<>(27);
    for(int i = 0; i < 27; i++) {
      set.add(i);
    }
    subsets = SetUtils.split(set, size);
    Assert.assertEquals(3, subsets.size());
    Assert.assertEquals(10, subsets.get(0).size());
    Assert.assertEquals(10, subsets.get(1).size());
    Assert.assertEquals(7, subsets.get(2).size());
  }
}
