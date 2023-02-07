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

import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import java.math.BigDecimal;

public class AbstractKeyAccessor implements KeyAccessor {

  private static final String UNSUPPORTED_MESSAGE_TEMPLATE = "%s does not support a key of type %s.";

  protected final DictReader dictReader;
  protected final ScalarReader keyReader;

  protected AbstractKeyAccessor(DictReader dictReader, ScalarReader keyReader) {
    this.dictReader = dictReader;
    this.keyReader = keyReader;
  }

  @Override
  public boolean find(boolean key) {
    throw unsupported("boolean");
  }

  @Override
  public boolean find(int key) {
    throw unsupported("int");
  }

  @Override
  public boolean find(BigDecimal key) {
    throw unsupported("BigDecimal");
  }

  @Override
  public boolean find(double key) {
    throw unsupported("double");
  }

  @Override
  public boolean find(long key) {
    throw unsupported("long");
  }

  @Override
  public boolean find(String key) {
    throw unsupported("String");
  }

  @Override
  public boolean find(byte[] key) {
    throw unsupported("byte[]");
  }

  @Override
  public boolean find(Period key) {
    throw unsupported("Period");
  }

  @Override
  public boolean find(LocalDate key) {
    throw unsupported("LocalDate");
  }

  @Override
  public boolean find(LocalTime key) {
    throw unsupported("LocalTime");
  }

  @Override
  public boolean find(Instant key) {
    throw unsupported("Instant");
  }

  private UnsupportedOperationException unsupported(String type) {
    return new UnsupportedOperationException(String.format(
        UNSUPPORTED_MESSAGE_TEMPLATE, this.getClass().getName(), type));
  }
}
