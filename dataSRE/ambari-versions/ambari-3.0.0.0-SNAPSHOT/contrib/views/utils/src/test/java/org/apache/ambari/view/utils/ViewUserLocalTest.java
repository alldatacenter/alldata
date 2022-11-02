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

package org.apache.ambari.view.utils;

import org.apache.ambari.view.ViewContext;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class ViewUserLocalTest {
  @Test
  public void testDifferentUsers() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getInstanceName()).andReturn("INSTANCE1").anyTimes();
    expect(viewContext.getUsername()).andReturn("luke").anyTimes();

    ViewContext viewContext2 = createNiceMock(ViewContext.class);
    expect(viewContext2.getInstanceName()).andReturn("INSTANCE1").anyTimes();
    expect(viewContext2.getUsername()).andReturn("leia").anyTimes();
    replay(viewContext, viewContext2);

    UserLocal<Object> test = new UserLocal<Object>(Object.class) {
      @Override
      protected synchronized Object initialValue(ViewContext context) {
        return new Object();
      }
    };

    Object obj1 = test.get(viewContext);
    Object obj2 = test.get(viewContext2);
    assertNotSame(obj1, obj2);
  }

  @Test
  public void testDifferentInstances() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getInstanceName()).andReturn("INSTANCE1").anyTimes();
    expect(viewContext.getUsername()).andReturn("luke").anyTimes();

    ViewContext viewContext2 = createNiceMock(ViewContext.class);
    expect(viewContext2.getInstanceName()).andReturn("INSTANCE2").anyTimes();
    expect(viewContext2.getUsername()).andReturn("luke").anyTimes();
    replay(viewContext, viewContext2);

    UserLocal<Object> test = new UserLocal<Object>(Object.class) {
      @Override
      protected synchronized Object initialValue(ViewContext context) {
        return new Object();
      }
    };

    Object obj1 = test.get(viewContext);
    Object obj2 = test.get(viewContext2);
    assertNotSame(obj1, obj2);
  }

  @Test
  public void testSameUsers() throws Exception {
    ViewContext viewContext = createNiceMock(ViewContext.class);
    expect(viewContext.getInstanceName()).andReturn("INSTANCE1").anyTimes();
    expect(viewContext.getUsername()).andReturn("luke").anyTimes();

    ViewContext viewContext2 = createNiceMock(ViewContext.class);
    expect(viewContext2.getInstanceName()).andReturn("INSTANCE1").anyTimes();
    expect(viewContext2.getUsername()).andReturn("luke").anyTimes();
    replay(viewContext, viewContext2);

    UserLocal<Object> test = new UserLocal<Object>(Object.class) {
      @Override
      protected synchronized Object initialValue(ViewContext context) {
        return new Object();
      }
    };

    Object obj1 = test.get(viewContext);
    Object obj2 = test.get(viewContext2);
    assertSame(obj1, obj2);
  }
}