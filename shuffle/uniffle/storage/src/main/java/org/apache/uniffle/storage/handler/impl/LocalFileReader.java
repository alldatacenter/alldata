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

package org.apache.uniffle.storage.handler.impl;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.storage.api.FileReader;

public class LocalFileReader implements FileReader, Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(LocalFileReader.class);
  private String path;
  private DataInputStream dataInputStream;

  public LocalFileReader(String path) throws Exception {
    this.path = path;
    dataInputStream = new DataInputStream(new FileInputStream(path));
  }

  public byte[] read(long offset, int length) {
    try {
      long targetSkip = offset;
      // comments from skip API:
      // The skip method may, for a variety of reasons,
      // end up skipping over some smaller number of bytes, possibly 0
      // the result should be checked and try again until skip expectation length
      while (targetSkip > 0) {
        long realSkip = dataInputStream.skip(targetSkip);
        if (realSkip == -1) {
          throw new RuntimeException("Unexpected EOF when skip bytes");
        }
        targetSkip -= realSkip;
        if (targetSkip > 0) {
          LOG.warn("Got unexpected skip for path:" + path + " with offset["
              + offset + "], length[" + length + "], remain[" + targetSkip + "]");
        }
      }
      byte[] buf = new byte[length];
      dataInputStream.readFully(buf);
      return buf;
    } catch (Exception e) {
      LOG.warn("Can't read data for path:" + path + " with offset[" + offset + "], length[" + length + "]", e);
    }
    return new byte[0];
  }

  public byte[] read() {
    try {
      return IOUtils.toByteArray(dataInputStream);
    } catch (IOException e) {
      LOG.error("Fail to read all data from {}", path, e);
      return new byte[0];
    }
  }

  @Override
  public synchronized void close() {
    if (dataInputStream != null) {
      try {
        dataInputStream.close();
      } catch (IOException ioe) {
        LOG.warn("Error happen when close " + path, ioe);
      }
    }
  }
}
