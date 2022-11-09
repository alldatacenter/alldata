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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MapColumnTest {

  @Test
  public void testListColumn() {
    StringColumn key1 = new StringColumn("key1");
    LongColumn value1 = new LongColumn("111");

    StringColumn key2 = new StringColumn("key2");
    LongColumn value2 = new LongColumn("222");

    StringColumn key3 = new StringColumn("key3");
    LongColumn value3 = new LongColumn("333");

    MapColumn<StringColumn, LongColumn> map1 = new MapColumn<>(StringColumn.class, LongColumn.class);
    map1.put(key1, value1);
    map1.put(key2, value2);
    map1.put(key3, value3);

    assertEquals(21, map1.getByteSize());
    assertEquals("{key1=111, key2=222, key3=333}", map1.getRawData().toString());
    assertEquals("{\"key1\":111,\"key2\":222,\"key3\":333}", map1.asString());
    assertEquals(3, map1.size());
    assertEquals("{\"key1\":111,\"key2\":222,\"key3\":333}", map1.asString());

    Map hm = new HashMap();
    hm.put(new StringColumn("key6"), new LongColumn("666"));
    hm.put(new StringColumn("key7"), new LongColumn("777"));
    hm.put(new StringColumn("key8"), new LongColumn("888"));
    hm.put(new StringColumn("key9"), new LongColumn("999"));

    MapColumn<StringColumn, LongColumn> map2 = new MapColumn<>(hm, StringColumn.class, LongColumn.class);
    assertEquals(4, map2.size());
    assertEquals(28, map2.getByteSize());
  }
}
