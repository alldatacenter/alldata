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
package org.apache.drill.exec.vector.complex.fn;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.apache.hadoop.fs.Seekable;

/**
 * A ByteArrayInputStream that supports the HDFS Seekable API.
 */
public class SeekableBAIS extends ByteArrayInputStream implements Seekable {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SeekableBAIS.class);


  public SeekableBAIS(byte[] buf, int offset, int length) {
    super(buf, offset, length);
  }

  public SeekableBAIS(byte[] buf) {
    super(buf);
  }

  @Override
  public void seek(long pos) throws IOException {
    if(pos > buf.length){
      throw new EOFException();
    }
    this.pos = (int) pos;
    this.count = buf.length - (int) pos;
  }

  @Override
  public long getPos() throws IOException {
    return pos;
  }

  @Override
  public boolean seekToNewSource(long targetPos) throws IOException {
    return false;
  }



}
