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
package org.apache.drill.exec.record.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestMetadataProperties extends BaseTest {

  @Test
  public void testBasics() {

    AbstractPropertied props = new AbstractPropertied();
    assertFalse(props.hasProperties());

    // Copy constructor works

    AbstractPropertied second = new AbstractPropertied(props);
    assertFalse(second.hasProperties());

    // Getting a property does not change state

    assertNull(props.property("foo"));
    assertFalse(props.hasProperties());

    // Clearing a property does not change state

    props.setProperty("foo", null);
    assertNull(props.property("foo"));
    assertFalse(props.hasProperties());

    // Getting all properties does not change state (though it
    // does materialize properties)

    assertNotNull(props.properties());
    assertTrue(props.properties().isEmpty());
    assertFalse(props.hasProperties());

    // Setting a property works as expected

    props.setProperty("foo", "bar");
    assertEquals("bar", props.property("foo"));
    assertTrue(props.hasProperties());
    assertEquals("bar", props.properties().get("foo"));

    // As does clearing a property

    props.setProperty("foo", null);
    assertNull(props.property("foo"));
    assertFalse(props.hasProperties());

    // Setting multiple properties overwrites duplicates,
    // leaves others.

    props.setProperty("foo", "bar");
    props.setProperty("fred", "wilma");

    Map<String, String> other = new HashMap<>();
    other.put("fred", "pebbles");
    other.put("barney", "bambam");

    props.setProperties(other);
    assertTrue(props.hasProperties());
    assertEquals(3, props.properties().size());
    assertEquals("bar", props.property("foo"));
    assertEquals("pebbles", props.property("fred"));
    assertEquals("bambam", props.property("barney"));

    // Copy constructor works

    second = new AbstractPropertied(props);
    assertTrue(second.hasProperties());
    assertEquals(3, second.properties().size());
  }

  @Test
  public void testAccessor() {
    AbstractPropertied props = new AbstractPropertied();

    // Accessors with default

    assertEquals("bar", PropertyAccessor.getString(props, "foo", "bar"));
    assertEquals(10, PropertyAccessor.getInt(props, "foo", 10));
    assertEquals(true, PropertyAccessor.getBoolean(props, "foo", true));

    // Accessors without default

    assertNull(props.property("foo"));
    assertEquals(0, PropertyAccessor.getInt(props, "foo"));
    assertEquals(false, PropertyAccessor.getBoolean(props, "foo"));

    // Set, then get, property

    props.setProperty("str", "value");
    assertEquals("value", props.property("str"));

    PropertyAccessor.set(props, "int", 20);
    assertEquals("20", props.property("int"));
    assertEquals(20, PropertyAccessor.getInt(props, "int"));

    PropertyAccessor.set(props, "bool", true);
    assertEquals("true", props.property("bool"));
    assertEquals(true, PropertyAccessor.getBoolean(props, "bool"));
  }

  @Test
  public void testWidth() {
    PrimitiveColumnMetadata col = new PrimitiveColumnMetadata("c", MinorType.VARCHAR, DataMode.OPTIONAL);
    AbstractPropertied props = col;

    // Width is not set by default
    assertFalse(props.hasProperties());

    // But is estimated on demand

    assertEquals(BasicTypeHelper.WIDTH_ESTIMATE, col.expectedWidth());

    // Set an explicit width

    col.setExpectedWidth(20);
    assertTrue(props.hasProperties());
    assertEquals(20, col.expectedWidth());
    assertEquals("20", col.property(ColumnMetadata.EXPECTED_WIDTH_PROP));

    // Clear the width

    col.setProperty(ColumnMetadata.EXPECTED_WIDTH_PROP, null);
    assertFalse(props.hasProperties());
    assertEquals(BasicTypeHelper.WIDTH_ESTIMATE, col.expectedWidth());
  }

  @Test
  public void testFormat() {
    PrimitiveColumnMetadata col = new PrimitiveColumnMetadata("c", MinorType.INT, DataMode.OPTIONAL);
    AbstractPropertied props = col;
    assertNull(col.format());
    col.setFormat("###");
    assertEquals("###", col.format());
    assertTrue(props.hasProperties());
    assertEquals("###", col.property(ColumnMetadata.FORMAT_PROP));
    col.setFormat(null);
    assertFalse(props.hasProperties());
    assertNull(col.format());
  }

  @Test
  public void testDefault() {
    PrimitiveColumnMetadata col = new PrimitiveColumnMetadata("c", MinorType.VARCHAR, DataMode.OPTIONAL);
    AbstractPropertied props = col;
    assertNull(col.defaultValue());
    col.setDefaultValue("empty");
    assertEquals("empty", col.defaultValue());
    assertTrue(props.hasProperties());
    assertEquals("empty", col.property(ColumnMetadata.DEFAULT_VALUE_PROP));
    col.setDefaultValue(null);
    assertFalse(props.hasProperties());
    assertNull(col.defaultValue());
  }

  @Test
  public void testStringEncode() {
    PrimitiveColumnMetadata col = new PrimitiveColumnMetadata("c", MinorType.VARCHAR, DataMode.OPTIONAL);
    String encoded = col.valueToString("foo");
    assertEquals("foo", encoded);
    col.setDefaultValue(encoded);
    assertEquals("foo", col.decodeDefaultValue());
  }

  @Test
  public void testIntEncode() {
    PrimitiveColumnMetadata col = new PrimitiveColumnMetadata("c", MinorType.INT, DataMode.OPTIONAL);
    String encoded = col.valueToString(20);
    assertEquals("20", encoded);
    assertEquals(20, col.valueFromString(encoded));
    col.setDefaultValue(encoded);
    assertEquals(20, col.decodeDefaultValue());
  }

  // TODO: Test encode/decode for other types
}
