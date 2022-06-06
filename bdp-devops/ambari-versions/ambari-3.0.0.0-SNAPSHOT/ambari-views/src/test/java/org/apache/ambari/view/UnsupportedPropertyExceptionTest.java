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

package org.apache.ambari.view;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * UnsupportedPropertyException tests.
 */
public class UnsupportedPropertyExceptionTest {
  @Test
  public void testGetType() throws Exception {
    UnsupportedPropertyException exception = new UnsupportedPropertyException("type", Collections.singleton("p1"));
    Assert.assertEquals("type", exception.getType());
  }

  @Test
  public void testGetPropertyIds() throws Exception {
    Set<String> ids = new HashSet<String>();
    ids.add("p1");
    ids.add("p2");

    UnsupportedPropertyException exception = new UnsupportedPropertyException("type", ids);
    Assert.assertEquals(ids, exception.getPropertyIds());
  }
}
