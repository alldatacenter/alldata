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
package org.apache.drill.exec.cache;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.metrics.DrillMetrics;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.netty.buffer.DrillBuf;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

/**
 * Serializes vector containers to an output stream or from
 * an input stream.
 */

public class VectorSerializer {

  /**
   * Writes multiple VectorAccessible or VectorContainer
   * objects to an output stream.
   */

  public static class Writer implements Closeable
  {
    static final MetricRegistry metrics = DrillMetrics.getRegistry();
    static final String WRITER_TIMER = MetricRegistry.name(VectorAccessibleSerializable.class, "writerTime");

    private final WritableByteChannel channel;
    private final OutputStream output;
    private long timeNs;
    private int bytesWritten;

    private Writer(WritableByteChannel channel) {
      this.channel = channel;
      output = Channels.newOutputStream(channel);
    }

    public int write(VectorAccessible va) throws IOException {
      return write(va, null);
    }

    public int write(VectorAccessible va, SelectionVector2 sv2) throws IOException {
      checkNotNull(va);
      WritableBatch batch = WritableBatch.getBatchNoHVWrap(
          va.getRecordCount(), va, sv2 != null);
      try {
        return write(batch, sv2);
      } finally {
        batch.clear();
      }
    }

    public int write(WritableBatch batch, SelectionVector2 sv2) throws IOException {
      checkNotNull(batch);
      checkNotNull(channel);
      final Timer.Context timerContext = metrics.timer(WRITER_TIMER).time();

      final DrillBuf[] incomingBuffers = batch.getBuffers();
      final UserBitShared.RecordBatchDef batchDef = batch.getDef();
      int bytesWritten = batchDef.getSerializedSize();

      /* Write the metadata to the file */
      batchDef.writeDelimitedTo(output);

      /* If we have a selection vector, dump it to file first */
      if (sv2 != null) {
        final int dataLength = sv2.getCount() * SelectionVector2.RECORD_SIZE;
        ByteBuffer buffer = sv2.getBuffer(false).nioBuffer(0, dataLength);
        while (buffer.remaining() > 0) {
          bytesWritten += channel.write(buffer);
        }
      }

      /* Dump the array of ByteBuf's associated with the value vectors */
      for (DrillBuf buf : incomingBuffers) {
        /* dump the buffer into the OutputStream */
        ByteBuffer buffer = buf.nioBuffer();
        while (buffer.remaining() > 0) {
          bytesWritten += channel.write(buffer);
        }
      }

      timeNs += timerContext.stop();
      this.bytesWritten += bytesWritten;
      return bytesWritten;
    }

    @Override
    public void close() throws IOException {
      if (!channel.isOpen()) {
        return;
      }
      channel.close();
    }

    public long time(TimeUnit unit) {
      return unit.convert(timeNs, TimeUnit.NANOSECONDS);
    }

    public int getBytesWritten() { return bytesWritten; }
  }

  /**
   * Read one or more vector containers from an input stream.
   */

  public static class Reader {
    private final InputStream stream;
    private long timeNs;
    private final VectorAccessibleSerializable vas;

    public Reader(BufferAllocator allocator, InputStream stream) {
      this.stream = stream;
      vas = new VectorAccessibleSerializable(allocator);
    }

    public VectorContainer read() throws IOException {
      vas.readFromStream(stream);
      timeNs = vas.getTimeNs();
      return vas.get();
    }

    public SelectionVector2 sv2() { return vas.getSv2(); }

    public long timeNs() { return timeNs; }
  }

  public static Writer writer(WritableByteChannel channel) throws IOException {
    return new Writer(channel);
  }

  public static Reader reader(BufferAllocator allocator, InputStream stream) {
    return new Reader(allocator, stream);
  }
}
