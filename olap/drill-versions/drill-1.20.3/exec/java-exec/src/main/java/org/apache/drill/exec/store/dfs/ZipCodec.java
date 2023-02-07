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

import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.io.compress.DefaultCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZIP codec implementation which cna read or create single entry.
 * <p/>
 * Note: Do not rename this class. Class naming must be 'ZipCodec' so it can be mapped by
 * {@link org.apache.hadoop.io.compress.CompressionCodecFactory} to the 'zip' extension.
 */
public class ZipCodec extends DefaultCodec {

  private static final String EXTENSION = ".zip";

  @Override
  public CompressionOutputStream createOutputStream(OutputStream out) throws IOException {
    return new ZipCompressionOutputStream(new ResetableZipOutputStream(out));
  }

  @Override
  public CompressionInputStream createInputStream(InputStream in) throws IOException {
    return new ZipCompressionInputStream(new ZipInputStream(in));
  }

  @Override
  public String getDefaultExtension() {
    return EXTENSION;
  }

  /**
   * Reads only first entry from {@link ZipInputStream},
   * other entries if present will be ignored.
   */
  private static class ZipCompressionInputStream extends CompressionInputStream {

    ZipCompressionInputStream(ZipInputStream in) throws IOException {
      super(in);
      // positions stream at the beginning of the first entry data
      in.getNextEntry();
    }

    @Override
    public int read() throws IOException {
      return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return in.read(b, off, len);
    }

    @Override
    public void resetState() throws IOException {
      in.reset();
    }

    @Override
    public void close() throws IOException {
      try {
        ((ZipInputStream) in).closeEntry();
      } finally {
        super.close();
      }
    }
  }

  /**
   * Extends {@link ZipOutputStream} to allow resetting compressor stream,
   * required by {@link CompressionOutputStream} implementation.
   */
  private static class ResetableZipOutputStream extends ZipOutputStream {

    ResetableZipOutputStream(OutputStream out) {
      super(out);
    }

    void resetState() {
      def.reset();
    }
  }

  /**
   * Writes given data into ZIP archive by placing all data in one entry with default naming.
   */
  private static class ZipCompressionOutputStream extends CompressionOutputStream {

    private static final String DEFAULT_ENTRY_NAME = "entry.out";

    ZipCompressionOutputStream(ResetableZipOutputStream out) throws IOException {
      super(out);
      ZipEntry zipEntry = new ZipEntry(DEFAULT_ENTRY_NAME);
      out.putNextEntry(zipEntry);
    }

    @Override
    public void write(int b) throws IOException {
      out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      out.write(b, off, len);
    }

    @Override
    public void finish() throws IOException {
      ((ResetableZipOutputStream) out).closeEntry();
    }

    @Override
    public void resetState() {
      ((ResetableZipOutputStream) out).resetState();
    }
  }
}
