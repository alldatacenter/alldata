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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.FastJsonUtil;

import com.alibaba.fastjson.JSON;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ListColumnTest {

  private static LongColumn lc;
  private static ListColumn<LongColumn> list1;
  private static ListColumn<StringColumn> list2;
  private static ListColumn<LongColumn> list3;
  private static List t;
  private static String str;

  @Before
  public void init() {
    ColumnCast.initColumnCast(BitSailConfiguration.newDefault());
    lc = new LongColumn("99");

    list1 = new ListColumn<LongColumn>(LongColumn.class);
    list1.add(new LongColumn("123"));
    list1.add(new LongColumn("4567"));

    list2 = new ListColumn<StringColumn>(StringColumn.class);
    list2.add(new StringColumn("abc"));
    list2.add(new StringColumn("edf"));
    list2.add(new StringColumn("\"test\":123"));

    t = new ArrayList<LongColumn>();
    t.add(new LongColumn("777"));
    t.add(new LongColumn("888"));
    t.add(new LongColumn("999"));

    list3 = new ListColumn(t, LongColumn.class);
    str = "[{\"byteSize\":3,\"rawData\":777},{\"byteSize\":3,\"rawData\":888},{\"byteSize\":3,\"rawData\":999}]";

  }

  @Test
  public void testListColumn() {
    assertEquals(2, list1.size());
    assertEquals(7, list1.getByteSize());
    assertEquals((Long) 123L, list1.get(0).asLong());

    assertEquals(3, list3.size());
    assertEquals((Long) 777L, list3.get(0).asLong());

  }

  @Test
  public void testListColumnNestString() {
    assertEquals("99", lc.getRawData().toString());
    assertEquals("[123, 4567]", list1.getRawData().toString());
    assertEquals("[777,888,999]", list3.asString());
    assertEquals(str, list3.toString());

    String s = list2.asString();
    assertEquals("[\"abc\",\"edf\",\"\\\"test\\\":123\"]", s);

    List list3 = JSON.parseArray(str);
    Object rawData = FastJsonUtil.parseObject(list3.get(0).toString()).get("rawData").toString();
    assertEquals("777", rawData);
  }

}
