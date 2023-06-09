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
package org.apache.drill.common.util;


import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.exception.ExceptionUtils;


public class DataInputInputStream extends InputStream {

  private final DataInput in;
  private boolean closed = false;

  /**
   * Construct an InputStream from the given DataInput. If 'in'
   * is already an InputStream, simply returns it. Otherwise, wraps
   * it in an InputStream.
   * @param in the DataInput to wrap
   * @return an InputStream instance that inputs to 'in'
   */

  public static InputStream constructInputStream(DataInput in) {
    if (in instanceof InputStream) {
      return (InputStream)in;
    } else {
      return new DataInputInputStream(in);
    }
  }

  private DataInputInputStream(DataInput in) {
    this.in = in;
  }

  @Override
  public void close() {
    this.closed = true;
  }

  @Override
  public int available() {
    return this.closed ? 0 : 1;
  }

  @Override
  public int read() throws IOException {
    return in.readByte();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    for (int i = off; i < off + len; i++) {
      try {
        b[i] = in.readByte();
      } catch(Exception e) {
        if (ExceptionUtils.getRootCause(e) instanceof EOFException) {
          return i - off;
        } else {
          throw e;
        }
      }
    }
    return len;
  }

  @Override
  public int read(byte[] b) throws IOException {
    for (int i = 0; i < b.length; i++) {
      try {
        b[i] = in.readByte();
      } catch (EOFException e) {
        close();
        return i;
      }
    }
    return b.length;
  }
}
