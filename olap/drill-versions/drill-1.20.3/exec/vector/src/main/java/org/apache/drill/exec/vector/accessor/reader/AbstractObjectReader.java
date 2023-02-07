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
package org.apache.drill.exec.vector.accessor.reader;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.DictReader;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.VariantReader;

public abstract class AbstractObjectReader implements ObjectReader {

  @Override
  public ColumnMetadata schema() { return reader().schema(); }

  @Override
  public ScalarReader scalar() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TupleReader tuple() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ArrayReader array() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DictReader dict() {
    throw new UnsupportedOperationException();
  }

  public abstract ReaderEvents events();

  @Override
  public abstract ColumnReader reader();

  @Override
  public VariantReader variant() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNull() { return reader().isNull(); }

  @Override
  public ObjectType type() { return reader().type(); }
}
