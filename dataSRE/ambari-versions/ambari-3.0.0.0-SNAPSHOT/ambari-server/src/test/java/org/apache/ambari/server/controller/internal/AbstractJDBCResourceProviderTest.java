/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.junit.Assert;
import org.junit.Test;

/**
 * AbstractJDBCResourceProvider tests.
 */
public class AbstractJDBCResourceProviderTest {
  private static final String property1 = "property1";
  private static final String property2 = "property2";

  @Test
  public void test() throws SQLException {
    Set<String> requestedIds = new TreeSet<>();
    requestedIds.add(property1);
    requestedIds.add("none1");
    requestedIds.add(property2);

    AbstractJDBCResourceProvider<TestFields> provider = new TestAbstractJDBCResourceProviderImpl(
        requestedIds, null);
    Assert.assertEquals(
        TestFields.field1 + "," + TestFields.field2,
        provider.getDBFieldString(requestedIds));
    Assert.assertEquals(TestFields.field1.toString(),
        provider.getDBFieldString(Collections.singleton(property1)));
    Assert.assertEquals("",
        provider.getDBFieldString(Collections.singleton("none1")));
    Assert.assertEquals(TestFields.field1, provider.getDBField(property1));
    Assert.assertEquals(TestFields.field2, provider.getDBField(property2));

    ResultSet rs = createMock(ResultSet.class);
    expect(rs.getString(TestFields.field1.toString())).andReturn("1").once();
    expect(rs.getLong(TestFields.field2.toString())).andReturn(2l).once();
    expect(rs.getInt(TestFields.field1.toString())).andReturn(3).once();
    replay(rs);
    Resource r = new ResourceImpl((Resource.Type) null);
    provider.setString(r, property1, rs, requestedIds);
    provider.setString(r, "none2", rs, requestedIds);
    Assert.assertEquals("1", r.getPropertyValue(property1));
    r = new ResourceImpl((Resource.Type) null);
    provider.setLong(r, property2, rs, requestedIds);
    provider.setLong(r, "none2", rs, requestedIds);
    Assert.assertEquals(2l, r.getPropertyValue(property2));
    r = new ResourceImpl((Resource.Type) null);
    provider.setInt(r, property1, rs, requestedIds);
    provider.setInt(r, "none2", rs, requestedIds);
    Assert.assertEquals(3, r.getPropertyValue(property1));
    verify(rs);
  }

  private enum TestFields {
    field1, field2
  }

  private static class TestAbstractJDBCResourceProviderImpl extends
      AbstractJDBCResourceProvider<TestFields> {
    protected TestAbstractJDBCResourceProviderImpl(Set<String> propertyIds,
        Map<Type,String> keyPropertyIds) {
      super(propertyIds, keyPropertyIds);
    }

    @Override
    public RequestStatus createResources(Request request)
        throws SystemException, UnsupportedPropertyException,
        ResourceAlreadyExistsException, NoSuchParentResourceException {
      return null;
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException,
        NoSuchResourceException, NoSuchParentResourceException {
      return null;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException,
        NoSuchResourceException, NoSuchParentResourceException {
      return null;
    }

    @Override
    public RequestStatus deleteResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException,
        NoSuchResourceException, NoSuchParentResourceException {
      return null;
    }

    @Override
    protected Map<String,TestFields> getDBFieldMap() {
      Map<String,TestFields> fields = new HashMap<>();
      fields.put(property1, TestFields.field1);
      fields.put(property2, TestFields.field2);
      return fields;
    }

    @Override
    protected Set<String> getPKPropertyIds() {
      return null;
    }
  }
}
