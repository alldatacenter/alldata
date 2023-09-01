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

package org.apache.hadoop.mapreduce.task.reduce;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.Decompressor;

import org.apache.uniffle.common.exception.RssException;

// In MR shuffle, MapOutput encapsulates the logic to fetch map task's output data via http.
// So, in RSS, we should bypass this logic, and directly write data to MapOutput.
public class RssBypassWriter {
  private static final Log LOG = LogFactory.getLog(RssBypassWriter.class);

  public static void write(MapOutput mapOutput, byte[] buffer) {
    // Write and commit uncompressed data to MapOutput.
    // In the majority of cases, merger allocates memory to accept data,
    // but when data size exceeds the threshold, merger can also allocate disk.
    // So, we should consider the two situations, respectively.
    if (mapOutput instanceof InMemoryMapOutput) {
      InMemoryMapOutput inMemoryMapOutput = (InMemoryMapOutput) mapOutput;
      // In InMemoryMapOutput constructor method, we create a decompressor or borrow a decompressor from
      // pool. Now we need to put it back, otherwise we will create a decompressor for every InMemoryMapOutput
      // object, they will cause `out of direct memory` problems.
      CodecPool.returnDecompressor(getDecompressor(inMemoryMapOutput));
      write(inMemoryMapOutput, buffer);
    } else if (mapOutput instanceof OnDiskMapOutput) {
      // RSS leverages its own compression, it is incompatible with hadoop's disk file compression.
      // So we should disable this situation.
      throw new IllegalStateException("RSS does not support OnDiskMapOutput as shuffle ouput,"
        + " try to reduce mapreduce.reduce.shuffle.memory.limit.percent");
    } else {
      throw new IllegalStateException("Merger reserve unknown type of MapOutput: "
        + mapOutput.getClass().getCanonicalName());
    }
  }

  private static void write(InMemoryMapOutput inMemoryMapOutput, byte[] buffer) {
    byte[] memory = inMemoryMapOutput.getMemory();
    System.arraycopy(buffer, 0, memory, 0, buffer.length);
  }

  static Decompressor getDecompressor(InMemoryMapOutput inMemoryMapOutput) {
    try {
      Class clazz = Class.forName(InMemoryMapOutput.class.getName());
      Field deCompressorField = clazz.getDeclaredField("decompressor");
      deCompressorField.setAccessible(true);
      Decompressor decompressor = (Decompressor) deCompressorField.get(inMemoryMapOutput);
      return decompressor;
    } catch (Exception e) {
      throw new RssException("Get Decompressor fail " + e.getMessage());
    }
  }
}
