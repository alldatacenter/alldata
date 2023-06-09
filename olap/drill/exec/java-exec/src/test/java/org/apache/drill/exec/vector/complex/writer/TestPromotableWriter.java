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

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.store.TestOutputMutator;
import org.apache.drill.exec.util.BatchPrinter;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

public class TestPromotableWriter extends BaseTest {

  @Test
  public void list() throws Exception {
    BufferAllocator allocator = RootAllocatorFactory.newRoot(DrillConfig.create());
    TestOutputMutator output = new TestOutputMutator(allocator);
    ComplexWriter rootWriter = new VectorContainerWriter(output, true);
    MapWriter writer = rootWriter.rootAsMap();


    rootWriter.setPosition(0);
    {
      writer.map("map").bigInt("a").writeBigInt(1);
    }
    rootWriter.setPosition(1);
    {
      writer.map("map").float4("a").writeFloat4(2.0f);
    }
    rootWriter.setPosition(2);
    {
      writer.map("map").list("a").startList();
      writer.map("map").list("a").endList();
    }
    rootWriter.setPosition(3);
    {
      writer.map("map").list("a").startList();
      writer.map("map").list("a").bigInt().writeBigInt(3);
      writer.map("map").list("a").float4().writeFloat4(4);
      writer.map("map").list("a").endList();
    }
    rootWriter.setValueCount(4);
    BatchPrinter.printBatch(output.getContainer());
  }

}
