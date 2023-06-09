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

import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Writer for a tuple. A tuple is composed of columns with a fixed order and
 * unique names: either can be used to reference columns. Columns are scalar
 * (simple values), tuples (i.e. maps), or arrays (of scalars, tuples or
 * arrays.) The row itself is just the top-level (anonymous) tuple. Generally,
 * implementers of this interface provide additional services on the
 * implementation of the top-level tuple (often called a "row writer.") Columns
 * are accessible via the associated column writer by name or index. Column
 * indexes are defined by the tuple schema.
 * <p>
 * Consumers of this interface can define the schema up front, or can define the
 * schema as the write progresses. To avoid redundant checks to see if a column
 * is already defined, consumers can simply ask for a column by name. The
 * {@code column()} (and related) methods will throw an (unchecked)
 * {@link UndefinedColumnException} exception if the column is undefined. The
 * consumer can catch the exception, define the column, and fetch the column
 * writer again. New columns may be added via this interface at any time; the
 * new column takes the next available index.
 * <p>
 * Also provides a convenience method to set the column value from a Java
 * object. The caller is responsible for providing the correct object type for
 * each column. (The object type must match the column accessor type.)
 * <p>
 * Convenience methods allow getting a column as a scalar, tuple or array. These
 * methods throw an exception if the column is not of the requested type.
 *
 * @see {@link SingleMapWriter}, the class which this class replaces
 */
public interface TupleWriter extends ColumnWriter {

  /**
   * Unchecked exception thrown when attempting to access a column writer by
   * name for an undefined columns. Clients that use a fixed schema can simply
   * omit catch blocks for the exception since it is unchecked and won't be
   * thrown if the schema can't evolve. Clients that can discover new columns
   * should catch the exception and define the column (using an implementation
   * that allows dynamic schema definition.)
   */
  @SuppressWarnings("serial")
  class UndefinedColumnException extends RuntimeException {
    public UndefinedColumnException(String colName) {
      super("Undefined column: " + colName);
    }
  }

  /**
   * Reports whether the given column is projected. Useful for
   * clients that can simply skip over unprojected columns.
   */
  boolean isProjected(String columnName);

  /**
   * Add a column to the tuple (row or map) that backs this writer. Support for
   * this operation depends on whether the client code has registered a listener
   * to implement the addition. Throws an exception if no listener is
   * implemented, or if the add request is otherwise invalid (duplicate name,
   * etc.)
   *
   * @param column
   *          the metadata for the column to add
   * @return the index of the newly added column which can be used to access the
   *         newly added writer
   */
  int addColumn(ColumnMetadata column);

  int addColumn(MaterializedField schema);

  TupleMetadata tupleSchema();

  int size();

  // Return the column as a generic object

  ObjectWriter column(int colIndex);

  ObjectWriter column(String colName);

  // Convenience methods

  ScalarWriter scalar(int colIndex);

  ScalarWriter scalar(String colName);

  TupleWriter tuple(int colIndex);

  TupleWriter tuple(String colName);

  ArrayWriter array(int colIndex);

  ArrayWriter array(String colName);

  VariantWriter variant(int colIndex);

  VariantWriter variant(String colName);

  DictWriter dict(int colIndex);

  DictWriter dict(String colName);

  ObjectType type(int colIndex);

  ObjectType type(String colName);

  /**
   * Write a value to the given column, automatically calling the proper
   * <code>set<i>Type</i></code> method for the data. While this method is
   * convenient for testing, it incurs quite a bit of type-checking overhead and
   * is not suitable for production code.
   *
   * @param colIndex
   *          the index of the column to set
   * @param value
   *          the value to set. The type of the object must be compatible with
   *          the type of the target column
   */

  void set(int colIndex, Object value);
}
