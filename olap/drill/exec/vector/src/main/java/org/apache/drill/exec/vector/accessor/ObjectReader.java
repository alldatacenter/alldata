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

/**
 * Defines a reader to get values for value vectors using
 * a simple, uniform interface modeled after a JSON object.
 * Every column value is an object of one of three types:
 * scalar, array or tuple. Methods exist to "cast" this object
 * to the proper type. This model allows a very simple representation:
 * tuples (rows, maps) consist of objects. Arrays are lists of
 * objects.
 * <p>
 * {@see ObjectWriter>
 */

public interface ObjectReader extends ColumnReader {
  ScalarReader scalar();
  TupleReader tuple();
  ArrayReader array();
  VariantReader variant();
  DictReader dict();

  /**
   * Gets the reader as a generic type, for dynamic
   * programming.
   * @return the untyped reader
   */
  ColumnReader reader();
}
