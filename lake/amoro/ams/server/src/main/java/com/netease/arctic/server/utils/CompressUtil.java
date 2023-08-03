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

package com.netease.arctic.server.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressUtil {

  /**
   * Compress the given data using gzip.
   */
  public static byte[] gzip(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream zipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
      zipOutputStream.write(bytes);
      zipOutputStream.finish();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Decompress the given data using gzip.
   */
  public static byte[] unGzip(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
      while ((len = gzipInputStream.read(buffer)) > 0) {
        byteArrayOutputStream.write(buffer, 0, len);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return byteArrayOutputStream.toByteArray();
  }
}
