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
package org.apache.drill.metastore.rdbms.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.drill.metastore.rdbms.RdbmsBaseTest;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestConverterUtil extends RdbmsBaseTest {

  @Test
  public void testConvertToString() {
    assertEquals("null", ConverterUtil.convertToString(null));
    assertEquals("\"\"", ConverterUtil.convertToString(""));
    assertEquals("\"abc\"", ConverterUtil.convertToString("abc"));
    assertEquals("123", ConverterUtil.convertToString(123L));
    assertEquals("true", ConverterUtil.convertToString(true));
  }

  @Test
  public void testConvertToType() {
    assertNull(ConverterUtil.convertTo(null, new TypeReference<String>() {
    }));
    assertNull(ConverterUtil.convertTo("null", new TypeReference<String>() {
    }));
    assertNull(ConverterUtil.convertTo("null", new TypeReference<Long>() {
    }));
    assertEquals("", ConverterUtil.convertTo("\"\"", new TypeReference<String>() {
    }));
    assertEquals("abc", ConverterUtil.convertTo("\"abc\"", new TypeReference<String>() {
    }));
    assertEquals(Long.valueOf(123), ConverterUtil.convertTo("123", new TypeReference<Long>() {
    }));
    assertTrue(ConverterUtil.convertTo("true", new TypeReference<Boolean>() {
    }));
    try {
      ConverterUtil.convertTo("abc", new TypeReference<Long>() {
      });
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Unable to convert"));
    }
  }

  @Test
  public void testConvertToListString() {
    assertNull(ConverterUtil.convertToListString("null"));
    assertEquals(Collections.<String>emptyList(), ConverterUtil.convertToListString("[]"));
    assertEquals(Arrays.asList("a", "b", "c"), ConverterUtil.convertToListString("[\"a\",\"b\",\"c\"]"));
    try {
      ConverterUtil.convertToListString("{}");
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Unable to convert"));
    }
  }

  @Test
  public void testConvertToListMapStringString() {
    assertNull(ConverterUtil.convertToMapStringString("null"));
    assertEquals(Collections.<String, String>emptyMap(), ConverterUtil.convertToMapStringString("{}"));
    assertEquals(ImmutableMap.of("a", "b", "c", "d"),
      ConverterUtil.convertToMapStringString("{\"a\":\"b\",\"c\":\"d\"}"));
    try {
      ConverterUtil.convertToMapStringString("[]");
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Unable to convert"));
    }
  }

  @Test
  public void testConvertToListMapStringFloat() {
    assertNull(ConverterUtil.convertToMapStringFloat("null"));
    assertEquals(Collections.<String, Float>emptyMap(), ConverterUtil.convertToMapStringFloat("{}"));
    assertEquals(ImmutableMap.of("a", 1.2F, "c", 3.4F),
      ConverterUtil.convertToMapStringFloat("{\"a\":1.2,\"c\":3.4}"));
    try {
      ConverterUtil.convertToMapStringFloat("{\"a\":\"b\",\"c\":\"d\"}");
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Unable to convert"));
    }
  }
}
