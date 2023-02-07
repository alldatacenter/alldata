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
package org.apache.drill.exec.store.dfs;

import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.shaded.guava.com.google.common.io.ByteStreams;
import org.apache.hadoop.classification.InterfaceAudience.LimitedPrivate;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.ReadOption;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.ByteBufferPool;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.EnumSet;


/**
 * Wrapper around FSDataInputStream to collect IO Stats.
 */
public class DrillFSDataInputStream extends FSDataInputStream {
  private final FSDataInputStream underlyingIs;
  private final OpenFileTracker openFileTracker;
  private final OperatorStats operatorStats;

  public DrillFSDataInputStream(FSDataInputStream in, OperatorStats operatorStats) {
    this(in, operatorStats, null);
  }

  public DrillFSDataInputStream(FSDataInputStream in, OperatorStats operatorStats,
      OpenFileTracker openFileTracker) {
    super(new WrappedInputStream(in, operatorStats));
    underlyingIs = in;
    this.openFileTracker = openFileTracker;
    this.operatorStats = operatorStats;
  }

  @Override
  public synchronized void seek(long desired) throws IOException {
    underlyingIs.seek(desired);
  }

  @Override
  public long getPos() throws IOException {
    return underlyingIs.getPos();
  }

  @Override
  public int read(long position, byte[] buffer, int offset, int length) throws IOException {
    operatorStats.startWait();
    try {
      return underlyingIs.read(position, buffer, offset, length);
    } finally {
      operatorStats.stopWait();
    }
  }

  @Override
  public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
    operatorStats.startWait();
    try {
      underlyingIs.readFully(position, buffer, offset, length);
    } finally {
      operatorStats.stopWait();
    }
  }

  @Override
  public void readFully(long position, byte[] buffer) throws IOException {
    operatorStats.startWait();
    try {
      underlyingIs.readFully(position, buffer);
    } finally {
      operatorStats.stopWait();
    }
  }

  @Override
  public boolean seekToNewSource(long targetPos) throws IOException {
    return underlyingIs.seekToNewSource(targetPos);
  }

  @Override
  @LimitedPrivate({"HDFS"})
  public InputStream getWrappedStream() {
    return underlyingIs.getWrappedStream();
  }

  @Override
  public int read(ByteBuffer buf) throws IOException {
    operatorStats.startWait();
    try {
      return underlyingIs.read(buf);
    } finally {
      operatorStats.stopWait();
    }
  }

  @Override
  public FileDescriptor getFileDescriptor() throws IOException {
    return underlyingIs.getFileDescriptor();
  }

  @Override
  public void setReadahead(Long readahead) throws IOException, UnsupportedOperationException {
    underlyingIs.setReadahead(readahead);
  }

  @Override
  public void setDropBehind(Boolean dropBehind) throws IOException, UnsupportedOperationException {
    underlyingIs.setDropBehind(dropBehind);
  }

  @Override
  public ByteBuffer read(ByteBufferPool bufferPool, int maxLength, EnumSet<ReadOption> opts) throws IOException, UnsupportedOperationException {
    operatorStats.startWait();
    try {
      return underlyingIs.read(bufferPool, maxLength, opts);
    } finally {
      operatorStats.stopWait();
    }
  }

  @Override
  public void releaseBuffer(ByteBuffer buffer) {
    underlyingIs.releaseBuffer(buffer);
  }

  @Override
  public int read() throws IOException {
    return underlyingIs.read();
  }

  @Override
  public long skip(long n) throws IOException {
    return underlyingIs.skip(n);
  }

  @Override
  public int available() throws IOException {
    return underlyingIs.available();
  }

  @Override
  public void close() throws IOException {
    if (openFileTracker != null) {
      openFileTracker.fileClosed(this);
    }
    underlyingIs.close();
  }

  @Override
  public void mark(int readlimit) {
    underlyingIs.mark(readlimit);
  }

  @Override
  public void reset() throws IOException {
    underlyingIs.reset();
  }

  @Override
  public boolean markSupported() {
    return underlyingIs.markSupported();
  }

  @Override
  public void unbuffer() {
    underlyingIs.unbuffer();
  }

  /**
   * We need to wrap the FSDataInputStream inside a InputStream, because read() method in InputStream is
   * overridden in FilterInputStream (super class of FSDataInputStream) as final, so we can not override in
   * DrillFSDataInputStream.
   */
  private static class WrappedInputStream extends InputStream implements Seekable, PositionedReadable {
    final FSDataInputStream is;
    final OperatorStats operatorStats;

    WrappedInputStream(FSDataInputStream is, OperatorStats operatorStats) {
      this.is = is;
      this.operatorStats = operatorStats;
    }

    /**
     * Most of the read are going to be block reads which use {@link #read(byte[], int,
     * int)}. So not adding stats for single byte reads.
     */
    @Override
    public int read() throws IOException {
      return is.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      operatorStats.startWait();
      try {
        return readBytes(b, off, len);
      } finally {
        operatorStats.stopWait();
      }
    }

    @Override
    public int read(byte[] b) throws IOException {
      operatorStats.startWait();
      try {
        return readBytes(b, 0, b.length);
      } finally {
        operatorStats.stopWait();
      }
    }

    /**
     * Reads up to {@code len} bytes of data from the input stream into an array of bytes.
     * This method guarantees that regardless of the underlying stream implementation,
     * the byte array will be populated with either {@code len} bytes or
     * all available in stream bytes if they are less than {@code len}.
     */
    private int readBytes(byte[] b, int off, int len) throws IOException {
      int read = ByteStreams.read(is, b, off, len);
      if (read == 0 && len > 0) {
        // ByteStreams.read() doesn't return -1 at EOF, but returns 0,
        // if no bytes available in the stream
        return -1;
      }
      return read;
    }

    @Override
    public long skip(long n) throws IOException {
      return is.skip(n);
    }

    @Override
    public int available() throws IOException {
      return is.available();
    }

    @Override
    public void close() throws IOException {
      is.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
      is.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
      is.reset();
    }

    @Override
    public boolean markSupported() {
      return is.markSupported();
    }

    @Override
    public int read(long position, byte[] buffer, int offset, int length) throws IOException {
      return is.read(position, buffer, offset, length);
    }

    @Override
    public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
      is.readFully(position, buffer, offset, length);
    }

    @Override
    public void readFully(long position, byte[] buffer) throws IOException {
      is.readFully(position, buffer);
    }

    @Override
    public void seek(long pos) throws IOException {
      is.seek(pos);
    }

    @Override
    public long getPos() throws IOException {
      return is.getPos();
    }

    @Override
    public boolean seekToNewSource(long targetPos) throws IOException {
      return is.seekToNewSource(targetPos);
    }
  }
}
