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
package org.apache.drill.exec.vector.accessor;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.vector.ValueVector;

/**
 * A decorating accessor that returns null for indices that is beyond underlying vector's capacity.
 */
public class BoundCheckingAccessor implements SqlAccessor {
  private final ValueVector vector;
  private final SqlAccessor delegate;

  public BoundCheckingAccessor(ValueVector vector, SqlAccessor inner) {
    this.vector = vector;
    this.delegate = inner;
  }

  @Override
  public MajorType getType() {
    return delegate.getType();
  }

  @Override
  public Class<?> getObjectClass() {
    return delegate.getObjectClass();
  }

  @Override
  public boolean isNull(int rowOffset) {
    return delegate.isNull(rowOffset);
  }

  @Override
  public BigDecimal getBigDecimal(int rowOffset) throws InvalidAccessException {
    return delegate.getBigDecimal(rowOffset);
  }

  @Override
  public boolean getBoolean(int rowOffset) throws InvalidAccessException {
    return delegate.getBoolean(rowOffset);
  }

  @Override
  public byte getByte(int rowOffset) throws InvalidAccessException {
    return delegate.getByte(rowOffset);
  }

  @Override
  public byte[] getBytes(int rowOffset) throws InvalidAccessException {
    return delegate.getBytes(rowOffset);
  }

  @Override
  public Date getDate(int rowOffset) throws InvalidAccessException {
    return delegate.getDate(rowOffset);
  }

  @Override
  public double getDouble(int rowOffset) throws InvalidAccessException {
    return delegate.getDouble(rowOffset);
  }

  @Override
  public float getFloat(int rowOffset) throws InvalidAccessException {
    return delegate.getFloat(rowOffset);
  }

  @Override
  public char getChar(int rowOffset) throws InvalidAccessException {
    return delegate.getChar(rowOffset);
  }

  @Override
  public int getInt(int rowOffset) throws InvalidAccessException {
    return delegate.getInt(rowOffset);
  }

  @Override
  public long getLong(int rowOffset) throws InvalidAccessException {
    return delegate.getLong(rowOffset);
  }

  @Override
  public short getShort(int rowOffset) throws InvalidAccessException {
    return delegate.getShort(rowOffset);
  }

  @Override
  public InputStream getStream(int rowOffset) throws InvalidAccessException {
    return delegate.getStream(rowOffset);
  }

  @Override
  public Reader getReader(int rowOffset) throws InvalidAccessException {
    return delegate.getReader(rowOffset);
  }

  @Override
  public String getString(int rowOffset) throws InvalidAccessException {
    return delegate.getString(rowOffset);
  }

  @Override
  public Time getTime(int rowOffset) throws InvalidAccessException {
    return delegate.getTime(rowOffset);
  }

  @Override
  public Timestamp getTimestamp(int rowOffset) throws InvalidAccessException {
    return delegate.getTimestamp(rowOffset);
  }

  /**
   * Returns an instance sitting at the given index if exists, null otherwise.
   *
   * @see org.apache.drill.exec.vector.accessor.SqlAccessor#getObject(int)
   */
  @Override
  public Object getObject(int rowOffset) throws InvalidAccessException {
    // In case some vectors have fewer values than others, and callee invokes
    // this method with index >= getValueCount(), this should still yield null.
    final ValueVector.Accessor accessor = vector.getAccessor();
    if (rowOffset < accessor.getValueCount()) {
      return delegate.getObject(rowOffset);
    }
    return null;
  }
}
