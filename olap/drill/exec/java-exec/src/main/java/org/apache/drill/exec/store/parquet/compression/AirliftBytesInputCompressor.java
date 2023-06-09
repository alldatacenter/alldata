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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;

import org.apache.parquet.bytes.ByteBufferAllocator;
import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.airlift.compress.Compressor;
import io.airlift.compress.Decompressor;
import io.airlift.compress.lz4.Lz4Compressor;
import io.airlift.compress.lz4.Lz4Decompressor;
import io.airlift.compress.lzo.LzoCompressor;
import io.airlift.compress.lzo.LzoDecompressor;
import io.airlift.compress.snappy.SnappyCompressor;
import io.airlift.compress.snappy.SnappyDecompressor;
import io.airlift.compress.zstd.ZstdCompressor;
import io.airlift.compress.zstd.ZstdDecompressor;

/**
 * A shim making an aircompressor (de)compressor available through the BytesInputCompressor
 * and BytesInputDecompressor interfaces.
 */
public class AirliftBytesInputCompressor implements CompressionCodecFactory.BytesInputCompressor, CompressionCodecFactory.BytesInputDecompressor {
  private static final Logger logger = LoggerFactory.getLogger(AirliftBytesInputCompressor.class);

  // name of the codec provided by this compressor
  private CompressionCodecName codecName;

  // backing aircompressor compressor
  private Compressor airComp;

  // backing aircompressor decompressor
  private Decompressor airDecomp;

  // the direct memory allocator to be used for (de)compression outputs
  private ByteBufferAllocator allocator;

  // all the direct memory buffers we've allocated, and must release
  private Deque<ByteBuffer> allocatedBuffers;

  public AirliftBytesInputCompressor(CompressionCodecName codecName, ByteBufferAllocator allocator) {
    this.codecName = codecName;

    switch (codecName) {
    case LZ4:
      airComp = new Lz4Compressor();
      airDecomp = new Lz4Decompressor();
      break;
    case LZO:
      airComp = new LzoCompressor();
      airDecomp = new LzoDecompressor();
      break;
    case SNAPPY:
      airComp = new SnappyCompressor();
      airDecomp = new SnappyDecompressor();
      break;
    case ZSTD:
      airComp = new ZstdCompressor();
      airDecomp = new ZstdDecompressor();
      break;
    default:
      throw new UnsupportedOperationException("Parquet compression codec is not supported: " + codecName);
    }

    this.allocator = allocator;
    this.allocatedBuffers = new LinkedList<>();

    logger.debug(
        "constructed a {} using a backing compressor of {}",
        getClass().getName(),
        airComp.getClass().getName()
    );
  }

  @Override
  public BytesInput compress(BytesInput bytes) throws IOException {
    ByteBuffer inBuf = bytes.toByteBuffer();

    logger.trace(
        "will use aircompressor to compress {} bytes from a {} containing a {}",
        bytes.size(),
        bytes.getClass().getName(),
        inBuf.getClass().getName()
    );

    // aircompressor tells us the maximum amount of output buffer we could need
    int maxOutLen = airComp.maxCompressedLength((int) bytes.size());
    ByteBuffer outBuf = allocator.allocate(maxOutLen);
    // track our allocation for later release in release()
    this.allocatedBuffers.push(outBuf);

    airComp.compress(inBuf, outBuf);

    // flip: callers expect the output buffer positioned at the start of data
    return BytesInput.from((ByteBuffer) outBuf.flip());
  }

  @Override
  public CompressionCodecName getCodecName() {
    return codecName;
  }

  @Override
  public BytesInput decompress(BytesInput bytes, int uncompressedSize) throws IOException {
    ByteBuffer inBuf = bytes.toByteBuffer();

    logger.trace(
        "will use aircompressor to decompress {} bytes from a {} containing a {}.",
        uncompressedSize,
        bytes.getClass().getName(),
        inBuf.getClass().getName()
    );

    ByteBuffer outBuf = allocator.allocate(uncompressedSize);
    // track our allocation for later release in release()
    this.allocatedBuffers.push(outBuf);

    airDecomp.decompress(inBuf, outBuf);

    // flip: callers expect the output buffer positioned at the start of data
    return BytesInput.from((ByteBuffer) outBuf.flip());
  }

  @Override
  public void decompress(ByteBuffer input, int compressedSize, ByteBuffer output, int uncompressedSize)
      throws IOException {
    logger.trace(
        "will use aircompressor to decompress {} bytes from a {} to a {}.",
        uncompressedSize,
        input.getClass().getName(),
        output.getClass().getName()
    );

    airDecomp.decompress(input, output);
  }

  @Override
  public void release() {
    int bufCount  = allocatedBuffers.size();

    // LIFO release order to try to reduce memory fragmentation.
    int i = 0;
    while (!allocatedBuffers.isEmpty()) {
      allocator.release(allocatedBuffers.pop());
      i++;
    }
    assert bufCount == i;

    logger.debug("released {} allocated buffers", bufCount);
  }
}
