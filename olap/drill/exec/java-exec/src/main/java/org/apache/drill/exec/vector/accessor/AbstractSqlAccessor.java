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

public abstract class AbstractSqlAccessor implements SqlAccessor {

  @Override
  public abstract boolean isNull(int rowOffset);

  @Override
  public BigDecimal getBigDecimal(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("BigDecimal");
  }

  @Override
  public boolean getBoolean(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("boolean");
  }

  @Override
  public byte getByte(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("byte");
  }

  @Override
  public byte[] getBytes(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("byte[]");
  }

  @Override
  public Date getDate(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("Date");
  }

  @Override
  public double getDouble(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("double");
  }

  @Override
  public float getFloat(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("float");
  }

  @Override
  public int getInt(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("int");
  }

  @Override
  public long getLong(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("long");
  }

  @Override
  public short getShort(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("short");
  }

  @Override
  public InputStream getStream(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("InputStream");
  }

  @Override
  public char getChar(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("Char");
  }

  @Override
  public Reader getReader(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("Reader");
  }

  @Override
  public String getString(int rowOffset) throws InvalidAccessException{
    Object o = getObject(rowOffset);
    return o != null ? o.toString() : null;
  }

  @Override
  public Time getTime(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("Time");
  }

  @Override
  public Timestamp getTimestamp(int rowOffset) throws InvalidAccessException{
    throw newInvalidAccessException("Timestamp");
  }


  private InvalidAccessException newInvalidAccessException(String name) {
    return new InvalidAccessException(
        String.format(
            "Requesting value of type %s for an object of type %s:%s is not allowed.",
             name, getType().getMinorType().name(), getType().getMode().name()));
  }
}
