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
package org.apache.drill.exec.store;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

public class CachedSingleFileSystem extends FileSystem {

  private ByteBuf file;
  private Path path;

  public CachedSingleFileSystem(Path path) throws IOException {
    this.path = path;
    File f = new File(path.toUri().getPath());
    long length = f.length();
    if (length > Integer.MAX_VALUE) {
      throw new UnsupportedOperationException("Cached file system only supports files of less than 2GB.");
    }
    try (InputStream is = new BufferedInputStream(new FileInputStream(path.toUri().getPath()))) {
      byte[] buffer = new byte[64*1024];
      this.file = UnpooledByteBufAllocator.DEFAULT.directBuffer((int) length);
      int read;
      while ( (read = is.read(buffer)) > 0) {
        file.writeBytes(buffer, 0, read);
      }
    }
  }

  @Override
  public void close() throws IOException{
    file.release();
    super.close();
  }

  @Override
  public FSDataOutputStream append(Path arg0, int arg1, Progressable arg2) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FSDataOutputStream create(Path arg0, FsPermission arg1, boolean arg2, int arg3, short arg4, long arg5,
      Progressable arg6) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete(Path arg0) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete(Path arg0, boolean arg1) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileStatus getFileStatus(Path arg0) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getUri() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getWorkingDirectory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileStatus[] listStatus(Path arg0) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean mkdirs(Path path, FsPermission arg1) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FSDataInputStream open(Path path, int arg1) throws IOException {
    if (!path.toString().equals(this.path)) {
      throw new IOException(String.format("You requested file %s but this cached single file system only has the file %s.", path.toString(), this.path));
    }
    return new FSDataInputStream(new CachedFSDataInputStream(file.slice()));
  }

  @Override
  public boolean rename(Path arg0, Path arg1) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setWorkingDirectory(Path arg0) {
    throw new UnsupportedOperationException();
  }


  private class CachedFSDataInputStream extends ByteBufInputStream implements Seekable, PositionedReadable{
    private ByteBuf buf;
    public CachedFSDataInputStream(ByteBuf buffer) {
      super(buffer);
      this.buf = buffer;

    }

    @Override
    public long getPos() throws IOException {
      return buf.readerIndex();
    }

    @Override
    public void seek(long arg0) throws IOException {
      buf.readerIndex((int) arg0);
    }

    @Override
    public boolean seekToNewSource(long arg0) throws IOException {
      return false;
    }

    @Override
    public int read(long pos, byte[] buffer, int offset, int length) throws IOException {
      ByteBuf local = buf.slice( (int) pos, (int) Math.min( buf.capacity() - pos, length));
      local.readBytes(buffer, offset, buf.capacity());
      return buf.capacity();
    }

    @Override
    public void readFully(long pos, byte[] buffer) throws IOException {
      readFully(pos, buffer, 0, buffer.length);
    }

    @Override
    public void readFully(long pos, byte[] buffer, int offset, int length) throws IOException {
      if (length + pos > buf.capacity()) {
        throw new IOException("Read was too big.");
      }
      read(pos, buffer, offset, length);
    }
  }

}
