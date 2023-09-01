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

package org.apache.spark.shuffle.writer;

import org.apache.spark.SparkConf;
import org.apache.spark.shuffle.RssSparkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferManagerOptions {

  private static final Logger LOG = LoggerFactory.getLogger(BufferManagerOptions.class);

  private long bufferSize;
  private long serializerBufferSize;
  private long bufferSegmentSize;
  private long bufferSpillThreshold;
  private long preAllocatedBufferSize;
  private long requireMemoryInterval;
  private int requireMemoryRetryMax;

  public BufferManagerOptions(SparkConf sparkConf) {
    bufferSize = sparkConf.getSizeAsBytes(RssSparkConfig.RSS_WRITER_BUFFER_SIZE.key(),
        RssSparkConfig.RSS_WRITER_BUFFER_SIZE.defaultValue().get());
    serializerBufferSize = sparkConf.getSizeAsBytes(RssSparkConfig.RSS_WRITER_SERIALIZER_BUFFER_SIZE.key(),
        RssSparkConfig.RSS_WRITER_SERIALIZER_BUFFER_SIZE.defaultValue().get());
    bufferSegmentSize = sparkConf.getSizeAsBytes(RssSparkConfig.RSS_WRITER_BUFFER_SEGMENT_SIZE.key(),
        RssSparkConfig.RSS_WRITER_BUFFER_SEGMENT_SIZE.defaultValue().get());
    bufferSpillThreshold = sparkConf.getSizeAsBytes(RssSparkConfig.RSS_WRITER_BUFFER_SPILL_SIZE.key(),
        RssSparkConfig.RSS_WRITER_BUFFER_SPILL_SIZE.defaultValue().get());
    preAllocatedBufferSize = sparkConf.getSizeAsBytes(RssSparkConfig.RSS_WRITER_PRE_ALLOCATED_BUFFER_SIZE.key(),
        RssSparkConfig.RSS_WRITER_PRE_ALLOCATED_BUFFER_SIZE.defaultValue().get());
    requireMemoryInterval = sparkConf.get(RssSparkConfig.RSS_WRITER_REQUIRE_MEMORY_INTERVAL);
    requireMemoryRetryMax = sparkConf.get(RssSparkConfig.RSS_WRITER_REQUIRE_MEMORY_RETRY_MAX);
    LOG.info(RssSparkConfig.RSS_WRITER_BUFFER_SIZE.key() + "=" + bufferSize);
    LOG.info(RssSparkConfig.RSS_WRITER_BUFFER_SPILL_SIZE.key() + "=" + bufferSpillThreshold);
    LOG.info(RssSparkConfig.RSS_WRITER_PRE_ALLOCATED_BUFFER_SIZE.key() + "=" + preAllocatedBufferSize);
    checkBufferSize();
  }

  private void checkBufferSize() {
    if (bufferSize < 0) {
      throw new RuntimeException("Unexpected value of " + RssSparkConfig.RSS_WRITER_BUFFER_SIZE.key()
          + "=" + bufferSize);
    }
    if (bufferSpillThreshold < 0) {
      throw new RuntimeException("Unexpected value of " + RssSparkConfig.RSS_WRITER_BUFFER_SPILL_SIZE.key()
          + "=" + bufferSpillThreshold);
    }
    if (bufferSegmentSize > bufferSize) {
      LOG.warn(RssSparkConfig.RSS_WRITER_BUFFER_SEGMENT_SIZE.key() + "[" + bufferSegmentSize + "] should be less than "
          + RssSparkConfig.RSS_WRITER_BUFFER_SIZE.key() + "[" + bufferSize + "]");
    }
  }

  // limit of buffer size is 2G
  public int getBufferSize() {
    return parseToInt(bufferSize);
  }

  public int getSerializerBufferSize() {
    return parseToInt(serializerBufferSize);
  }

  public int getBufferSegmentSize() {
    return parseToInt(bufferSegmentSize);
  }

  private int parseToInt(long value) {
    if (value > Integer.MAX_VALUE) {
      value = Integer.MAX_VALUE;
    }
    return (int) value;
  }

  public long getPreAllocatedBufferSize() {
    return preAllocatedBufferSize;
  }

  public long getBufferSpillThreshold() {
    return bufferSpillThreshold;
  }

  public long getRequireMemoryInterval() {
    return requireMemoryInterval;
  }

  public int getRequireMemoryRetryMax() {
    return requireMemoryRetryMax;
  }
}
