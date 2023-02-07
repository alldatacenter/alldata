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
package org.apache.drill.exec.physical.impl.xsort;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.cache.VectorSerializer;
import org.apache.drill.exec.cache.VectorSerializer.Writer;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.spill.SpillSet;
import org.apache.drill.exec.record.SchemaUtil;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

/**
 * Holds a set of spilled batches, represented by a file on disk.
 * Handles reads from, and writes to the spill file. The data structure
 * is:
 * <ul>
 * <li>A pointer to a file that contains serialized batches.</li>
 * <li>When writing, each batch is appended to the output file.</li>
 * <li>When reading, iterates over each spilled batch, and for each
 * of those, each spilled record.</li>
 * </ul>
 * <p>
 * Starts out with no current batch. Defines the current batch to be the
 * (shell: schema without data) of the last batch spilled to disk.
 * <p>
 * When reading, has destructive read-once behavior: closing the
 * batch (after reading) deletes the underlying spill file.
 * <p>
 * This single class does three tasks: load data, hold data and
 * read data. This should be split into three separate classes. But,
 * the original (combined) structure is retained for expedience at
 * present.
 */
public class SpilledRun extends BatchGroup {
  private InputStream inputStream;
  private final String path;
  private final SpillSet spillSet;
  private final BufferAllocator allocator;
  private int spilledBatches;
  private long batchSizeBytes;
  private Writer writer;
  private VectorSerializer.Reader reader;

  public SpilledRun(SpillSet spillSet, String path, BufferAllocator allocator) throws IOException {
    super(null, allocator);
    this.spillSet = spillSet;
    this.path = path;
    this.allocator = allocator;
    writer = spillSet.writer(path);
  }

  public void spillBatch(VectorContainer newContainer) throws IOException {
    writer.write(newContainer);
    newContainer.zeroVectors();
    logger.trace("Wrote {} records in {} us", newContainer.getRecordCount(), writer.time(TimeUnit.MICROSECONDS));
    spilledBatches++;

    // Hold onto the husk of the last added container so that we have a
    // current container when starting to read rows back later.

    currentContainer = newContainer;
    currentContainer.setEmpty();
  }

  public void setBatchSize(long batchSize) {
    this.batchSizeBytes = batchSize;
  }

  public long getBatchSize() { return batchSizeBytes; }
  public String getPath() { return path; }

  @Override
  public int getNextIndex() {
    if (mergeIndex == getRecordCount()) {
      if (spilledBatches == 0) {
        return -1;
      }
      readBatch();

      // The pointer indicates the NEXT index, not the one we
      // return here. At this point, we just started reading a
      // new batch and have returned index 0. So, the next index
      // is 1.

      mergeIndex = 1;
      return 0;
    }
    return super.getNextIndex();
  }

  private void readBatch() {
    try {
      if (inputStream == null) {
        inputStream = spillSet.openForInput(path);
        reader = VectorSerializer.reader(allocator, inputStream);
      }
      Stopwatch watch = Stopwatch.createStarted();
      long start = allocator.getAllocatedMemory();
      VectorContainer c = reader.read();
      long end = allocator.getAllocatedMemory();
      logger.trace("Read {} records in {} us; size = {}, memory = {}",
                   c.getRecordCount(),
                   watch.elapsed(TimeUnit.MICROSECONDS),
                   (end - start), end);
      if (schema != null) {
        c = SchemaUtil.coerceContainer(c, schema, allocator);
      }
      spilledBatches--;
      currentContainer.zeroVectors();
      Iterator<VectorWrapper<?>> wrapperIterator = c.iterator();
      for (VectorWrapper<?> w : currentContainer) {
        TransferPair pair = wrapperIterator.next().getValueVector().makeTransferPair(w.getValueVector());
        pair.transfer();
      }
      currentContainer.setRecordCount(c.getRecordCount());
      c.zeroVectors();
    } catch (IOException e) {
      // Release any partially-loaded data.
      currentContainer.clear();
      throw UserException.dataReadError(e)
          .message("Failure while reading spilled data")
          .build(logger);
    }
  }

  /**
   * Close resources owned by this batch group. Each can fail; report
   * only the first error. This is cluttered because this class tries
   * to do multiple tasks. TODO: Split into multiple classes.
   */
  @Override
  public void close() throws IOException {
    try {
      AutoCloseables.close(super::close, this::closeWriter,
          this::closeInputStream, () -> spillSet.delete(path));
    } catch (Exception e) {
      throw (e instanceof IOException) ? (IOException) e : new IOException(e);
    }
  }

  private void closeInputStream() throws IOException {
    if (inputStream == null) {
      return;
    }
    long readLength = spillSet.getPosition(inputStream);
    spillSet.tallyReadBytes(readLength);
    inputStream.close();
    inputStream = null;
    reader = null;
    logger.trace("Summary: Read {} bytes from {}", readLength, path);
  }

  public void closeWriter() throws IOException {
    if (writer != null) {
      spillSet.close(writer);
      logger.trace("Summary: Wrote {} bytes in {} us to {}", writer.getBytesWritten(), writer.time(TimeUnit.MICROSECONDS), path);
      writer = null;
    }
  }
}