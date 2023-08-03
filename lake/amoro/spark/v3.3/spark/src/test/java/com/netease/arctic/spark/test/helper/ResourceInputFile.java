/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.test.helper;

import org.apache.iceberg.io.ByteBufferInputStream;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.SeekableInputStream;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class ResourceInputFile implements InputFile {

  public static ResourceInputFile newFile(ClassLoader loader, String resource) {
    List<ByteBuffer> bf = Lists.newArrayList();
    int size = 0;
    URL url = loader.getResource(resource);
    Preconditions.checkNotNull(url);
    try (InputStream in = loader.getResourceAsStream(resource)) {
      Preconditions.checkNotNull(in);

      byte[] buff = new byte[1024];
      int numRead = 0;
      while ((numRead = in.read(buff)) > 0) {
        size += numRead;
        byte[] readerBytes = Arrays.copyOfRange(buff, 0, numRead);
        bf.add(ByteBuffer.wrap(readerBytes));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return new ResourceInputFile(bf, size, url.getFile());
  }

  private final List<ByteBuffer> bf;
  private final long length;
  private final String location;

  protected ResourceInputFile(List<ByteBuffer> bf, int length, String location) {
    this.length = length;
    this.bf = bf;
    this.location = location;
  }

  @Override
  public long getLength() {
    return this.length;
  }

  @Override
  public SeekableInputStream newStream() {
    return ByteBufferInputStream.wrap(bf);
  }

  @Override
  public String location() {
    return location;
  }

  @Override
  public boolean exists() {
    return true;
  }
}
