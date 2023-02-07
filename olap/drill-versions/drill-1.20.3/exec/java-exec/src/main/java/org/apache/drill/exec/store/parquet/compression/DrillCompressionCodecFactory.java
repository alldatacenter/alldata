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
package org.apache.drill.exec.store.parquet.compression;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.bytes.ByteBufferAllocator;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.CodecFactory;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A delegating compression codec factory that returns (de)compressors based on
 * https://github.com/airlift/aircompressor when possible and falls back to
 * parquet-mr otherwise.  The aircompressor lib was introduced into Drill
 * because of difficulties encountered with the JNI-based implementations of
 * lzo, lz4 and zstd in parquet-mr.
 *
 * By modifying the constant AIRCOMPRESSOR_CODECS it is possible to choose
 * which codecs should be routed to which lib.  In addition, this class
 * implements parquet-mr's CompressionCodecFactory interface meaning that
 * swapping this factory for e.g. one in parquet-mr will have minimal impact
 * on code in Drill relying on a CompressCodecFactory.
 *
 */
public class DrillCompressionCodecFactory implements CompressionCodecFactory {
  private static final Logger logger = LoggerFactory.getLogger(DrillCompressionCodecFactory.class);

  // The set of codecs to be handled by aircompressor
  private static final Set<CompressionCodecName> AIRCOMPRESSOR_CODECS = new HashSet<>(
      Arrays.asList(
        CompressionCodecName.LZ4,
        CompressionCodecName.LZO,
        CompressionCodecName.SNAPPY,
        CompressionCodecName.ZSTD
      )
  );

  // pool of reusable thread-safe aircompressor compressors (parquet-mr's factory has its own)
  private final Map<CompressionCodecName, AirliftBytesInputCompressor> airCompressors = new HashMap<>();

  // fallback parquet-mr compression codec factory
  // TODO: uncomment once PARQUET-2126 is fixed.
  // private final CompressionCodecFactory parqCodecFactory;

  // direct memory allocator to be used during (de)compression
  private final ByteBufferAllocator allocator;

  // Start: members for working around a CodecFactory concurrency bug c.f. DRILL-8139
  // TODO: remove once PARQUET-2126 is fixed.
  private final Deque<CompressionCodecFactory> singleUseFactories;
  private final Configuration config;
  private final int pageSize;
  // End

  // static builder method, solely to mimick the parquet-mr API as closely as possible
  public static CompressionCodecFactory createDirectCodecFactory(Configuration config, ByteBufferAllocator allocator,
      int pageSize) {
    return new DrillCompressionCodecFactory(config, allocator, pageSize);
  }

  public DrillCompressionCodecFactory(Configuration config, ByteBufferAllocator allocator, int pageSize) {
    this.config = config;
    this.allocator = allocator;
    this.pageSize = pageSize;
    this.singleUseFactories = new LinkedList<>();
    // TODO: uncomment once PARQUET-2126 is fixed.
    // this.parqCodecFactory = CodecFactory.createDirectCodecFactory(config, allocator, pageSize);
  }

  @Override
  public synchronized BytesInputCompressor getCompressor(CompressionCodecName codecName) {
    if (AIRCOMPRESSOR_CODECS.contains(codecName)) {
      return airCompressors.computeIfAbsent(
          codecName,
          c -> new AirliftBytesInputCompressor(codecName, allocator)
      );
    } else {
      // Work around PARQUET-2126: construct a new codec factory every time to
      // avoid a concurrrency bug c.f. DRILL-8139.  Fortunately, constructing
      // and releasing codec factories appears to be light weight.
      CompressionCodecFactory ccf = CodecFactory.createDirectCodecFactory(config, allocator, pageSize);
      // hold onto a reference for later release()
      singleUseFactories.add(ccf);
      return ccf.getCompressor(codecName);

      // TODO: replace the above with the below PARQUET-2126 is fixed
      // return parqCodecFactory.getDecompressor(codecName);
    }
  }

  @Override
  public synchronized BytesInputDecompressor getDecompressor(CompressionCodecName codecName) {
    if (AIRCOMPRESSOR_CODECS.contains(codecName)) {
      return airCompressors.computeIfAbsent(
          codecName,
          c -> new AirliftBytesInputCompressor(codecName, allocator)
      );
    } else {
      // Work around PARQUET-2126: construct a new codec factory every time to
      // avoid a concurrrency bug c.f. DRILL-8139.  Fortunately, constructing
      // and releasing codec factories appears to be light weight.
      CompressionCodecFactory ccf = CodecFactory.createDirectCodecFactory(config, allocator, pageSize);
      // hold onto a reference for later release()
      singleUseFactories.add(ccf);
      return ccf.getDecompressor(codecName);

      // TODO: replace the above with the below PARQUET-2126 is fixed
      // return parqCodecFactory.getDecompressor(codecName);
    }
  }

  @Override
  public synchronized void release() {
    // TODO: uncomment once PARQUET-2126 is fixed.
    // parqCodecFactory.release();
    // logger.debug("released {}", parqCodecFactory);

    airCompressors.values().forEach(AirliftBytesInputCompressor::release);
    logger.debug("released {} aircompressors", airCompressors.size());
    airCompressors.clear();

    // TODO: remove once PARQUET-2126 is fixed.
    singleUseFactories.forEach(CompressionCodecFactory::release);
    logger.debug("released {} single-use codec factories.", singleUseFactories.size());
    singleUseFactories.clear();
  }
}
