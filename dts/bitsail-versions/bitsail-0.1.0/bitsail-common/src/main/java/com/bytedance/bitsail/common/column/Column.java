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

package com.bytedance.bitsail.common.column;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public abstract class Column implements Serializable, Comparable<Column> {

  private static final long serialVersionUID = 1L;

  private Object rawData;

  private int byteSize;

  public Column(final Object object, int byteSize) {
    this.rawData = object;
    this.byteSize = byteSize;
  }

  public Object getRawData() {
    return this.rawData;
  }

  protected void setRawData(Object rawData) {
    this.rawData = rawData;
  }

  public int getByteSize() {
    return this.byteSize;
  }

  protected void setByteSize(int byteSize) {
    this.byteSize = byteSize;
  }

  public abstract Long asLong();

  public abstract Double asDouble();

  public abstract String asString();

  public abstract Date asDate();

  public abstract byte[] asBytes();

  public abstract Boolean asBoolean();

  public abstract BigDecimal asBigDecimal();

  public abstract BigInteger asBigInteger();

  @Override
  public String toString() {
    return JSON.toJSONString(this);
  }

}
