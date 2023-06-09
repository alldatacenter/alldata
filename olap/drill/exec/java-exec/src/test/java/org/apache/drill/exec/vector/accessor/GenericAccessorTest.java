/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.vector.accessor;

import org.apache.drill.categories.VectorTest;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.BaseTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(VectorTest.class)
public class GenericAccessorTest extends BaseTest {

  public static final Object NON_NULL_VALUE = "Non-null value";

  private GenericAccessor genericAccessor;
  private ValueVector valueVector;
  private ValueVector.Accessor accessor;
  private UserBitShared.SerializedField metadata;

  @Before
  public void setUp() throws Exception {
    // Set up a mock accessor that has two columns, one non-null and one null
    accessor = mock(ValueVector.Accessor.class);
    when(accessor.getObject(anyInt())).thenAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] args = invocationOnMock.getArguments();
        Integer index = (Integer) args[0];
        if(index == 0) {
          return NON_NULL_VALUE;
        }
        if(index == 1) {
          return null;
        }
        throw new IndexOutOfBoundsException("Index out of bounds");
      }
    });
    when(accessor.isNull(anyInt())).thenAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] args = invocationOnMock.getArguments();
        Integer index = (Integer) args[0];
        if(index == 0) {
          return false;
        }
        return true;
      }
    });

    metadata = UserBitShared.SerializedField.getDefaultInstance();
    valueVector = mock(ValueVector.class);
    when(valueVector.getAccessor()).thenReturn(accessor);
    when(valueVector.getMetadata()).thenReturn(metadata);

    genericAccessor = new GenericAccessor(valueVector);
  }

  @Test
  public void testIsNull() throws Exception {
    assertFalse(genericAccessor.isNull(0));
    assertTrue(genericAccessor.isNull(1));
  }

  @Test
  public void testGetObject() throws Exception {
    assertEquals(NON_NULL_VALUE, genericAccessor.getObject(0));
    assertNull(genericAccessor.getObject(1));
  }

  @Test(expected=IndexOutOfBoundsException.class)
  public void testGetObject_indexOutOfBounds() throws Exception {
    genericAccessor.getObject(2);
  }

  @Test
  public void testGetType() throws Exception {
    assertEquals(UserBitShared.SerializedField.getDefaultInstance().getMajorType(), genericAccessor.getType());
  }
}
