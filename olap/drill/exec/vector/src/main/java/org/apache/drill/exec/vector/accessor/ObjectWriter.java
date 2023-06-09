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

import org.apache.drill.exec.vector.accessor.writer.WriterEvents;

/**
 * Represents a column within a tuple. A column can be an array, a scalar or a
 * tuple. Each has an associated column metadata (schema) and a writer. The
 * writer is one of three kinds, depending on the kind of the column. If the
 * column is a map, then the column also has an associated tuple loader to
 * define and write to the tuple.
 * <p>
 * This interface defines a writer to set values for value vectors using a
 * simple, uniform interface modeled after a JSON object. Every column value is
 * an object of one of three types: scalar, array or tuple. Methods exist to
 * "cast" this object to the proper type. This model allows a very simple
 * representation: tuples (rows, maps) consist of objects. Arrays are lists of
 * objects.
 * <p>
 * Every column resides at an index, is defined by a schema, is backed by a
 * value vector, and and is written to by a writer. Each column also tracks the
 * schema version in which it was added to detect schema evolution. Each column
 * has an optional overflow vector that holds overflow record values when a
 * batch becomes full.
 * <p>
 * {@see ObjectReader}
 */

public interface ObjectWriter extends ColumnWriter {

  ScalarWriter scalar();
  TupleWriter tuple();
  ArrayWriter array();
  VariantWriter variant();
  DictWriter dict();

  /**
   * Generic version of the above, for dynamic handling of
   * writers.
   * @return the generic form of the column writer
   */

  ColumnWriter writer();

  /**
   * The internal state behind this writer. To be used only by the
   * implementation, not by the client.
   */

  WriterEvents events();
}
