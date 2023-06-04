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

package com.bytedance.bitsail.conversion.hive.extractor;

import org.apache.hadoop.io.Text;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class StringText extends Text {
  private final String text;

  public StringText(String text) {
    this.text = text;
  }

  private void lazySetBytes() {
    if ((super.getBytes() == null || super.getBytes().length == 0) && this.text != null) {
      set(this.text);
    }
  }

  @Override
  public void set(byte[] utf8, int start, int len) {
    lazySetBytes();
    super.set(utf8, start, len);
  }

  @Override
  public byte[] copyBytes() {
    lazySetBytes();
    return super.copyBytes();
  }

  @Override
  public byte[] getBytes() {
    lazySetBytes();
    return super.getBytes();
  }

  @Override
  public int getLength() {
    lazySetBytes();
    return super.getLength();
  }

  @Override
  public int charAt(int position) {
    return text.charAt(position);
  }

  @Override
  public int find(String what) {
    lazySetBytes();
    return super.find(what);
  }

  @Override
  public int find(String what, int start) {
    lazySetBytes();
    return super.find(what, start);
  }

  @Override
  public void append(byte[] utf8, int start, int len) {
    lazySetBytes();
    super.append(utf8, start, len);
  }

  @Override
  public void clear() {
    lazySetBytes();
    super.clear();
  }

  @Override
  public String toString() {
    return text;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    lazySetBytes();
    super.readFields(in);
  }

  @Override
  public void readFields(DataInput in, int maxLength) throws IOException {
    lazySetBytes();
    super.readFields(in, maxLength);
  }

  @Override
  public void readWithKnownLength(DataInput in, int len) throws IOException {
    lazySetBytes();
    super.readWithKnownLength(in, len);
  }

  @Override
  public void write(DataOutput out) throws IOException {
    lazySetBytes();
    super.write(out);
  }

  @Override
  public void write(DataOutput out, int maxLength) throws IOException {
    lazySetBytes();
    super.write(out, maxLength);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof StringText && text.equals(((StringText) o).text);
  }

  @Override
  public int hashCode() {
    return this.text.hashCode();
  }
}
