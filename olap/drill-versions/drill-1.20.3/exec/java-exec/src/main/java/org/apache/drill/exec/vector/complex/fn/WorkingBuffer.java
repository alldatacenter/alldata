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

import io.netty.buffer.DrillBuf;

import java.io.IOException;

import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;

class WorkingBuffer {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorkingBuffer.class);

  private DrillBuf workBuf;

  public WorkingBuffer(DrillBuf workBuf) {
    this.workBuf = workBuf;
  }

  private void ensure(int length) {
    workBuf = workBuf.reallocIfNeeded(length);
  }

  public void prepareVarCharHolder(String value, VarCharHolder h) throws IOException {
    byte[] b = value.getBytes(Charsets.UTF_8);
    ensure(b.length);
    workBuf.setBytes(0, b);
    h.start = 0;
    h.end = b.length;
    h.buffer = workBuf;
  }

  public int prepareVarCharHolder(String value) throws IOException {
    byte[] b = value.getBytes(Charsets.UTF_8);
    ensure(b.length);
    workBuf.setBytes(0, b);
    return b.length;
  }

  public void prepareBinary(byte[] b, VarBinaryHolder h) throws IOException {
    ensure(b.length);
    workBuf.setBytes(0, b);
    h.start = 0;
    h.end = b.length;
    h.buffer = workBuf;
  }

  public DrillBuf getBuf(){
    return workBuf;
  }

}
