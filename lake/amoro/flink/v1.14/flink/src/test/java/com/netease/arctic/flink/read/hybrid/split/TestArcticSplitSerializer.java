/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netease.arctic.flink.read.hybrid.split;

import com.netease.arctic.flink.read.FlinkSplitPlanner;
import com.netease.arctic.flink.read.hybrid.reader.TestRowDataReaderFunction;
import org.apache.flink.util.FlinkRuntimeException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestArcticSplitSerializer extends TestRowDataReaderFunction {

  @Test
  public void testSerAndDes() {
    List<ArcticSplit> arcticSplits = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger(0));

    ArcticSplitSerializer serializer = new ArcticSplitSerializer();
    List<byte[]> contents = arcticSplits.stream().map(split -> {
      try {
        return serializer.serialize(split);
      } catch (IOException e) {
        e.printStackTrace();
        return new byte[0];
      }
    }).collect(Collectors.toList());

    Assert.assertArrayEquals(arcticSplits.toArray(new ArcticSplit[0]), contents.stream().map(data -> {
      if (data.length == 0) {
        throw new FlinkRuntimeException("failed cause data length is 0.");
      }
      try {
        return serializer.deserialize(1, data);
      } catch (IOException e) {
        throw new FlinkRuntimeException(e);
      }
    }).toArray(ArcticSplit[]::new));
  }

  @Test
  public void testNullableSplit() throws IOException {
    ArcticSplitSerializer serializer = new ArcticSplitSerializer();
    byte[] ser = serializer.serialize(null);

    ArcticSplit actual = serializer.deserialize(1, ser);

    Assert.assertNull(actual);
  }

}