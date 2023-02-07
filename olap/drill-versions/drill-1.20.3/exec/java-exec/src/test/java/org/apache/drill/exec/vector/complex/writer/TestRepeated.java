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
package org.apache.drill.exec.vector.complex.writer;

import java.io.ByteArrayOutputStream;

import org.apache.drill.common.DrillAutoCloseables;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.fn.JsonWriter;
import org.apache.drill.exec.vector.complex.impl.ComplexWriterImpl;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class TestRepeated extends BaseTest {
  // private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestRepeated.class);

  private static final DrillConfig drillConfig = DrillConfig.create();
  private static BufferAllocator allocator;

  @BeforeClass
  public static void setupAllocator() {
    allocator = RootAllocatorFactory.newRoot(drillConfig);
  }

  @AfterClass
  public static void destroyAllocator() {
    DrillAutoCloseables.closeNoChecked(allocator);
  }
//
//  @Test
//  public void repeatedMap() {
//
//    /**
//     * We're going to try to create an object that looks like:
//     *
//     *  {
//     *    a: [
//     *      {x: 1, y: 2}
//     *      {x: 2, y: 1}
//     *    ]
//     *  }
//     *
//     */
//    MapVector v = new MapVector("", allocator);
//    ComplexWriter writer = new ComplexWriterImpl("col", v);
//
//    MapWriter map = writer.rootAsMap();
//
//    map.start();
//    ListWriter list = map.list("a");
//    MapWriter inner = list.map();
//
//    IntHolder holder = new IntHolder();
//    IntWriter xCol = inner.integer("x");
//    IntWriter yCol = inner.integer("y");
//
//    inner.start();
//
//    holder.value = 1;
//    xCol.write(holder);
//    holder.value = 2;
//    yCol.write(holder);
//
//    inner.end();
//
//    inner.start();
//
//    holder.value = 2;
//    xCol.write(holder);
//    holder.value = 1;
//    yCol.write(holder);
//
//    inner.end();
//
//    IntWriter numCol = map.integer("nums");
//    holder.value = 14;
//    numCol.write(holder);
//
//    map.end();
//
//
//    assertTrue(writer.ok());
//
//  }

  @Test
  public void listOfList() throws Exception {
    /**
     * We're going to try to create an object that looks like:
     *
     *  {
     *    a: [
     *      [1,2,3],
     *      [2,3,4]
     *    ],
     *    nums: 14,
     *    b: [
     *      { c: 1 },
     *      { c: 2 , x: 15}
     *    ]
     *  }
     *
     */

    final MapVector mapVector = new MapVector("", allocator, null);
    final ComplexWriterImpl writer = new ComplexWriterImpl("col", mapVector);
    writer.allocate();

    {
      final MapWriter map = writer.rootAsMap();
      final ListWriter list = map.list("a");
      list.startList();

      final ListWriter innerList = list.list();
      final IntWriter innerInt = innerList.integer();

      innerList.startList();

      final IntHolder holder = new IntHolder();

      holder.value = 1;
      innerInt.write(holder);
      holder.value = 2;
      innerInt.write(holder);
      holder.value = 3;
      innerInt.write(holder);

      innerList.endList();
      innerList.startList();

      holder.value = 4;
      innerInt.write(holder);
      holder.value = 5;
      innerInt.write(holder);

      innerList.endList();
      list.endList();

      final IntWriter numCol = map.integer("nums");
      holder.value = 14;
      numCol.write(holder);

      final MapWriter repeatedMap = map.list("b").map();
      repeatedMap.start();
      holder.value = 1;
      repeatedMap.integer("c").write(holder);
      repeatedMap.end();

      repeatedMap.start();
      holder.value = 2;
      repeatedMap.integer("c").write(holder);
      final BigIntHolder h = new BigIntHolder();
      h.value = 15;
      repeatedMap.bigInt("x").write(h);
      repeatedMap.end();

      map.end();
    }

    {
      writer.setPosition(1);

      final MapWriter map = writer.rootAsMap();
      final ListWriter list = map.list("a");
      list.startList();

      final ListWriter innerList = list.list();
      final IntWriter innerInt = innerList.integer();

      innerList.startList();

      final IntHolder holder = new IntHolder();

      holder.value = -1;
      innerInt.write(holder);
      holder.value = -2;
      innerInt.write(holder);
      holder.value = -3;
      innerInt.write(holder);

      innerList.endList();
      innerList.startList();

      holder.value = -4;
      innerInt.write(holder);
      holder.value = -5;
      innerInt.write(holder);

      innerList.endList();
      list.endList();

      final IntWriter numCol = map.integer("nums");
      holder.value = -28;
      numCol.write(holder);

      final MapWriter repeatedMap = map.list("b").map();
      repeatedMap.start();
      holder.value = -1;
      repeatedMap.integer("c").write(holder);
      repeatedMap.end();

      repeatedMap.start();
      holder.value = -2;
      repeatedMap.integer("c").write(holder);
      final BigIntHolder h = new BigIntHolder();
      h.value = -30;
      repeatedMap.bigInt("x").write(h);
      repeatedMap.end();

      map.end();
    }

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    final JsonWriter jsonWriter = new JsonWriter(stream, true, true);
    final FieldReader reader = mapVector.getChild("col", MapVector.class).getReader();
    reader.setPosition(0);
    jsonWriter.write(reader);
    reader.setPosition(1);
    jsonWriter.write(reader);

    writer.close();
  }
}
