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

package com.bytedance.bitsail.connector.elasticsearch.doc;

import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.elasticsearch.doc.tools.EsKeySelector;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class EsKeySelectorTest {

  @Test
  public void testKeySelector() {
    List<Integer> idFieldIndices = Arrays.asList(0, 1);
    String delimiter = ",";
    EsKeySelector selector = new EsKeySelector(idFieldIndices, delimiter);

    String[] fields = new String[] {"A", "B", "C", "D"};
    Row row = new Row(fields);

    Assert.assertEquals("A,B", selector.getKey(row));
  }
}
