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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.VariantMetadata;

/**
 * Writer for a Drill "union vector." The union vector is presented
 * as a writer over a set of variants. In the old Visual Basic world,
 * "the Variant data type is a tagged union that can be used to
 * represent any other data type." The term is used here to avoid
 * confusion with the "union operator" which is something entirely
 * different.
 * <p>
 * At write time, the set of possible types is expanded upon the
 * first write to each type. A request to obtain a writer for type
 * will create the underlying storage vector if needed, then return
 * a write of the proper type. Note that unlike most other writers,
 * the caller is <i>required</i> to call the
 * {@link #scalar(MinorType)} method for each value so that this
 * writer knows which type of value is to be stored.
 * <p>
 * Alternatively, the client can cache a writer by calling
 * {@link #memberWriter(MinorType)} to retrieve a writer (without setting
 * the type for a row), then calling the set method on that writer for each
 * row, <b>and</b> calling {@link #setType(MinorType)} for each row.
 * <p>
 * This writer acts as somewhat like a map: it allows access to
 * type-specific writers.
 * <p>
 * Although the union and list vectors supports a union of any Drill
 * type, the only sane combinations are:
 * <ul>
 * <li>One of a (single or repeated) (map or list), or</li>
 * <li>One or more scalar type.</li>
 * </ul>
 *
 * @see {@link VariantReader}
 */
public interface VariantWriter extends ColumnWriter {

  interface VariantWriterListener {
    ObjectWriter addMember(ColumnMetadata schema);
    ObjectWriter addType(MinorType type);
  }

  /**
   * Metadata description of the variant that includes the set of
   * types, along with extended properties of the types such as
   * expected allocations sizes, expected array cardinality, etc.
   *
   * @return metadata for the variant
   */
  VariantMetadata variantSchema();

  /**
   * Returns the number of types in the variant. Some implementations
   * (such as lists) impart special meaning to a variant with a single type.
   *
   * @return number of types in the variant
   */
  int size();

  /**
   * Determine if the union vector has materialized storage for the
   * given type. (The storage will be created as needed during writing.)
   *
   * @param type data type
   * @return {@code true} if a value of the given type has been written
   * and storage allocated (or storage was allocated implicitly),
   * {@code false} otherwise
   */
  boolean hasType(MinorType type);

  ObjectWriter addMember(MinorType type);
  ObjectWriter addMember(ColumnMetadata schema);

  /**
   * Create or retrieve a writer for the given type. Use this form when
   * caching writers. This form <i>does not</i> set the type of the current
   * row; call {@link #setType(MinorType)} per row when the writers are
   * cached. This method can be called at any time as it does not depend
   * on an active batch.
   *
   * @param type the type of the writer to cache
   * @return the writer for that type without setting the type of the
   * current row.
   */
  ObjectWriter memberWriter(MinorType type);

  /**
   * Explicitly set the type of the present value. Use this when
   * the writers are cached. The writer must already exist.
   *
   * @param type type to set for the current row
   */
  void setType(MinorType type);

  /**
   * Set the type of the present value and get the writer for
   * that type. Available only when a batch is active. Use this form to
   * declare the type of the current row, and retrieve a writer for that
   * value.
   *
   * @param type type to set for the current row
   * @return writer for the type just set
   */
  ObjectWriter member(MinorType type);
  ScalarWriter scalar(MinorType type);
  TupleWriter tuple();
  ArrayWriter array();
}
