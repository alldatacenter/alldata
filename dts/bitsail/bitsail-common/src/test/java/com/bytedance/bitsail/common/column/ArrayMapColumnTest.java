/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.column;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ArrayMapColumnTest {
  @Test
  public void testArrayMapColumn() {
    StringColumn key1 = new StringColumn("key1");
    LongColumn value1 = new LongColumn("111");

    StringColumn key2 = new StringColumn("key2");
    LongColumn value2 = new LongColumn("222");

    StringColumn key3 = new StringColumn("key3");
    LongColumn value3 = new LongColumn("333");

    List<StringColumn> keys = new ArrayList<>();
    keys.add(key1);
    keys.add(key2);
    keys.add(key3);
    List<LongColumn> values = new ArrayList<>();
    values.add(value1);
    values.add(value2);
    values.add(value3);
    ArrayMapColumn<StringColumn, LongColumn> map1 = new ArrayMapColumn<>(keys, values, StringColumn.class, LongColumn.class);

    assertEquals(21, map1.getByteSize());
    assertEquals("{key1=111, key2=222, key3=333}", map1.getRawData().toString());
    assertEquals("{\"key1\":111,\"key2\":222,\"key3\":333}", map1.asString());
    assertEquals(3, map1.size());
    assertEquals("{\"key1\":111,\"key2\":222,\"key3\":333}", map1.asString());

    keys = new ArrayList<>();
    keys.add(new StringColumn("key6"));
    keys.add(new StringColumn("key7"));
    keys.add(new StringColumn("key8"));
    keys.add(new StringColumn("key9"));
    values = new ArrayList<>();
    values.add(new LongColumn("666"));
    values.add(new LongColumn("777"));
    values.add(new LongColumn("888"));
    values.add(new LongColumn("999"));

    ArrayMapColumn<StringColumn, LongColumn> map2 = new ArrayMapColumn<>(keys, values, StringColumn.class, LongColumn.class);
    assertEquals(4, map2.size());
    assertEquals(28, map2.getByteSize());
  }
}
