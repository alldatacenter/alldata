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

package com.bytedance.bitsail.flink.core.util;

import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.StringColumn;

import org.apache.flink.types.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RowUtilTest {

  @Test
  public void getRowBytesSizeFromBitSailColumnTest() {
    final Row row = new Row(2);
    row.setField(0, new LongColumn(1));
    row.setField(1, new StringColumn("dance"));
    Assert.assertEquals(13, RowUtil.getRowBytesSize(row));
  }

  @Test
  public void getRowBytesSizeFromFlinkBasicTypeTest() {
    final Row row = new Row(2);
    row.setField(0, 1L);
    row.setField(1, "dance");
    Assert.assertEquals(13, RowUtil.getRowBytesSize(row));
  }

  @Test
  public void getRowBytesSizeFromFlinkListTypeTest() {
    final Row row = new Row(2);
    List<List<String>> listType = new ArrayList<>();
    List<String> listItem = new ArrayList<>();
    listItem.add("byte");
    listItem.add("dance");
    listType.add(listItem);

    row.setField(0, listType);
    row.setField(1, listItem);
    Assert.assertEquals(18, RowUtil.getRowBytesSize(row));
  }

  @Test
  public void getRowBytesSizeFromFlinkMapTypeTest() {
    final Row row = new Row(2);
    Map<String, List<Long>> mapType = new HashMap<>();
    List<Long> listItem = new ArrayList<>();
    listItem.add(0L);
    mapType.put("byte", listItem);

    row.setField(0, mapType);
    row.setField(1, listItem);
    Assert.assertEquals(20, RowUtil.getRowBytesSize(row));
  }
}