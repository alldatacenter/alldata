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
package org.apache.drill.exec.util.filereader;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.hadoop.fs.ByteBufferReadable;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.parquet.hadoop.util.HadoopStreams;
import org.apache.parquet.io.SeekableInputStream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class DirectBufInputStream extends FilterInputStream {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DirectBufInputStream.class);

  protected boolean enableHints = true;
  protected String streamId; // a name for logging purposes only
  protected BufferAllocator allocator;
  /**
   * The length of the data we expect to read. The caller may, in fact,
   * ask for more or less bytes. However this is useful for providing hints where
   * the underlying InputStream supports hints (e.g. fadvise)
   */
  protected final long totalByteSize;

  // if true, the input stream willreturn EOF if we have read upto totalByteSize bytes
  protected final boolean enforceTotalByteSize;

  /**
   * The offset in the underlying stream to start reading from
   */
  protected final long startOffset;

  public DirectBufInputStream(InputStream in, BufferAllocator allocator, String id, long startOffset,
      long totalByteSize, boolean enforceTotalByteSize, boolean enableHints) {
    super(in);
    Preconditions.checkArgument(startOffset >= 0);
    Preconditions.checkArgument(totalByteSize >= 0);
    this.streamId = id;
    this.allocator = allocator;
    this.startOffset = startOffset;
    this.totalByteSize = totalByteSize;
    this.enforceTotalByteSize = enforceTotalByteSize;
    this.enableHints = enableHints;
  }

  public void init() throws IOException, UnsupportedOperationException {
    checkStreamSupportsByteBuffer();
    if (enableHints) {
      fadviseIfAvailable(getInputStream(), this.startOffset, this.totalByteSize);
    }
    getInputStream().seek(this.startOffset);
    return;
  }

  public int read() throws IOException {
    return getInputStream().read();
  }

  public synchronized int read(DrillBuf buf, int off, int len) throws IOException {
    buf.clear();
    ByteBuffer directBuffer = buf.nioBuffer(0, len);
    int lengthLeftToRead = len;
    SeekableInputStream seekableInputStream = HadoopStreams.wrap(getInputStream());
    while (lengthLeftToRead > 0) {
      if(logger.isTraceEnabled()) {
        logger.trace("PERF: Disk read start. {}, StartOffset: {}, TotalByteSize: {}", this.streamId, this.startOffset, this.totalByteSize);
      }
      Stopwatch timer = Stopwatch.createStarted();
      int bytesRead = seekableInputStream.read(directBuffer);
      if (bytesRead < 0) {
        return bytesRead;
      }
      lengthLeftToRead -= bytesRead;
      if(logger.isTraceEnabled()) {
        logger.trace(
            "PERF: Disk read complete. {}, StartOffset: {}, TotalByteSize: {}, BytesRead: {}, Time: {} ms",
            this.streamId, this.startOffset, this.totalByteSize, bytesRead,
            ((double) timer.elapsed(TimeUnit.MICROSECONDS)) / 1000);
      }
    }
    buf.writerIndex(len);
    return len;
  }

  public synchronized DrillBuf getNext(int bytes) throws IOException {
    DrillBuf b = allocator.buffer(bytes);
    int bytesRead = -1;
    try {
    bytesRead = read(b, 0, bytes);
    } catch (IOException e){
      b.release();
      throw e;
    }
    if (bytesRead < 0) {
      b.release();
      return null;
    }
    return b;
  }

  public long getPos() throws IOException {
    return getInputStream().getPos();
  }

  public boolean hasRemainder() throws IOException {
    // We use the following instead of "getInputStream.available() > 0" because
    // available() on HDFS seems to have issues with file sizes
    // that are greater than Integer.MAX_VALUE
    return (this.getPos() < (this.startOffset + this.totalByteSize));
  }

  protected FSDataInputStream getInputStream() throws IOException {
    // Make sure stream is open
    checkInputStreamState();
    return (FSDataInputStream) in;
  }

  protected void checkInputStreamState() throws IOException {
    if (in == null) {
      throw new IOException("Input stream is closed.");
    }
  }

  public synchronized void close() throws IOException {
    InputStream inp;
    if ((inp = in) != null) {
      in = null;
      inp.close();
    }
  }

  protected void checkStreamSupportsByteBuffer() throws UnsupportedOperationException {
    // Check input stream supports ByteBuffer
    if (!(in instanceof ByteBufferReadable)) {
      throw new UnsupportedOperationException("The input stream is not ByteBuffer readable.");
    }
  }

  protected static void fadviseIfAvailable(FSDataInputStream inputStream, long off, long n) {
    Method readAhead;
    final Class adviceType;

    try {
      adviceType = Class.forName("org.apache.hadoop.fs.FSDataInputStream$FadviseType");
    } catch (ClassNotFoundException e) {
      logger.info("Unable to call fadvise due to: {}", e.toString());
      readAhead = null;
      return;
    }
    try {
      Class<? extends FSDataInputStream> inputStreamClass = inputStream.getClass();
      readAhead =
          inputStreamClass.getMethod("adviseFile", new Class[] {adviceType, long.class, long.class});
    } catch (NoSuchMethodException e) {
      logger.info("Unable to call fadvise due to: {}", e.toString());
      readAhead = null;
      return;
    }
    if (readAhead != null) {
      Object[] adviceTypeValues = adviceType.getEnumConstants();
      for (int idx = 0; idx < adviceTypeValues.length; idx++) {
        if ((adviceTypeValues[idx]).toString().contains("SEQUENTIAL")) {
          try {
            readAhead.invoke(inputStream, adviceTypeValues[idx], off, n);
          } catch (IllegalAccessException e) {
            logger.info("Unable to call fadvise due to: {}", e.toString());
          } catch (InvocationTargetException e) {
            logger.info("Unable to call fadvise due to: {}", e.toString());
          }
          break;
        }
      }
    }
    return;
  }


}
